# Oscar App — Sistema Central · Referência da API

Backend RESTful para o cliente Android do Oscar App. Todas as requisições e
respostas usam **JSON** (`Content-Type: application/json; charset=utf-8`). Todos
os endpoints têm o prefixo `/api`.

- URL base (local): `http://localhost:8080`
- A partir do emulador Android, alcance a máquina host em `http://10.0.2.2:8080`.

## Convenções

- Verbos: `GET` para leituras, `POST` para ações que alteram estado.
- Os códigos de status são semânticos: `200` sucesso, `400` requisição inválida,
  `401` não autorizado, `409` conflito, `500` erro interno, `502` falha ao obter
  um catálogo remoto.
- Todo erro tem o mesmo envelope:

  ```json
  { "sucesso": false, "erro": "mensagem legivel" }
  ```

- Dados sensíveis (hashes de senha, tokens de outros usuários) **nunca** são
  retornados.

---

## 1. `POST /api/login` — Autenticação

Valida as credenciais. Em caso de sucesso o servidor gera um **token de sessão**
(inteiro aleatório de `0` a `100`, único entre as sessões ativas), o armazena
vinculado ao usuário e o retorna. O token é a chave usada depois para confirmar
o voto.

**Corpo da requisição**

```json
{ "login": "alice", "senha": "senha123" }
```

| Campo   | Tipo   | Obrigatório | Observações       |
|---------|--------|-------------|-------------------|
| `login` | string | sim         | não vazio         |
| `senha` | string | sim         | não vazio         |

**Sucesso — `200 OK`**

```json
{
  "sucesso": true,
  "token": 42,
  "usuario": { "id": 2, "login": "bob", "nome": "Bob Barbosa" },
  "jaVotou": false
}
```

Quando o usuário **já votou**, a resposta também reproduz o voto registrado (com
os nomes de filme/diretor resolvidos a partir dos catálogos locais), para que o
cliente possa exibi-lo e manter a edição travada:

```json
{
  "sucesso": true,
  "token": 42,
  "usuario": { "id": 1, "login": "alice", "nome": "Alice Andrade" },
  "jaVotou": true,
  "voto": {
    "filmeId": "2",
    "diretorId": "20",
    "filmeNome": "Oppenheimer",
    "diretorNome": "Christopher Nolan"
  }
}
```

| Campo     | Tipo    | Observações                                       |
|-----------|---------|---------------------------------------------------|
| `jaVotou` | boolean | `true` quando o usuário já registrou um voto      |
| `voto`    | objeto  | presente apenas quando `jaVotou` é `true`         |

**Erros**

| Status | Quando                                       | Corpo                                                            |
|--------|----------------------------------------------|------------------------------------------------------------------|
| `400`  | JSON malformado / corpo ausente              | `{ "sucesso": false, "erro": "corpo da requisicao invalido" }`   |
| `400`  | `login` ou `senha` vazios                     | `{ "sucesso": false, "erro": "login e senha sao obrigatorios" }` |
| `401`  | usuário desconhecido ou senha incorreta       | `{ "sucesso": false, "erro": "login ou senha incorretos" }`      |
| `500`  | falha interna ao autenticar                   | `{ "sucesso": false, "erro": "erro interno ao autenticar" }`     |

---

## 2. `GET /api/filmes` — Catálogo de filmes

Retorna a lista de filmes indicados. O catálogo é **buscado do upstream remoto**
(`http://200.236.3.97/filme.json`) a cada requisição e repassado ao cliente, de
modo que qualquer alteração na origem é refletida imediatamente (sem recompilar).
A lista no Android deve renderizar corretamente para **qualquer** número de itens.

**Sucesso — `200 OK`**

```json
[
  { "id": "1", "nome": "Piratas do Caribe", "genero": "Aventura", "foto": "http://200.236.3.97/imagens/piratas.jpeg" },
  { "id": "10", "nome": "La La Land", "genero": "Musical", "foto": "http://200.236.3.97/imagens/land.jpeg" }
]
```

| Campo    | Tipo   | Observações                          |
|----------|--------|--------------------------------------|
| `id`     | string | id único do filme                    |
| `nome`   | string | título                               |
| `genero` | string | gênero                               |
| `foto`   | string | URL do pôster (carregada de forma assíncrona) |

**Erros**

| Status | Quando                                                 | `erro`                       |
|--------|--------------------------------------------------------|------------------------------|
| `502`  | upstream inacessível, status não-200 ou falha de leitura | `catalogo indisponivel`      |
| `502`  | o upstream respondeu com JSON inválido                  | `catalogo com json invalido` |

---

## 3. `GET /api/diretores` — Catálogo de diretores

Retorna a lista de diretores indicados, **buscada do upstream remoto**
(`http://200.236.3.97/diretor.json`) a cada requisição. Usada para montar o
`RadioGroup` dinâmico no cliente (um `RadioButton` por item).

**Sucesso — `200 OK`**

```json
[
  { "id": "1", "nome": "James Cameron" },
  { "id": "15", "nome": "Steven Spielberg" }
]
```

| Campo  | Tipo   | Observações          |
|--------|--------|----------------------|
| `id`   | string | id único do diretor  |
| `nome` | string | nome do diretor      |

**Erros** — os mesmos de `GET /api/filmes` (`502` com `catalogo indisponivel`
ou `catalogo com json invalido`).

---

## 4. `POST /api/votos` — Registrar voto

Confirma o voto do usuário. O servidor **valida o token**, resolve a qual usuário
ele pertence, garante que esse usuário **ainda não votou**, verifica que os ids
existem no **catálogo local** (`data/filme.json` / `data/diretor.json`) e persiste
o voto. Cada usuário vota **apenas uma vez**. O token é de uso único: após um voto
bem-sucedido ele é limpo e não pode ser reutilizado.

**Corpo da requisição**

```json
{ "filmeId": "3", "diretorId": "22", "token": 42 }
```

| Campo       | Tipo    | Obrigatório | Observações                          |
|-------------|---------|-------------|--------------------------------------|
| `filmeId`   | string  | sim         | deve existir no catálogo de filmes   |
| `diretorId` | string  | sim         | deve existir no catálogo de diretores|
| `token`     | inteiro | sim         | token de sessão retornado por `/login` |

**Sucesso — `200 OK`**

```json
{
  "sucesso": true,
  "mensagem": "voto registrado com sucesso",
  "voto": { "filmeId": "3", "diretorId": "22" }
}
```

**Erros**

| Status | Quando                                    | `erro`                                |
|--------|-------------------------------------------|---------------------------------------|
| `400`  | JSON malformado / corpo ausente           | `corpo da requisicao invalido`        |
| `400`  | `token` ausente                           | `token e obrigatorio`                 |
| `400`  | `filmeId` ou `diretorId` vazios           | `filmeId e diretorId sao obrigatorios`|
| `400`  | id não presente no catálogo local         | `filmeId inexistente` / `diretorId inexistente` |
| `401`  | token desconhecido / já consumido         | `token invalido ou expirado`          |
| `409`  | usuário já registrou um voto              | `este usuario ja registrou seu voto`  |

---

## 5. `GET /api/health` — Verificação de saúde

```json
{ "sucesso": true, "status": "ok" }
```

---

## Fluxo típico do cliente

1. `POST /api/login` → guarda o `token` retornado em memória durante a sessão.
2. `GET /api/filmes` → exibe a lista + pôsteres; o usuário escolhe um filme (mantido localmente).
3. `GET /api/diretores` → monta o `RadioGroup`; o usuário escolhe um diretor (mantido localmente).
4. Na tela "Confirmar Voto", envia `POST /api/votos` com `filmeId`, `diretorId`
   e `token`. Em `200`, trava a edição; caso contrário, exibe a mensagem de
   `erro` em um `AlertDialog`.

## Usuários de teste pré-cadastrados

| login   | senha      | estado                              |
|---------|------------|-------------------------------------|
| `alice` | `senha123` | já possui um voto confirmado        |
| `bob`   | `senha123` | disponível para votar               |
| `carol` | `senha123` | disponível para votar               |
| `dave`  | `senha123` | nunca usado (sem token, sem voto)   |
| `eve`   | `senha123` | nunca usado (sem token, sem voto)   |
