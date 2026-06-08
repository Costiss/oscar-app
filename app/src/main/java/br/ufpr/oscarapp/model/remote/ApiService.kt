package br.ufpr.oscarapp.model.remote

import br.ufpr.oscarapp.model.Diretor
import br.ufpr.oscarapp.model.Filme
import br.ufpr.oscarapp.model.dto.LoginRequest
import br.ufpr.oscarapp.model.dto.LoginResponse
import br.ufpr.oscarapp.model.dto.VotoRequest
import br.ufpr.oscarapp.model.dto.VotoResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit description of the Oscar App "Sistema Central" REST API.
 * Every call returns a [Response] so the controllers can inspect the
 * HTTP status code and read the error envelope when needed.
 */
interface ApiService {

    @POST("api/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @GET("api/filmes")
    suspend fun listarFilmes(): Response<List<Filme>>

    @GET("api/diretores")
    suspend fun listarDiretores(): Response<List<Diretor>>

    @POST("api/votos")
    suspend fun registrarVoto(@Body body: VotoRequest): Response<VotoResponse>
}
