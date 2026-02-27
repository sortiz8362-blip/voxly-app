package com.voxly.app.ui.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.voxly.app.R
import com.voxly.app.data.model.TweetData

class TweetAdapter(
    private var listaTweets: List<TweetData>,
    private val miUserId: String,
    private val onLikeClick: (TweetData) -> Unit,
    private val onRetweetClick: (TweetData) -> Unit,
    private val onShareClick: (TweetData) -> Unit // ¡NUEVO: Compartir!
) : RecyclerView.Adapter<TweetAdapter.TweetViewHolder>() {

    fun actualizarLista(nuevaLista: List<TweetData>) {
        listaTweets = nuevaLista
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TweetViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tweet, parent, false)
        return TweetViewHolder(view)
    }

    override fun getItemCount() = listaTweets.size

    override fun onBindViewHolder(holder: TweetViewHolder, position: Int) {
        val tweet = listaTweets[position]

        holder.tvNombre.text = tweet.nombre.ifEmpty { "Usuario Voxly" }
        holder.tvUsername.text = if (tweet.username.isNotEmpty()) "@${tweet.username}" else "· Oculto"
        holder.tvTextoTweet.text = tweet.texto

        holder.btnComentar.text = if (tweet.cantidadComentarios > 0) tweet.cantidadComentarios.toString() else ""

        // --- PINTADO VISUAL DE LIKES ---
        val leDiLike = tweet.likesUsuarios.contains(miUserId)
        holder.btnLike.text = if (tweet.likesUsuarios.isNotEmpty()) tweet.likesUsuarios.size.toString() else ""
        if (leDiLike) {
            holder.btnLike.setTextColor(Color.parseColor("#F91880"))
            holder.btnLike.compoundDrawablesRelative[0]?.setTint(Color.parseColor("#F91880"))
        } else {
            holder.btnLike.setTextColor(Color.parseColor("#888888"))
            holder.btnLike.compoundDrawablesRelative[0]?.setTint(Color.parseColor("#888888"))
        }

        // --- PINTADO VISUAL DE RETWEETS ---
        val leDiRT = tweet.retweetsUsuarios.contains(miUserId)
        holder.btnRetweet.text = if (tweet.retweetsUsuarios.isNotEmpty()) tweet.retweetsUsuarios.size.toString() else ""
        if (leDiRT) {
            holder.btnRetweet.setTextColor(Color.parseColor("#00BA7C"))
            holder.btnRetweet.compoundDrawablesRelative[0]?.setTint(Color.parseColor("#00BA7C"))
        } else {
            holder.btnRetweet.setTextColor(Color.parseColor("#888888"))
            holder.btnRetweet.compoundDrawablesRelative[0]?.setTint(Color.parseColor("#888888"))
        }

        // ==========================================
        // LOS CLICKS INSTANTÁNEOS (OPTIMISTIC UI)
        // ==========================================
        holder.btnLike.setOnClickListener {
            // 1. Cambiamos los datos localmente
            if (tweet.likesUsuarios.contains(miUserId)) {
                tweet.likesUsuarios = tweet.likesUsuarios - miUserId
            } else {
                tweet.likesUsuarios = tweet.likesUsuarios + miUserId
            }
            // 2. Refrescamos SOLO este tweet al instante (Cero lag)
            notifyItemChanged(position)
            // 3. Avisamos a la base de datos en silencio
            onLikeClick(tweet)
        }

        holder.btnRetweet.setOnClickListener {
            if (tweet.retweetsUsuarios.contains(miUserId)) {
                tweet.retweetsUsuarios = tweet.retweetsUsuarios - miUserId
            } else {
                tweet.retweetsUsuarios = tweet.retweetsUsuarios + miUserId
            }
            notifyItemChanged(position)
            onRetweetClick(tweet)
        }

        holder.btnCompartir.setOnClickListener {
            onShareClick(tweet)
        }
    }

    class TweetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombre)
        val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
        val tvTextoTweet: TextView = itemView.findViewById(R.id.tvTextoTweet)
        val btnComentar: TextView = itemView.findViewById(R.id.btnComentar)
        val btnRetweet: TextView = itemView.findViewById(R.id.btnRetweet)
        val btnLike: TextView = itemView.findViewById(R.id.btnLike)
        val btnCompartir: ImageView = itemView.findViewById(R.id.btnCompartir)
    }
}