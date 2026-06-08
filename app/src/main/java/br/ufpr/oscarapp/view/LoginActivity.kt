package br.ufpr.oscarapp.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import br.ufpr.oscarapp.controller.LoginController
import br.ufpr.oscarapp.databinding.ActivityLoginBinding
import br.ufpr.oscarapp.model.Resultado

/**
 * Entry screen. Validates the fields locally, never sends a request with an
 * empty login/senha, and consumes the login web service. On success it moves
 * to the welcome screen.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val controller by lazy { LoginController(lifecycleScope) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnEntrar.setOnClickListener { tentarLogar() }
    }

    private fun tentarLogar() {
        val login = binding.etLogin.text?.toString()?.trim().orEmpty()
        val senha = binding.etSenha.text?.toString().orEmpty()

        // Local validation — blank fields are blocked before sending.
        var valido = true
        if (login.isEmpty()) {
            binding.tilLogin.error = getString(br.ufpr.oscarapp.R.string.login_campos_obrigatorios)
            valido = false
        } else {
            binding.tilLogin.error = null
        }
        if (senha.isEmpty()) {
            binding.tilSenha.error = getString(br.ufpr.oscarapp.R.string.login_campos_obrigatorios)
            valido = false
        } else {
            binding.tilSenha.error = null
        }
        if (!valido) return

        mostrarCarregando(true)
        controller.autenticar(login, senha) { resultado ->
            mostrarCarregando(false)
            when (resultado) {
                is Resultado.Sucesso -> {
                    startActivity(Intent(this, BoasVindasActivity::class.java))
                    finish()
                }
                is Resultado.Erro -> mostrarErro(resultado.mensagem)
            }
        }
    }

    private fun mostrarCarregando(carregando: Boolean) {
        binding.progressLogin.visibility = if (carregando) View.VISIBLE else View.GONE
        binding.btnEntrar.isEnabled = !carregando
    }
}
