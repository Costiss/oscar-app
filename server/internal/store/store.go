// O pacote store concentra todo o acesso ao banco de dados do backend do Oscar App.
//
// O Sistema Central persiste aqui todo o estado: os usuários, o token de sessão
// gerado no login e o voto único que cada usuário pode registrar. O SQLite é
// usado através de um driver puro em Go, de modo que o servidor roda sem nenhum
// processo de banco externo e sem o toolchain CGO.
package store

import (
	"database/sql"
	"errors"
	"fmt"
	"math/rand"
	"time"

	"golang.org/x/crypto/bcrypt"
	_ "modernc.org/sqlite"
)

// Erros sentinela retornados pelo store para que a camada de API possa mapeá-los
// para os códigos de status HTTP corretos sem inspecionar mensagens específicas
// do driver.
var (
	ErrCredenciaisInvalidas = errors.New("login ou senha invalidos")
	ErrTokenInvalido        = errors.New("token invalido ou expirado")
	ErrJaVotou              = errors.New("usuario ja registrou seu voto")
)

// Usuario é a visão pública de um usuário. O hash da senha e qualquer outra
// coluna sensível são, de propósito, mantidos fora desta struct para que nunca
// possam vazar em uma resposta da API.
type Usuario struct {
	ID    int64  `json:"id"`
	Login string `json:"login"`
	Nome  string `json:"nome"`
}

// Voto representa um voto registrado (um por usuário, garantido pelo schema).
type Voto struct {
	UsuarioID int64  `json:"usuarioId"`
	FilmeID   string `json:"filmeId"`
	DiretorID string `json:"diretorId"`
	CriadoEm  string `json:"criadoEm"`
}

// Store encapsula o handle do banco de dados e a fonte de aleatoriedade usada
// para gerar os tokens de sessão.
type Store struct {
	db  *sql.DB
	rng *rand.Rand
}

// Open abre (criando-o se necessário) o banco de dados SQLite em path, executa a
// migração do schema e popula os dados de teste necessários.
func Open(path string) (*Store, error) {
	db, err := sql.Open("sqlite", path)
	if err != nil {
		return nil, fmt.Errorf("abrindo banco: %w", err)
	}
	// O SQLite lida melhor com concorrência usando uma única conexão de escrita.
	db.SetMaxOpenConns(1)
	if _, err := db.Exec("PRAGMA foreign_keys = ON;"); err != nil {
		return nil, fmt.Errorf("habilitando foreign keys: %w", err)
	}

	s := &Store{db: db, rng: rand.New(rand.NewSource(time.Now().UnixNano()))}
	if err := s.migrate(); err != nil {
		return nil, err
	}
	if err := s.seed(); err != nil {
		return nil, err
	}
	return s, nil
}

// Close libera o handle do banco de dados.
func (s *Store) Close() error { return s.db.Close() }

// migrate cria as tabelas, constraints e índices exigidos pela especificação:
// unicidade do voto por usuário, integridade referencial, colunas NOT NULL e um
// índice na coluna login para buscas rápidas na autenticação.
func (s *Store) migrate() error {
	const schema = `
CREATE TABLE IF NOT EXISTS usuarios (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    login       TEXT    NOT NULL UNIQUE,
    senha_hash  TEXT    NOT NULL,
    nome        TEXT    NOT NULL,
    token       INTEGER          -- token de sessao atual, NULL quando deslogado
);

CREATE INDEX IF NOT EXISTS idx_usuarios_login ON usuarios(login);
CREATE INDEX IF NOT EXISTS idx_usuarios_token ON usuarios(token);

CREATE TABLE IF NOT EXISTS votos (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    usuario_id  INTEGER NOT NULL UNIQUE,      -- constraint: 1 voto por usuario
    filme_id    TEXT    NOT NULL,
    diretor_id  TEXT    NOT NULL,
    criado_em   TEXT    NOT NULL,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);
`
	if _, err := s.db.Exec(schema); err != nil {
		return fmt.Errorf("migracao: %w", err)
	}
	return nil
}

// seed insere os dados de teste pré-cadastrados exigidos para a apresentação:
//   - pelo menos 5 usuários;
//   - 2 usuários que nunca são usados e não têm votos (dave, eve);
//   - pelo menos 1 usuário com um voto confirmado (alice).
//
// É idempotente: só roda quando a tabela de usuários está vazia.
func (s *Store) seed() error {
	var count int
	if err := s.db.QueryRow("SELECT COUNT(*) FROM usuarios").Scan(&count); err != nil {
		return fmt.Errorf("contando usuarios: %w", err)
	}
	if count > 0 {
		return nil
	}

	type seedUser struct {
		login, senha, nome string
	}
	users := []seedUser{
		{"alice", "senha123", "Alice Andrade"},
		{"bob", "senha123", "Bob Barbosa"},
		{"carol", "senha123", "Carol Carvalho"},
		{"dave", "senha123", "Dave Dias"},   // nunca usado
		{"eve", "senha123", "Eve Esteves"},  // nunca usado
	}

	tx, err := s.db.Begin()
	if err != nil {
		return err
	}
	defer tx.Rollback()

	var aliceID int64
	for _, u := range users {
		hash, err := bcrypt.GenerateFromPassword([]byte(u.senha), bcrypt.DefaultCost)
		if err != nil {
			return fmt.Errorf("hash senha seed: %w", err)
		}
		res, err := tx.Exec(
			"INSERT INTO usuarios (login, senha_hash, nome, token) VALUES (?, ?, ?, NULL)",
			u.login, string(hash), u.nome,
		)
		if err != nil {
			return fmt.Errorf("inserindo usuario seed %q: %w", u.login, err)
		}
		if u.login == "alice" {
			aliceID, _ = res.LastInsertId()
		}
	}

	// A Alice já tem um voto confirmado registrado no banco de dados.
	if _, err := tx.Exec(
		"INSERT INTO votos (usuario_id, filme_id, diretor_id, criado_em) VALUES (?, ?, ?, ?)",
		aliceID, "2", "20", time.Now().UTC().Format(time.RFC3339),
	); err != nil {
		return fmt.Errorf("inserindo voto seed: %w", err)
	}

	return tx.Commit()
}

// BuscarVoto retorna o voto já registrado pelo usuário, ou (nil, nil) quando o
// usuário ainda não votou. Usado no login para que o cliente possa carregar um
// voto existente na sessão e travar a edição.
func (s *Store) BuscarVoto(usuarioID int64) (*Voto, error) {
	var v Voto
	err := s.db.QueryRow(
		"SELECT usuario_id, filme_id, diretor_id, criado_em FROM votos WHERE usuario_id = ?",
		usuarioID,
	).Scan(&v.UsuarioID, &v.FilmeID, &v.DiretorID, &v.CriadoEm)
	if errors.Is(err, sql.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return &v, nil
}

// Autenticar valida as credenciais e, em caso de sucesso, gera um novo token de
// sessão (inteiro aleatório de 0 a 100, único entre as sessões ativas), persiste-o
// vinculado ao usuário e retorna o usuário junto com o token.
func (s *Store) Autenticar(login, senha string) (Usuario, int, error) {
	var (
		u    Usuario
		hash string
	)
	err := s.db.QueryRow(
		"SELECT id, login, nome, senha_hash FROM usuarios WHERE login = ?", login,
	).Scan(&u.ID, &u.Login, &u.Nome, &hash)
	if errors.Is(err, sql.ErrNoRows) {
		return Usuario{}, 0, ErrCredenciaisInvalidas
	}
	if err != nil {
		return Usuario{}, 0, err
	}
	if bcrypt.CompareHashAndPassword([]byte(hash), []byte(senha)) != nil {
		return Usuario{}, 0, ErrCredenciaisInvalidas
	}

	token, err := s.gerarTokenUnico(u.ID)
	if err != nil {
		return Usuario{}, 0, err
	}
	if _, err := s.db.Exec("UPDATE usuarios SET token = ? WHERE id = ?", token, u.ID); err != nil {
		return Usuario{}, 0, err
	}
	return u, token, nil
}

// gerarTokenUnico retorna um inteiro aleatório em [0,100] que nenhum outro
// usuário possui no momento, mantendo o token único por sessão ativa. Tenta um
// número limitado de sorteios aleatórios e, em seguida, recorre a uma varredura
// linear.
func (s *Store) gerarTokenUnico(usuarioID int64) (int, error) {
	emUso := func(tok int) (bool, error) {
		var n int
		err := s.db.QueryRow(
			"SELECT COUNT(*) FROM usuarios WHERE token = ? AND id <> ?", tok, usuarioID,
		).Scan(&n)
		return n > 0, err
	}
	for i := 0; i < 200; i++ {
		tok := s.rng.Intn(101)
		used, err := emUso(tok)
		if err != nil {
			return 0, err
		}
		if !used {
			return tok, nil
		}
	}
	for tok := 0; tok <= 100; tok++ {
		used, err := emUso(tok)
		if err != nil {
			return 0, err
		}
		if !used {
			return tok, nil
		}
	}
	return 0, errors.New("nao ha tokens de sessao disponiveis")
}

// RegistrarVoto valida o token, garante que o usuário ainda não votou e persiste
// o voto de forma atômica. O usuário é resolvido apenas a partir do token, já que
// o endpoint de voto recebe somente o id do filme, o id do diretor e o token.
func (s *Store) RegistrarVoto(token int, filmeID, diretorID string) (Voto, error) {
	tx, err := s.db.Begin()
	if err != nil {
		return Voto{}, err
	}
	defer tx.Rollback()

	var usuarioID int64
	err = tx.QueryRow("SELECT id FROM usuarios WHERE token = ?", token).Scan(&usuarioID)
	if errors.Is(err, sql.ErrNoRows) {
		return Voto{}, ErrTokenInvalido
	}
	if err != nil {
		return Voto{}, err
	}

	var existe int
	if err := tx.QueryRow("SELECT COUNT(*) FROM votos WHERE usuario_id = ?", usuarioID).Scan(&existe); err != nil {
		return Voto{}, err
	}
	if existe > 0 {
		return Voto{}, ErrJaVotou
	}

	voto := Voto{
		UsuarioID: usuarioID,
		FilmeID:   filmeID,
		DiretorID: diretorID,
		CriadoEm:  time.Now().UTC().Format(time.RFC3339),
	}
	if _, err := tx.Exec(
		"INSERT INTO votos (usuario_id, filme_id, diretor_id, criado_em) VALUES (?, ?, ?, ?)",
		voto.UsuarioID, voto.FilmeID, voto.DiretorID, voto.CriadoEm,
	); err != nil {
		return Voto{}, err
	}

	// O token de sessão é de uso único para votar: assim que o voto é registrado
	// ele é limpo para que o mesmo token não possa ser reutilizado.
	if _, err := tx.Exec("UPDATE usuarios SET token = NULL WHERE id = ?", usuarioID); err != nil {
		return Voto{}, err
	}

	if err := tx.Commit(); err != nil {
		return Voto{}, err
	}
	return voto, nil
}
