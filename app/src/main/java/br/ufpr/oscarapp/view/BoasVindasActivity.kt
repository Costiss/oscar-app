package br.ufpr.oscarapp.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import br.ufpr.oscarapp.R
import br.ufpr.oscarapp.databinding.ActivityBoasVindasBinding
import br.ufpr.oscarapp.model.Sessao

/**
 * Central navigation hub shown after a successful login. Displays the local
 * Oscar trophy image, the session token returned by the server, and the four
 * action buttons (Votar Filme, Votar Diretor, Confirmar Voto, Sair).
 */
class BoasVindasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBoasVindasBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBoasVindasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvSaudacao.text =
            getString(R.string.bv_saudacao, Sessao.usuario?.nome ?: "")
        binding.tvToken.text = Sessao.token?.toString() ?: "—"

        // When the user has already voted, inform them on the welcome screen and
        // keep the registered choice visible. Editing stays locked everywhere.
        if (Sessao.votoConfirmado) {
            binding.tvVotoRegistrado.visibility = View.VISIBLE
            binding.tvVotoRegistrado.text = getString(
                R.string.bv_voto_registrado,
                Sessao.filmeSelecionado?.nome ?: "",
                Sessao.diretorSelecionado?.nome ?: ""
            )
        }

        binding.btnVotarFilme.setOnClickListener {
            startActivity(Intent(this, VotarFilmeActivity::class.java))
        }
        binding.btnVotarDiretor.setOnClickListener {
            startActivity(Intent(this, VotarDiretorActivity::class.java))
        }
        binding.btnConfirmarVoto.setOnClickListener {
            startActivity(Intent(this, ConfirmarVotoActivity::class.java))
        }
        binding.btnSair.setOnClickListener { sair() }
    }

    private fun sair() {
        Sessao.limpar()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
