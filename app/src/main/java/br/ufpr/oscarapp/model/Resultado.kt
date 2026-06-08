package br.ufpr.oscarapp.model

/**
 * Outcome of a controller operation handed back to the View.
 * Keeps the Activities free of any networking / parsing concerns.
 */
sealed class Resultado<out T> {
    data class Sucesso<T>(val dado: T) : Resultado<T>()
    data class Erro(val mensagem: String) : Resultado<Nothing>()
}
