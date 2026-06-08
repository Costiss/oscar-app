package br.ufpr.oscarapp.model

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

    fun iniciar(usuario: Usuario, token: Int) {
        this.usuario = usuario
        this.token = token
        this.filmeSelecionado = null
        this.diretorSelecionado = null
        this.votoConfirmado = false
    }

    fun limpar() {
        token = null
        usuario = null
        filmeSelecionado = null
        diretorSelecionado = null
        votoConfirmado = false
    }
}
