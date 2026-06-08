package br.ufpr.oscarapp.controller

import br.ufpr.oscarapp.model.dto.ApiError
import com.google.gson.Gson
import retrofit2.Response

/**
 * Shared helpers for controllers: turns a Retrofit error [Response] into the
 * human-readable message carried by the API's standard error envelope.
 */
abstract class BaseController {

    protected fun <T> mensagemDeErro(response: Response<T>): String {
        val raw = response.errorBody()?.string()
        if (!raw.isNullOrBlank()) {
            try {
                val erro = Gson().fromJson(raw, ApiError::class.java)
                if (!erro?.erro.isNullOrBlank()) return erro.erro!!
            } catch (_: Exception) {
                // fall through to the generic message below
            }
        }
        return "Erro inesperado (HTTP ${response.code()})"
    }
}
