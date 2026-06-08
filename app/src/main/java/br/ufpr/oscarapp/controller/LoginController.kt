package br.ufpr.oscarapp.controller

import br.ufpr.oscarapp.model.Resultado
import br.ufpr.oscarapp.model.Sessao
import br.ufpr.oscarapp.model.dto.LoginRequest
import br.ufpr.oscarapp.model.dto.LoginResponse
import br.ufpr.oscarapp.model.remote.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Authenticates the user against POST /api/login. On success it stores the
 * user and the session token in [Sessao] before handing the result back.
 */
class LoginController(private val scope: CoroutineScope) : BaseController() {

    fun autenticar(
        login: String,
        senha: String,
        onResultado: (Resultado<LoginResponse>) -> Unit
    ) {
        scope.launch {
            val resultado = try {
                val response = RetrofitClient.api.login(LoginRequest(login, senha))
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    Sessao.iniciar(body.usuario, body.token)
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
