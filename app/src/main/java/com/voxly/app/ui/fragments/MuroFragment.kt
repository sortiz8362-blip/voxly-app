package com.voxly.app.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.voxly.app.R
import com.voxly.app.data.model.TweetData
import com.voxly.app.data.remote.AppwriteConfig
import com.voxly.app.ui.home.TweetAdapter
import com.voxly.app.utils.Constantes
import io.appwrite.Query
import kotlinx.coroutines.launch

class MuroFragment : Fragment() {

    private lateinit var adapter: TweetAdapter
    private var miUserId: String = ""
    private lateinit var rvTweets: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Conectamos el diseño XML que acabamos de crear
        return inflater.inflate(R.layout.fragment_muro, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ---> AÑADE ESTO PARA ENTRAR AL PERFIL <---
        val btnMiPerfil = view.findViewById<View>(R.id.btnMiPerfil)
        btnMiPerfil.setOnClickListener {
            startActivity(Intent(requireContext(), com.voxly.app.ui.profile.PerfilActivity::class.java))
        }
        // -------------------------------------------

        rvTweets = view.findViewById(R.id.rvTweets)
        rvTweets.layoutManager = LinearLayoutManager(requireContext())

        adapter = TweetAdapter(emptyList(), miUserId,
            onLikeClick = { tweet -> guardarLikeSilencioso(tweet) },
            onRetweetClick = { tweet -> guardarRetweetSilencioso(tweet) },
            onShareClick = { tweet -> compartirTweetNativo(tweet) }
        )
        rvTweets.adapter = adapter
    }

    // Usamos onResume para que recargue los tweets cuando vuelves de publicar uno nuevo
    override fun onResume() {
        super.onResume()
        descargarTweetsReales()
    }

    private fun descargarTweetsReales() {
        viewLifecycleOwner.lifecycleScope.launch {
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
                        nombre = map["nombre"]?.toString() ?: "Usuario Voxly",
                        username = map["username"]?.toString() ?: "",
                        texto = map["texto"]?.toString() ?: "",
                        fecha = map["fecha"]?.toString() ?: "",
                        likesUsuarios = likes,
                        retweetsUsuarios = rts,
                        cantidadComentarios = comentarios,
                        mediaUrl = map["media_url"]?.toString()
                    )
                }

                activity?.runOnUiThread {
                    adapter = TweetAdapter(tweets, miUserId,
                        onLikeClick = { tweet -> guardarLikeSilencioso(tweet) },
                        onRetweetClick = { tweet -> guardarRetweetSilencioso(tweet) },
                        onShareClick = { tweet -> compartirTweetNativo(tweet) }
                    )
                    rvTweets.adapter = adapter
                }

            } catch (e: Exception) {
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "Error al descargar feed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun guardarLikeSilencioso(tweet: TweetData) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 1. Guardar el like en el tweet (esto ya lo hacías)
                AppwriteConfig.databases.updateDocument(
                    databaseId = Constantes.DATABASE_ID,
                    collectionId = Constantes.COLECCION_TWEETS,
                    documentId = tweet.id,
                    data = mapOf("likes_usuarios" to tweet.likesUsuarios)
                )

                // 2. ENVIAR LA NOTIFICACIÓN (Solo si el tweet no es mío)
                val miCuenta = AppwriteConfig.account.get()
                if (tweet.usuarioId != miCuenta.id) {
                    val fechaActual = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault()).format(java.util.Date())

                    val datosNotificacion = mapOf(
                        "usuario_destino_id" to tweet.usuarioId, // El dueño del tweet
                        "nombre_origen" to miCuenta.name,        // Yo, el que dio el like
                        "tipo" to "like",
                        "texto_extra" to (if (tweet.texto.length > 30) tweet.texto.take(30) + "..." else tweet.texto),
                        "fecha" to fechaActual
                    )

                    AppwriteConfig.databases.createDocument(
                        databaseId = Constantes.DATABASE_ID,
                        collectionId = Constantes.COLECCION_NOTIFICACIONES,
                        documentId = io.appwrite.ID.unique(),
                        data = datosNotificacion
                    )
                }
            } catch (e: Exception) { }
        }
    }

    private fun guardarRetweetSilencioso(tweet: TweetData) {
        viewLifecycleOwner.lifecycleScope.launch {
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

    private fun compartirTweetNativo(tweet: TweetData) {
        val textoACompartir = "Mira este tweet en Voxly:\n\n\"${tweet.texto}\""
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, textoACompartir)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(intent, "Compartir tweet vía..."))
    }
}