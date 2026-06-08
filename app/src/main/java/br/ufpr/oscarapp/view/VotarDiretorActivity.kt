package br.ufpr.oscarapp.view

import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import br.ufpr.oscarapp.R
import br.ufpr.oscarapp.controller.DiretorController
import br.ufpr.oscarapp.databinding.ActivityVotarDiretorBinding
import br.ufpr.oscarapp.model.Diretor
import br.ufpr.oscarapp.model.Resultado
import br.ufpr.oscarapp.model.Sessao

/**
 * Builds a dynamic RadioGroup with one RadioButton per director loaded from
 * the web service. Exactly one director may be selected. The choice is stored
 * locally until the final confirmation.
 */
class VotarDiretorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVotarDiretorBinding
    private val controller by lazy { DiretorController(lifecycleScope) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVotarDiretorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Sessao.votoConfirmado) {
            binding.btnSelecionarDiretor.isEnabled = false
            binding.btnSelecionarDiretor.text = getString(R.string.cv_voto_bloqueado)
        }

        binding.btnSelecionarDiretor.setOnClickListener { confirmarSelecao() }
        carregarDiretores()
    }

    private fun carregarDiretores() {
        mostrarCarregando(true)
        controller.carregarDiretores { resultado ->
            mostrarCarregando(false)
            when (resultado) {
                is Resultado.Sucesso -> montarRadioGroup(resultado.dado)
                is Resultado.Erro -> mostrarErro(resultado.mensagem)
            }
        }
    }

    private fun montarRadioGroup(diretores: List<Diretor>) {
        binding.contentDiretor.visibility = View.VISIBLE
        binding.rgDiretores.removeAllViews()

        diretores.forEach { diretor ->
            val radio = RadioButton(this).apply {
                id = View.generateViewId()
                text = diretor.nome
                tag = diretor
                textSize = 16f
                isEnabled = !Sessao.votoConfirmado
                // Restore a previously made (still unconfirmed) choice.
                isChecked = Sessao.diretorSelecionado?.id == diretor.id
            }
            binding.rgDiretores.addView(radio)
        }
    }

    private fun confirmarSelecao() {
        val selecionadoId = binding.rgDiretores.checkedRadioButtonId
        if (selecionadoId == View.NO_ID) {
            Toast.makeText(this, R.string.diretor_selecione, Toast.LENGTH_SHORT).show()
            return
        }
        val radio = binding.rgDiretores.findViewById<RadioButton>(selecionadoId)
        val diretor = radio.tag as Diretor
        Sessao.diretorSelecionado = diretor
        Toast.makeText(
            this,
            getString(R.string.diretor_selecionado, diretor.nome),
            Toast.LENGTH_SHORT
        ).show()
        finish()
    }

    private fun mostrarCarregando(carregando: Boolean) {
        binding.progressDiretores.visibility = if (carregando) View.VISIBLE else View.GONE
    }
}
