package br.ufpr.oscarapp.controller

import br.ufpr.oscarapp.model.Resultado
import br.ufpr.oscarapp.model.Sessao
import br.ufpr.oscarapp.model.dto.VotoRequest
import br.ufpr.oscarapp.model.dto.VotoResponse
import br.ufpr.oscarapp.model.remote.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Sends the final vote to POST /api/votos. On success it marks the session
 * vote as confirmed so the UI can lock any further editing.
 */
class VotoController(private val scope: CoroutineScope) : BaseController() {

    fun confirmarVoto(
        filmeId: String,
        diretorId: String,
        token: Int,
        onResultado: (Resultado<VotoResponse>) -> Unit
    ) {
        scope.launch {
            val resultado = try {
                val response = RetrofitClient.api.registrarVoto(
                    VotoRequest(filmeId, diretorId, token)
                )
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    Sessao.votoConfirmado = true
                    Resultado.Sucesso(body)
                } else {
                    Resultado.Erro(mensagemDeErro(response))
                }
            } catch (e: Exception) {
                Resultado.Erro("Falha de conexão com o servidor. Tente novamente.")
            }
            onResultado(resultado)
        }
    }
}
