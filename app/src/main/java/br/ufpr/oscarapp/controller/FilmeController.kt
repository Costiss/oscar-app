package br.ufpr.oscarapp.controller

import br.ufpr.oscarapp.model.Filme
import br.ufpr.oscarapp.model.Resultado
import br.ufpr.oscarapp.model.remote.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Loads the film catalog from GET /api/filmes. */
class FilmeController(private val scope: CoroutineScope) : BaseController() {

    fun carregarFilmes(onResultado: (Resultado<List<Filme>>) -> Unit) {
        scope.launch {
            val resultado = try {
                val response = RetrofitClient.api.listarFilmes()
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
