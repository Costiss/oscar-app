package br.ufpr.oscarapp.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import br.ufpr.oscarapp.controller.FilmeController
import br.ufpr.oscarapp.databinding.ActivityVotarFilmeBinding
import br.ufpr.oscarapp.model.Filme
import br.ufpr.oscarapp.model.Resultado

/**
 * Loads the film catalog from the web service, shows a ProgressBar while
 * loading, and renders a list (poster + name + genre) for any number of
 * items. Tapping an item opens the film details screen.
 */
class VotarFilmeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVotarFilmeBinding
    private val controller by lazy { FilmeController(lifecycleScope) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVotarFilmeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvFilmes.layoutManager = LinearLayoutManager(this)
        carregarFilmes()
    }

    private fun carregarFilmes() {
        mostrarCarregando(true)
        controller.carregarFilmes { resultado ->
            mostrarCarregando(false)
            when (resultado) {
                is Resultado.Sucesso -> exibirFilmes(resultado.dado)
                is Resultado.Erro -> mostrarErro(resultado.mensagem)
            }
        }
    }

    private fun exibirFilmes(filmes: List<Filme>) {
        binding.rvFilmes.visibility = View.VISIBLE
        binding.rvFilmes.adapter = FilmeAdapter(filmes) { filme ->
            val intent = Intent(this, DetalheFilmeActivity::class.java)
            intent.putExtra(DetalheFilmeActivity.EXTRA_FILME, filme)
            startActivity(intent)
        }
    }

    private fun mostrarCarregando(carregando: Boolean) {
        binding.progressFilmes.visibility = if (carregando) View.VISIBLE else View.GONE
    }
}
