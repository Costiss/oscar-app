package br.ufpr.oscarapp.model

import br.ufpr.oscarapp.model.dto.VotoLogin

/**
 * In-memory session state for the logged-in user.
 *
 * Holds the token returned at login, the chosen film/director (kept locally
 * until the final confirmation) and whether the vote has already been
 * confirmed on the server. Cleared on logout.
 */
object Sessao {

    var token: Int? = null
    var usuario: Usuario? = null

    var filmeSelecionado: Filme? = null
    var diretorSelecionado: Diretor? = null

    /** Once true, the vote is locked and editing must be blocked. */
    var votoConfirmado: Boolean = false

    fun iniciar(usuario: Usuario, token: Int, voto: VotoLogin? = null) {
        this.usuario = usuario
        this.token = token
        if (voto != null) {
            // The user has already voted: load the registered choice and lock
            // editing. Genre/photo are unknown here and only the name is shown.
            this.filmeSelecionado = Filme(voto.filmeId, voto.filmeNome, "", "")
            this.diretorSelecionado = Diretor(voto.diretorId, voto.diretorNome)
            this.votoConfirmado = true
        } else {
            this.filmeSelecionado = null
            this.diretorSelecionado = null
            this.votoConfirmado = false
        }
    }

    fun limpar() {
        token = null
        usuario = null
        filmeSelecionado = null
        diretorSelecionado = null
        votoConfirmado = false
    }
}
