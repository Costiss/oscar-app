package br.ufpr.oscarapp.model

import java.io.Serializable

/** Authenticated user as returned by POST /api/login. */
data class Usuario(
    val id: Long,
    val login: String,
    val nome: String
) : Serializable
