package br.ufpr.oscarapp.view

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import br.ufpr.oscarapp.R
import br.ufpr.oscarapp.controller.VotoController
import br.ufpr.oscarapp.databinding.ActivityConfirmarVotoBinding
import br.ufpr.oscarapp.model.Resultado
import br.ufpr.oscarapp.model.Sessao

/**
 * Final step: shows the chosen film and director, takes the login token, and
 * sends the vote to the web service. The server response decides the outcome,
 * shown in an AlertDialog. Once confirmed, all editing is locked.
 */
class ConfirmarVotoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfirmarVotoBinding
    private val controller by lazy { VotoController(lifecycleScope) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfirmarVotoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnConfirmar.setOnClickListener { confirmar() }
    }

    override fun onResume() {
        super.onResume()
        atualizarResumo()
    }

    /** Reflects the (possibly changed) local choices and the lock state. */
    private fun atualizarResumo() {
        val nenhum = getString(R.string.cv_nenhum)
        binding.tvFilmeVotado.text = Sessao.filmeSelecionado?.nome ?: nenhum
        binding.tvDiretorVotado.text = Sessao.diretorSelecionado?.nome ?: nenhum

        if (Sessao.votoConfirmado) {
            bloquearEdicao()
        }
    }

    private fun confirmar() {
        val filme = Sessao.filmeSelecionado
        val diretor = Sessao.diretorSelecionado
        if (filme == null || diretor == null) {
            mostrarErro(getString(R.string.cv_falta_selecao))
            return
        }

        val tokenTexto = binding.etToken.text?.toString()?.trim().orEmpty()
        if (tokenTexto.isEmpty()) {
            binding.tilToken.error = getString(R.string.cv_token_obrigatorio)
            return
        }
        binding.tilToken.error = null
        val token = tokenTexto.toIntOrNull()
        if (token == null) {
            binding.tilToken.error = getString(R.string.cv_token_obrigatorio)
            return
        }

        mostrarCarregando(true)
        controller.confirmarVoto(filme.id, diretor.id, token) { resultado ->
            mostrarCarregando(false)
            when (resultado) {
                is Resultado.Sucesso -> mostrarSucesso(resultado.dado.mensagem) {
                    bloquearEdicao()
                }
                is Resultado.Erro -> mostrarErro(resultado.mensagem)
            }
        }
    }

    private fun bloquearEdicao() {
        binding.etToken.isEnabled = false
        binding.tilToken.isEnabled = false
        binding.btnConfirmar.isEnabled = false
        binding.btnConfirmar.text = getString(R.string.cv_voto_bloqueado)
    }

    private fun mostrarCarregando(carregando: Boolean) {
        binding.progressConfirmar.visibility = if (carregando) View.VISIBLE else View.GONE
        binding.btnConfirmar.isEnabled = !carregando && !Sessao.votoConfirmado
    }
}
