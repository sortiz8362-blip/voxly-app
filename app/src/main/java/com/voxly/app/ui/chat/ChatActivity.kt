package com.voxly.app.ui.chat

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.voxly.app.R
import com.voxly.app.data.model.MensajeData
import com.voxly.app.data.remote.AppwriteConfig
import com.voxly.app.utils.Constantes
import io.appwrite.ID
import io.appwrite.Query
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatActivity : AppCompatActivity() {

    private lateinit var rvMensajes: RecyclerView
    private lateinit var etMensaje: EditText
    private lateinit var adapter: MensajeAdapter
    private var miUserId: String = ""
    private var destinatarioId: String = ""
    private var destinatarioNombre: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_chat)

        // Recibimos con quién vamos a chatear desde la pantalla anterior
        destinatarioId = intent.getStringExtra("DESTINATARIO_ID") ?: ""
        destinatarioNombre = intent.getStringExtra("DESTINATARIO_NOMBRE") ?: "Usuario"

        val tvNombreDestino = findViewById<TextView>(R.id.tvNombreDestino)
        val btnVolver = findViewById<ImageView>(R.id.btnVolverChat)
        val btnEnviar = findViewById<FloatingActionButton>(R.id.btnEnviarMensaje)
        etMensaje = findViewById(R.id.etMensaje)
        rvMensajes = findViewById(R.id.rvMensajes)

        tvNombreDestino.text = destinatarioNombre
        btnVolver.setOnClickListener { finish() }

        rvMensajes.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            try {
                miUserId = AppwriteConfig.account.get().id
                descargarMensajes()
            } catch (e: Exception) { }
        }

        btnEnviar.setOnClickListener {
            val texto = etMensaje.text.toString().trim()
            if (texto.isNotEmpty()) {
                enviarMensaje(texto)
            }
        }
    }

    private fun descargarMensajes() {
        lifecycleScope.launch {
            try {
                val respuesta = AppwriteConfig.databases.listDocuments(
                    databaseId = Constantes.DATABASE_ID,
                    collectionId = Constantes.COLECCION_MENSAJES,
                    queries = listOf(Query.orderAsc("fecha"), Query.limit(100))
                )

                val todosLosMensajes = respuesta.documents.map { doc ->
                    val map = doc.data
                    MensajeData(
                        id = doc.id,
                        remitenteId = map["remitente_id"]?.toString() ?: "",
                        destinatarioId = map["destinatario_id"]?.toString() ?: "",
                        texto = map["texto"]?.toString() ?: "",
                        fecha = map["fecha"]?.toString() ?: ""
                    )
                }

                // Filtramos solo la conversación entre tú y la otra persona
                val misMensajes = todosLosMensajes.filter {
                    (it.remitenteId == miUserId && it.destinatarioId == destinatarioId) ||
                            (it.remitenteId == destinatarioId && it.destinatarioId == miUserId)
                }

                runOnUiThread {
                    adapter = MensajeAdapter(misMensajes, miUserId)
                    rvMensajes.adapter = adapter
                    // Hacer scroll automático hacia abajo (al último mensaje)
                    if (misMensajes.isNotEmpty()) {
                        rvMensajes.scrollToPosition(misMensajes.size - 1)
                    }
                }
            } catch (e: Exception) { }
        }
    }

    private fun enviarMensaje(texto: String) {
        etMensaje.text.clear() // Limpiamos la barra al enviar
        lifecycleScope.launch {
            try {
                val fechaActual = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())
                val datosMensaje = mapOf(
                    "remitente_id" to miUserId,
                    "destinatario_id" to destinatarioId,
                    "texto" to texto,
                    "fecha" to fechaActual
                )

                AppwriteConfig.databases.createDocument(
                    databaseId = Constantes.DATABASE_ID,
                    collectionId = Constantes.COLECCION_MENSAJES,
                    documentId = ID.unique(),
                    data = datosMensaje
                )

                descargarMensajes() // Recargamos la lista para ver nuestro mensaje enviado

            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this@ChatActivity, "Error al enviar", Toast.LENGTH_SHORT).show() }
            }
        }
    }
}