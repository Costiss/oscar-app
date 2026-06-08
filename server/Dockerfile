# ---- estágio de build ----
FROM golang:1.25-alpine AS build

WORKDIR /src

# Faz cache das dependências primeiro.
COPY go.mod go.sum ./
RUN go mod download

# Compila um binário estático (modernc.org/sqlite é puro Go, então CGO não é necessário).
COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -ldflags="-s -w" -o /out/oscar-server .

# ---- estágio de runtime ----
FROM alpine:3.20

WORKDIR /app

# Dados de catálogo (filme.json / diretor.json) lidos na inicialização.
COPY --from=build /out/oscar-server /app/oscar-server
COPY data ./data

# O arquivo SQLite fica aqui para que possa ser persistido via volume.
VOLUME ["/app/db"]

EXPOSE 8080

ENTRYPOINT ["/app/oscar-server"]
CMD ["-addr", ":8080", "-db", "/app/db/oscarapp.db", "-data", "data"]
