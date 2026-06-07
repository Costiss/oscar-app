// O comando server é o "Sistema Central" do Oscar App: um backend RESTful que
// trata da autenticação, da geração do token de sessão, dos catálogos de
// filmes/diretores e do voto único que cada usuário pode registrar.
package main

import (
	"flag"
	"log"
	"net/http"
	"time"

	"oscarapp/server/internal/api"
	"oscarapp/server/internal/store"
)

func main() {
	addr := flag.String("addr", ":8080", "endereco HTTP (ex.: :8080)")
	dbPath := flag.String("db", "oscarapp.db", "caminho do arquivo SQLite")
	dataDir := flag.String("data", "data", "diretorio com filme.json e diretor.json")
	flag.Parse()

	st, err := store.Open(*dbPath)
	if err != nil {
		log.Fatalf("falha ao inicializar o banco: %v", err)
	}
	defer st.Close()

	srv := &http.Server{
		Addr:         *addr,
		Handler:      api.New(st, *dataDir).Routes(),
		ReadTimeout:  10 * time.Second,
		WriteTimeout: 10 * time.Second,
	}

	log.Printf("Sistema Central ouvindo em %s (db=%s, data=%s)", *addr, *dbPath, *dataDir)
	if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
		log.Fatalf("servidor encerrado: %v", err)
	}
}
