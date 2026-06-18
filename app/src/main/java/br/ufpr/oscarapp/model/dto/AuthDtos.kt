package br.ufpr.oscarapp.model.dto

import br.ufpr.oscarapp.model.Usuario

/** Body of POST /api/login. */
data class LoginRequest(
    val login: String,
    val senha: String
)

/** Success body of POST /api/login. */
data class LoginResponse(
    val sucesso: Boolean,
    val token: Int,
    val usuario: Usuario,
    val jaVotou: Boolean = false,
    val voto: VotoLogin? = null
)

/**
 * Vote already registered by the user, echoed at login when [jaVotou] is true.
 * Carries the resolved film/director names so the app can show the vote without
 * an extra request.
 */
data class VotoLogin(
    val filmeId: String,
    val diretorId: String,
    val filmeNome: String,
    val diretorNome: String
)
