package br.ufpr.oscarapp.model.dto

/** Body of POST /api/votos. */
data class VotoRequest(
    val filmeId: String,
    val diretorId: String,
    val token: Int
)

/** Echoed vote inside a successful voto response. */
data class VotoRegistrado(
    val filmeId: String,
    val diretorId: String
)

/** Success body of POST /api/votos. */
data class VotoResponse(
    val sucesso: Boolean,
    val mensagem: String,
    val voto: VotoRegistrado?
)
