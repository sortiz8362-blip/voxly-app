package com.voxly.app.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.voxly.app.R
import com.voxly.app.data.model.TweetData
import com.voxly.app.data.remote.AppwriteConfig
import com.voxly.app.utils.Constantes
import io.appwrite.Query
import kotlinx.coroutines.launch

class MuroTweetsActivity : AppCompatActivity() {

    private lateinit var adapter: TweetAdapter
    private var miUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_muro_tweets)

        val rvTweets = findViewById<RecyclerView>(R.id.rvTweets)
        rvTweets.layoutManager = LinearLayoutManager(this)

        val fabRedactar = findViewById<FloatingActionButton>(R.id.fabRedactar)

        adapter = TweetAdapter(emptyList(), miUserId,
            onLikeClick = { tweet -> guardarLikeSilencioso(tweet) },
            onRetweetClick = { tweet -> guardarRetweetSilencioso(tweet) },
            onShareClick = { tweet -> compartirTweetNativo(tweet) }
        )
        rvTweets.adapter = adapter

        descargarTweetsReales()

        fabRedactar.setOnClickListener {
            Toast.makeText(this, "Pronto armaremos la pantalla para escribir", Toast.LENGTH_SHORT).show()
        }
    }

    private fun descargarTweetsReales() {
        lifecycleScope.launch {
            try {
                val cuenta = AppwriteConfig.account.get()
                miUserId = cuenta.id

                val respuesta = AppwriteConfig.databases.listDocuments(
                    databaseId = Constantes.DATABASE_ID,
                    collectionId = Constantes.COLECCION_TWEETS,
                    queries = listOf(Query.orderDesc("fecha"))
                )

                val tweets = respuesta.documents.map { doc ->
                    val map = doc.data
                    val likes = (map["likes_usuarios"] as? List<*>)?.map { it.toString() } ?: emptyList()
                    val rts = (map["retweets_usuarios"] as? List<*>)?.map { it.toString() } ?: emptyList()
                    val comentarios = (map["cantidad_comentarios"] as? Number)?.toInt() ?: 0

                    TweetData(
                        id = doc.id,
                        usuarioId = map["usuario_id"]?.toString() ?: "",
                        nombre = map["nombre"]?.toString() ?: "Usuario Voxly", // Recuperado
                        username = map["username"]?.toString() ?: "",          // Recuperado
                        texto = map["texto"]?.toString() ?: "",
                        fecha = map["fecha"]?.toString() ?: "",
                        likesUsuarios = likes,
                        retweetsUsuarios = rts,
                        cantidadComentarios = comentarios,
                        mediaUrl = map["media_url"]?.toString()
                    )
                }

                runOnUiThread {
                    adapter = TweetAdapter(tweets, miUserId,
                        onLikeClick = { tweet -> guardarLikeSilencioso(tweet) },
                        onRetweetClick = { tweet -> guardarRetweetSilencioso(tweet) },
                        onShareClick = { tweet -> compartirTweetNativo(tweet) }
                    )
                    findViewById<RecyclerView>(R.id.rvTweets).adapter = adapter
                }

            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this@MuroTweetsActivity, "Error al descargar feed", Toast.LENGTH_LONG).show() }
            }
        }
    }

    // --- BASE DE DATOS EN SEGUNDO PLANO ---
    // Ya no recargamos la pantalla, solo guardamos el dato en internet silenciosamente
    private fun guardarLikeSilencioso(tweet: TweetData) {
        lifecycleScope.launch {
            try {
                AppwriteConfig.databases.updateDocument(
                    databaseId = Constantes.DATABASE_ID,
                    collectionId = Constantes.COLECCION_TWEETS,
                    documentId = tweet.id,
                    data = mapOf("likes_usuarios" to tweet.likesUsuarios)
                )
            } catch (e: Exception) { }
        }
    }

    private fun guardarRetweetSilencioso(tweet: TweetData) {
        lifecycleScope.launch {
            try {
                AppwriteConfig.databases.updateDocument(
                    databaseId = Constantes.DATABASE_ID,
                    collectionId = Constantes.COLECCION_TWEETS,
                    documentId = tweet.id,
                    data = mapOf("retweets_usuarios" to tweet.retweetsUsuarios)
                )
            } catch (e: Exception) { }
        }
    }

    // --- COMPARTIR NATIVO ---
    private fun compartirTweetNativo(tweet: TweetData) {
        val textoACompartir = "Mira este tweet en Voxly:\n\n\"${tweet.texto}\""

        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, textoACompartir)
            type = "text/plain"
        }

        // Esto abre la ventana que te pregunta si quieres mandarlo por WhatsApp, Gmail, etc.
        startActivity(Intent.createChooser(intent, "Compartir tweet vía..."))
    }
}