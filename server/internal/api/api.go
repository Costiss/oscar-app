// O pacote api expõe a camada HTTP RESTful do Sistema Central.
//
// Todos os endpoints se comunicam exclusivamente em JSON, usam verbos e códigos
// de status HTTP semânticos e nunca retornam dados sensíveis (como hashes de
// senha).
package api

import (
	"encoding/json"
	"errors"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strings"

	"oscarapp/server/internal/store"
)

// API guarda as dependências compartilhadas por todos os handlers.
type API struct {
	store   *store.Store
	dataDir string // diretório que contém filme.json e diretor.json
}

// New constrói uma API vinculada a um store e a um diretório de dados.
func New(s *store.Store, dataDir string) *API {
	return &API{store: s, dataDir: dataDir}
}

// Routes retorna o handler HTTP configurado com logging e padrões de JSON.
func (a *API) Routes() http.Handler {
	mux := http.NewServeMux()

	mux.HandleFunc("GET /api/health", a.handleHealth)
	mux.HandleFunc("POST /api/login", a.handleLogin)
	mux.HandleFunc("GET /api/filmes", a.handleFilmes)
	mux.HandleFunc("GET /api/diretores", a.handleDiretores)
	mux.HandleFunc("POST /api/votos", a.handleVotar)

	return logRequests(mux)
}

// ---------------------------------------------------------------------------
// Auxiliares de JSON
// ---------------------------------------------------------------------------

func writeJSON(w http.ResponseWriter, status int, payload any) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.WriteHeader(status)
	if payload != nil {
		_ = json.NewEncoder(w).Encode(payload)
	}
}

// erroJSON é o envelope de erro padrão retornado por todos os endpoints.
type erroJSON struct {
	Sucesso bool   `json:"sucesso"`
	Erro    string `json:"erro"`
}

func writeErro(w http.ResponseWriter, status int, msg string) {
	writeJSON(w, status, erroJSON{Sucesso: false, Erro: msg})
}

func logRequests(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		log.Printf("%s %s", r.Method, r.URL.Path)
		next.ServeHTTP(w, r)
	})
}

// ---------------------------------------------------------------------------
// Handlers
// ---------------------------------------------------------------------------

func (a *API) handleHealth(w http.ResponseWriter, _ *http.Request) {
	writeJSON(w, http.StatusOK, map[string]any{"sucesso": true, "status": "ok"})
}

// loginRequest é o corpo esperado de POST /api/login.
type loginRequest struct {
	Login string `json:"login"`
	Senha string `json:"senha"`
}

// votoLogin reproduz um voto que o usuário já registrou, retornado no login para
// que o cliente possa exibi-lo e travar a edição. Os nomes são resolvidos a
// partir dos catálogos para que o app não precise de uma requisição extra só
// para exibi-los.
type votoLogin struct {
	FilmeID     string `json:"filmeId"`
	DiretorID   string `json:"diretorId"`
	FilmeNome   string `json:"filmeNome"`
	DiretorNome string `json:"diretorNome"`
}

// loginResponse é retornado em uma autenticação bem-sucedida. Carrega o token de
// sessão (aleatório de 0 a 100) e apenas os dados públicos do usuário. Quando o
// usuário já votou, jaVotou é true e voto carrega a escolha registrada.
type loginResponse struct {
	Sucesso bool          `json:"sucesso"`
	Token   int           `json:"token"`
	Usuario store.Usuario `json:"usuario"`
	JaVotou bool          `json:"jaVotou"`
	Voto    *votoLogin    `json:"voto,omitempty"`
}

func (a *API) handleLogin(w http.ResponseWriter, r *http.Request) {
	var req loginRequest
	if err := decodeBody(r, &req); err != nil {
		writeErro(w, http.StatusBadRequest, "corpo da requisicao invalido")
		return
	}

	req.Login = strings.TrimSpace(req.Login)
	if req.Login == "" || req.Senha == "" {
		writeErro(w, http.StatusBadRequest, "login e senha sao obrigatorios")
		return
	}

	usuario, token, err := a.store.Autenticar(req.Login, req.Senha)
	if errors.Is(err, store.ErrCredenciaisInvalidas) {
		writeErro(w, http.StatusUnauthorized, "login ou senha incorretos")
		return
	}
	if err != nil {
		log.Printf("erro no login: %v", err)
		writeErro(w, http.StatusInternalServerError, "erro interno ao autenticar")
		return
	}

	resp := loginResponse{Sucesso: true, Token: token, Usuario: usuario}

	// Se o usuário já votou, reproduz o voto para que o app possa carregá-lo na
	// sessão, exibi-lo na tela de boas-vindas e manter a edição travada.
	voto, err := a.store.BuscarVoto(usuario.ID)
	if err != nil {
		log.Printf("erro buscando voto do usuario %d: %v", usuario.ID, err)
	} else if voto != nil {
		resp.JaVotou = true
		resp.Voto = &votoLogin{
			FilmeID:     voto.FilmeID,
			DiretorID:   voto.DiretorID,
			FilmeNome:   a.nomePorId("filme.json", voto.FilmeID),
			DiretorNome: a.nomePorId("diretor.json", voto.DiretorID),
		}
	}

	writeJSON(w, http.StatusOK, resp)
}

func (a *API) handleFilmes(w http.ResponseWriter, r *http.Request) {
	a.serveDataFile(w, "filme.json")
}

func (a *API) handleDiretores(w http.ResponseWriter, r *http.Request) {
	a.serveDataFile(w, "diretor.json")
}

// serveDataFile transmite um arquivo JSON de catálogo do disco a cada requisição,
// de modo que o app reflita qualquer alteração em filme.json / diretor.json sem
// recompilar.
func (a *API) serveDataFile(w http.ResponseWriter, name string) {
	data, err := os.ReadFile(filepath.Join(a.dataDir, name))
	if err != nil {
		log.Printf("erro lendo %s: %v", name, err)
		writeErro(w, http.StatusInternalServerError, "catalogo indisponivel")
		return
	}
	// Valida que é um JSON bem formado antes de servir.
	if !json.Valid(data) {
		writeErro(w, http.StatusInternalServerError, "catalogo com json invalido")
		return
	}
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.WriteHeader(http.StatusOK)
	_, _ = w.Write(data)
}

// votarRequest é o corpo esperado de POST /api/votos. O usuário é identificado
// somente pelo token de sessão.
type votarRequest struct {
	FilmeID   string `json:"filmeId"`
	DiretorID string `json:"diretorId"`
	Token     *int   `json:"token"`
}

func (a *API) handleVotar(w http.ResponseWriter, r *http.Request) {
	var req votarRequest
	if err := decodeBody(r, &req); err != nil {
		writeErro(w, http.StatusBadRequest, "corpo da requisicao invalido")
		return
	}

	req.FilmeID = strings.TrimSpace(req.FilmeID)
	req.DiretorID = strings.TrimSpace(req.DiretorID)
	if req.Token == nil {
		writeErro(w, http.StatusBadRequest, "token e obrigatorio")
		return
	}
	if req.FilmeID == "" || req.DiretorID == "" {
		writeErro(w, http.StatusBadRequest, "filmeId e diretorId sao obrigatorios")
		return
	}

	// Valida que os ids referenciados realmente existem nos catálogos.
	if !a.idExiste("filme.json", req.FilmeID) {
		writeErro(w, http.StatusBadRequest, "filmeId inexistente")
		return
	}
	if !a.idExiste("diretor.json", req.DiretorID) {
		writeErro(w, http.StatusBadRequest, "diretorId inexistente")
		return
	}

	voto, err := a.store.RegistrarVoto(*req.Token, req.FilmeID, req.DiretorID)
	switch {
	case errors.Is(err, store.ErrTokenInvalido):
		writeErro(w, http.StatusUnauthorized, "token invalido ou expirado")
		return
	case errors.Is(err, store.ErrJaVotou):
		writeErro(w, http.StatusConflict, "este usuario ja registrou seu voto")
		return
	case err != nil:
		log.Printf("erro ao registrar voto: %v", err)
		writeErro(w, http.StatusInternalServerError, "erro interno ao registrar voto")
		return
	}

	writeJSON(w, http.StatusOK, map[string]any{
		"sucesso":  true,
		"mensagem": "voto registrado com sucesso",
		"voto": map[string]string{
			"filmeId":   voto.FilmeID,
			"diretorId": voto.DiretorID,
		},
	})
}

// idExiste informa se o id fornecido está presente no arquivo de catálogo.
func (a *API) idExiste(file, id string) bool {
	data, err := os.ReadFile(filepath.Join(a.dataDir, file))
	if err != nil {
		return false
	}
	var itens []map[string]any
	if json.Unmarshal(data, &itens) != nil {
		return false
	}
	for _, it := range itens {
		if v, ok := it["id"].(string); ok && v == id {
			return true
		}
	}
	return false
}

// nomePorId retorna o campo "nome" do item de catálogo com o id fornecido, ou
// uma string vazia quando o id (ou o arquivo) não é encontrado.
func (a *API) nomePorId(file, id string) string {
	data, err := os.ReadFile(filepath.Join(a.dataDir, file))
	if err != nil {
		return ""
	}
	var itens []map[string]any
	if json.Unmarshal(data, &itens) != nil {
		return ""
	}
	for _, it := range itens {
		if v, ok := it["id"].(string); ok && v == id {
			if nome, ok := it["nome"].(string); ok {
				return nome
			}
		}
	}
	return ""
}

// decodeBody decodifica o corpo JSON de uma requisição, rejeitando campos
// desconhecidos e corpos vazios para que entradas malformadas sejam detectadas
// antes de chegar à lógica de negócio.
func decodeBody(r *http.Request, dst any) error {
	if r.Body == nil {
		return errors.New("corpo vazio")
	}
	dec := json.NewDecoder(r.Body)
	dec.DisallowUnknownFields()
	return dec.Decode(dst)
}
