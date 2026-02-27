package com.voxly.app.ui.home

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.voxly.app.R
import com.voxly.app.data.remote.AppwriteConfig
import com.voxly.app.utils.Constantes
import io.appwrite.ID
import io.appwrite.models.InputFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrearPostActivity : AppCompatActivity() {

    private val MAX_CHARS = 280
    private var imagenSeleccionadaUri: Uri? = null
    private lateinit var btnPublicar: Button

    // Lanzador para abrir la galería del celular
    private val abrirGaleria = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            imagenSeleccionadaUri = uri
            Toast.makeText(this, "Imagen adjuntada", Toast.LENGTH_SHORT).show()
            btnPublicar.isEnabled = true
            btnPublicar.alpha = 1.0f
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_crear_post)

        val btnCancelar = findViewById<TextView>(R.id.btnCancelar)
        btnPublicar = findViewById(R.id.btnPublicar)
        val etTextoTweet = findViewById<EditText>(R.id.etTextoTweet)
        val tvContador = findViewById<TextView>(R.id.tvContador)
        val btnAdjuntarImagen = findViewById<ImageView>(R.id.btnAdjuntarImagen)

        btnCancelar.setOnClickListener { finish() }

        // Al tocar el ícono de galería, abrimos las fotos del teléfono (solo imágenes)
        btnAdjuntarImagen.setOnClickListener {
            abrirGaleria.launch("image/*")
        }

        etTextoTweet.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val length = s?.length ?: 0
                tvContador.text = "$length/$MAX_CHARS"

                if (length == 0 && imagenSeleccionadaUri == null) {
                    btnPublicar.isEnabled = false
                    btnPublicar.alpha = 0.5f
                } else {
                    btnPublicar.isEnabled = true
                    btnPublicar.alpha = 1.0f
                }

                if (length >= MAX_CHARS) tvContador.setTextColor(Color.RED)
                else tvContador.setTextColor(getColor(android.R.color.tab_indicator_text))
            }
        })

        btnPublicar.setOnClickListener {
            val texto = etTextoTweet.text.toString().trim()
            if (texto.isNotEmpty() || imagenSeleccionadaUri != null) {
                btnPublicar.text = "Subiendo..."
                btnPublicar.isEnabled = false
                subirTweetConImagen(texto)
            }
        }
    }

    private fun subirTweetConImagen(texto: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                var urlImagenSubida: String? = null

                // Si el usuario eligió una foto, la subimos a Appwrite Storage primero
                if (imagenSeleccionadaUri != null) {
                    val inputStream = contentResolver.openInputStream(imagenSeleccionadaUri!!)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()

                    if (bytes != null) {
                        val archivoSubido = AppwriteConfig.storage.createFile(
                            bucketId = Constantes.BUCKET_TWEETS,
                            fileId = ID.unique(),
                            file = InputFile.fromBytes(bytes, "imagen_tweet.jpg")
                        )
                        // Construimos la URL pública de la imagen
                        urlImagenSubida = "${Constantes.ENDPOINT}/storage/buckets/${Constantes.BUCKET_TWEETS}/files/${archivoSubido.id}/view?project=${Constantes.PROJECT_ID}"
                    }
                }

                // Ahora guardamos el texto y la URL de la imagen en la base de datos
                val usuarioActual = AppwriteConfig.account.get()
                val fechaActual = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())

                val datosTweet = mutableMapOf<String, Any>(
                    "usuario_id" to usuarioActual.id,
                    "nombre" to usuarioActual.name,
                    "username" to "@${usuarioActual.name.replace(" ", "").lowercase()}",
                    "texto" to texto,
                    "fecha" to fechaActual,
                    "likes_usuarios" to emptyList<String>(),
                    "retweets_usuarios" to emptyList<String>(),
                    "cantidad_comentarios" to 0
                )

                if (urlImagenSubida != null) {
                    datosTweet["media_url"] = urlImagenSubida
                }

                AppwriteConfig.databases.createDocument(
                    databaseId = Constantes.DATABASE_ID,
                    collectionId = Constantes.COLECCION_TWEETS,
                    documentId = ID.unique(),
                    data = datosTweet
                )

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CrearPostActivity, "¡Publicado!", Toast.LENGTH_SHORT).show()
                    finish()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnPublicar.text = "Publicar"
                    btnPublicar.isEnabled = true
                    Toast.makeText(this@CrearPostActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}