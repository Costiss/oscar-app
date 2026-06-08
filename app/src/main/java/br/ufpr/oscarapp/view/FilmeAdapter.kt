package br.ufpr.oscarapp.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.ufpr.oscarapp.databinding.ItemFilmeBinding
import br.ufpr.oscarapp.model.Filme
import com.bumptech.glide.Glide

/**
 * Renders the film catalog. Works for any number of items and loads each
 * poster asynchronously from its URL with Glide.
 */
class FilmeAdapter(
    private val filmes: List<Filme>,
    private val onClique: (Filme) -> Unit
) : RecyclerView.Adapter<FilmeAdapter.FilmeViewHolder>() {

    inner class FilmeViewHolder(val binding: ItemFilmeBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilmeViewHolder {
        val binding = ItemFilmeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FilmeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FilmeViewHolder, position: Int) {
        val filme = filmes[position]
        with(holder.binding) {
            tvNome.text = filme.nome
            tvGenero.text = filme.genero
            Glide.with(ivPoster.context)
                .load(filme.foto)
                .centerCrop()
                .into(ivPoster)
            root.setOnClickListener { onClique(filme) }
        }
    }

    override fun getItemCount(): Int = filmes.size
}
