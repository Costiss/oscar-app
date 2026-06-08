package br.ufpr.oscarapp.model

import java.io.Serializable

/** A nominated director, served by GET /api/diretores. */
data class Diretor(
    val id: String,
    val nome: String
) : Serializable
