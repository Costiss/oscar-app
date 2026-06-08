package br.ufpr.oscarapp.view

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import br.ufpr.oscarapp.R
import br.ufpr.oscarapp.databinding.ActivityDetalheFilmeBinding
import br.ufpr.oscarapp.model.Filme
import br.ufpr.oscarapp.model.Sessao
import com.bumptech.glide.Glide

/**
 * Shows a single film in detail (larger poster, name, genre) and lets the user
 * pick it as their candidate. The choice is stored locally in [Sessao] and can
 * be changed until the final confirmation — unless the vote is already locked.
 */
class DetalheFilmeActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FILME = "extra_filme"
    }

    private lateinit var binding: ActivityDetalheFilmeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetalheFilmeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        @Suppress("DEPRECATION")
        val filme = intent.getSerializableExtra(EXTRA_FILME) as? Filme
        if (filme == null) {
            finish()
            return
        }

        binding.tvNomeDetalhe.text = filme.nome
        binding.tvGeneroDetalhe.text = filme.genero
        Glide.with(this).load(filme.foto).centerCrop().into(binding.ivPosterGrande)

        // After a confirmed vote the user may view but not change the choice.
        if (Sessao.votoConfirmado) {
            binding.btnVotarFilme.isEnabled = false
            binding.btnVotarFilme.text = getString(R.string.cv_voto_bloqueado)
        }

        binding.btnVotarFilme.setOnClickListener {
            Sessao.filmeSelecionado = filme
            Toast.makeText(
                this,
                getString(R.string.filme_selecionado, filme.nome),
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }
}
