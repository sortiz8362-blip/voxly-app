package com.voxly.app.ui.profile

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

class PerfilActivity : AppCompatActivity() {

    private lateinit var adapter: TweetAdapter
    private var miUserId: String = ""
    private lateinit var rvTweetsPerfil: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_perfil)

        val btnVolver = findViewById<ImageView>(R.id.btnVolver)
        val tvNombrePerfil = findViewById<TextView>(R.id.tvNombrePerfil)
        val tvUsernamePerfil = findViewById<TextView>(R.id.tvUsernamePerfil)
        rvTweetsPerfil = findViewById(R.id.rvTweetsPerfil)

        rvTweetsPerfil.layoutManager = LinearLayoutManager(this)

        btnVolver.setOnClickListener { finish() }

        // Inicializar el adaptador vacío
        adapter = TweetAdapter(emptyList(), "", { _ -> }, { _ -> }, { _ -> })
        rvTweetsPerfil.adapter = adapter

        // Cargar los datos del perfil y los tweets
        cargarDatosYPosts(tvNombrePerfil, tvUsernamePerfil)
    }

    private fun cargarDatosYPosts(tvNombre: TextView, tvUsername: TextView) {
        lifecycleScope.launch {
            try {
                // 1. Obtener la info del usuario actual
                val cuenta = AppwriteConfig.account.get()
                miUserId = cuenta.id

                runOnUiThread {
                    tvNombre.text = cuenta.name
                    tvUsername.text = "@${cuenta.name.replace(" ", "").lowercase()}"
                }
                val idDueñoPerfil = intent.getStringExtra("USUARIO_ID") ?: miUserId

                runOnUiThread {
                    val btnAccion = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAccionPerfil)
                    if (idDueñoPerfil == miUserId) {
                        btnAccion.text = "Editar perfil"
                    } else {
                        btnAccion.text = "Enviar Mensaje"
                        btnAccion.setOnClickListener {
                            val intentChat = Intent(this@PerfilActivity, com.voxly.app.ui.chat.ChatActivity::class.java).apply {
                                putExtra("DESTINATARIO_ID", idDueñoPerfil)
                                putExtra("DESTINATARIO_NOMBRE", tvNombre.text.toString())
                            }
                            startActivity(intentChat)
                        }
                    }
                }

                // 2. Descargar SOLO los tweets de este usuario (Pestaña "Posts")
                val respuesta = AppwriteConfig.databases.listDocuments(
                    databaseId = Constantes.DATABASE_ID,
                    collectionId = Constantes.COLECCION_TWEETS,
                    // Filtramos donde "usuario_id" sea igual a miUserId
                    queries = listOf(
                        Query.equal("usuario_id", miUserId),
                        Query.orderDesc("fecha")
                    )
                )

                val misTweets = respuesta.documents.map { doc ->
                    val map = doc.data
                    val likes = (map["likes_usuarios"] as? List<*>)?.map { it.toString() } ?: emptyList()
                    val rts = (map["retweets_usuarios"] as? List<*>)?.map { it.toString() } ?: emptyList()

                    TweetData(
                        id = doc.id,
                        usuarioId = map["usuario_id"]?.toString() ?: "",
                        nombre = map["nombre"]?.toString() ?: "",
                        username = map["username"]?.toString() ?: "",
                        texto = map["texto"]?.toString() ?: "",
                        fecha = map["fecha"]?.toString() ?: "",
                        likesUsuarios = likes,
                        retweetsUsuarios = rts,
                        cantidadComentarios = (map["cantidad_comentarios"] as? Number)?.toInt() ?: 0,
                        mediaUrl = map["media_url"]?.toString()
                    )
                }

                runOnUiThread {
                    adapter = TweetAdapter(misTweets, miUserId, { _ -> }, { _ -> }, { _ -> })
                    rvTweetsPerfil.adapter = adapter
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@PerfilActivity, "Error cargando perfil", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}