package br.ufpr.oscarapp.model.dto

/** Standard error envelope: { "sucesso": false, "erro": "mensagem legivel" }. */
data class ApiError(
    val sucesso: Boolean = false,
    val erro: String? = null
)
