package br.ufpr.oscarapp.controller

import br.ufpr.oscarapp.model.Diretor
import br.ufpr.oscarapp.model.Resultado
import br.ufpr.oscarapp.model.remote.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Loads the director catalog from GET /api/diretores. */
class DiretorController(private val scope: CoroutineScope) : BaseController() {

    fun carregarDiretores(onResultado: (Resultado<List<Diretor>>) -> Unit) {
        scope.launch {
            val resultado = try {
                val response = RetrofitClient.api.listarDiretores()
                val body = response.body()
                if (response.isSuccessful && body != null) {
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
