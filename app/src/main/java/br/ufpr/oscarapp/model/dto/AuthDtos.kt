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
    val usuario: Usuario
)
