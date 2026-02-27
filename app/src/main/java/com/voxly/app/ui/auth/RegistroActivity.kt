package com.voxly.app.ui.auth

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.voxly.app.R
import com.voxly.app.data.remote.AppwriteConfig
import com.voxly.app.utils.Constantes
import io.appwrite.ID
import io.appwrite.Query
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class RegistroActivity : AppCompatActivity() {

    private var pasoActual = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_registro)

        val tvAtras = findViewById<TextView>(R.id.tvAtras)
        val tvPasoIndicador = findViewById<TextView>(R.id.tvPasoIndicador)

        val layoutPaso1 = findViewById<LinearLayout>(R.id.layoutPaso1)
        val layoutPaso2 = findViewById<LinearLayout>(R.id.layoutPaso2)
        val layoutPaso3 = findViewById<LinearLayout>(R.id.layoutPaso3)
        val layoutPaso4 = findViewById<LinearLayout>(R.id.layoutPaso4)

        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etFechaNac = findViewById<EditText>(R.id.etFechaNacimiento)
        val etGenero = findViewById<EditText>(R.id.etGenero)
        val btnSiguiente1 = findViewById<Button>(R.id.btnSiguiente1)

        val etPais = findViewById<EditText>(R.id.etPais)
        val etEstado = findViewById<EditText>(R.id.etEstado)
        val btnSiguiente2 = findViewById<Button>(R.id.btnSiguiente2)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val btnSiguiente3 = findViewById<Button>(R.id.btnSiguiente3)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnFinalizar = findViewById<Button>(R.id.btnFinalizar)

        // --- MAGIA 1: CALENDARIO NATIVO ---
        etFechaNac.setOnClickListener {
            val calendario = Calendar.getInstance()
            val year = calendario.get(Calendar.YEAR)
            val month = calendario.get(Calendar.MONTH)
            val day = calendario.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(this, { _, yearSeleccionado, monthSeleccionado, daySeleccionado ->
                // Formateamos la fecha seleccionada
                val fecha = "$daySeleccionado/${monthSeleccionado + 1}/$yearSeleccionado"
                etFechaNac.setText(fecha)
            }, year, month, day)
            datePicker.show()
        }

        // --- MAGIA 2: MENÚ DE GÉNERO ---
        etGenero.setOnClickListener {
            val opciones = arrayOf("Masculino", "Femenino", "No binario", "Prefiero no decirlo")
            AlertDialog.Builder(this)
                .setTitle("Selecciona tu género")
                .setItems(opciones) { _, which ->
                    etGenero.setText(opciones[which])
                }
                .show()
        }

        // --- MAGIA 3: LISTA DE PAÍSES NATIVA ---
        etPais.setOnClickListener {
            // Obtenemos todos los países del mundo gracias al sistema de Android
            val codigosISO = Locale.getISOCountries()
            val listaPaises = codigosISO.map { Locale("", it).displayCountry }.sorted().toTypedArray()

            AlertDialog.Builder(this)
                .setTitle("Selecciona tu país")
                .setItems(listaPaises) { _, which ->
                    etPais.setText(listaPaises[which])
                }
                .show()
        }

        // --- LÓGICA DE NAVEGACIÓN ---
        fun actualizarVistaPasos() {
            tvPasoIndicador.text = "Paso $pasoActual de 4"
            layoutPaso1.visibility = View.GONE
            layoutPaso2.visibility = View.GONE
            layoutPaso3.visibility = View.GONE
            layoutPaso4.visibility = View.GONE

            when (pasoActual) {
                1 -> layoutPaso1.visibility = View.VISIBLE
                2 -> layoutPaso2.visibility = View.VISIBLE
                3 -> layoutPaso3.visibility = View.VISIBLE
                4 -> layoutPaso4.visibility = View.VISIBLE
            }
        }

        tvAtras.setOnClickListener {
            if (pasoActual > 1) {
                pasoActual--
                actualizarVistaPasos()
            } else {
                finish()
            }
        }

        btnSiguiente1.setOnClickListener {
            if (etNombre.text.isEmpty() || etFechaNac.text.isEmpty() || etGenero.text.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            pasoActual = 2
            actualizarVistaPasos()
        }

        btnSiguiente2.setOnClickListener {
            if (etPais.text.isEmpty()) {
                Toast.makeText(this, "Selecciona tu país", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            pasoActual = 3
            actualizarVistaPasos()
        }

        btnSiguiente3.setOnClickListener {
            if (etEmail.text.isEmpty() || !etEmail.text.contains("@")) {
                Toast.makeText(this, "Ingresa un correo válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            pasoActual = 4
            actualizarVistaPasos()
        }

        // --- LÓGICA FINAL (APPWRITE) ---
        btnFinalizar.setOnClickListener {
            val username = etUsername.text.toString().trim().lowercase()
            val password = etPassword.text.toString()

            if (username.isEmpty() || password.length < 8) {
                Toast.makeText(this, "El usuario es obligatorio y la clave de 8 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnFinalizar.text = "Creando cuenta..."
            btnFinalizar.isEnabled = false

            lifecycleScope.launch {
                try {
                    val checkUsername = AppwriteConfig.databases.listDocuments(
                        databaseId = Constantes.DATABASE_ID,
                        collectionId = Constantes.COLECCION_PERFILES,
                        queries = listOf(Query.equal("username", username))
                    )

                    if (checkUsername.documents.isNotEmpty()) {
                        runOnUiThread {
                            Toast.makeText(this@RegistroActivity, "El usuario @$username ya existe", Toast.LENGTH_LONG).show()
                            btnFinalizar.text = "Finalizar y crear cuenta"
                            btnFinalizar.isEnabled = true
                        }
                        return@launch
                    }

                    val emailLimpio = etEmail.text.toString().trim()
                    val nombreLimpio = etNombre.text.toString().trim()

                    val nuevaCuenta = AppwriteConfig.account.create(
                        userId = ID.unique(),
                        email = emailLimpio,
                        password = password,
                        name = nombreLimpio
                    )

                    AppwriteConfig.account.createEmailPasswordSession(emailLimpio, password)

                    val datosPerfil = mapOf(
                        "usuario_id" to nuevaCuenta.id,
                        "nombre" to nombreLimpio,
                        "username" to username,
                        "fecha_nacimiento" to etFechaNac.text.toString(),
                        "genero" to etGenero.text.toString(),
                        "pais" to etPais.text.toString(),
                        "estado" to etEstado.text.toString().trim(),
                        "bio" to "",
                        "foto_url" to "",
                        "portada_url" to ""
                    )

                    AppwriteConfig.databases.createDocument(
                        databaseId = Constantes.DATABASE_ID,
                        collectionId = Constantes.COLECCION_PERFILES,
                        documentId = ID.unique(),
                        data = datosPerfil
                    )

                    runOnUiThread {
                        Toast.makeText(this@RegistroActivity, "¡Bienvenido a Voxly!", Toast.LENGTH_LONG).show()
                        finish()
                    }

                } catch (error: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@RegistroActivity, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                        btnFinalizar.text = "Finalizar y crear cuenta"
                        btnFinalizar.isEnabled = true
                    }
                }
            }
        }
    }
}