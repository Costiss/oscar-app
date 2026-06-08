package br.ufpr.oscarapp.model

import java.io.Serializable

/** A nominated film, served by GET /api/filmes. */
data class Filme(
    val id: String,
    val nome: String,
    val genero: String,
    val foto: String
) : Serializable
