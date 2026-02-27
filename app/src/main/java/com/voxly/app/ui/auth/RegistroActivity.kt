package com.voxly.app.ui.auth

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
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
    // Variable para guardar la sesión temporal de Appwrite cuando enviamos el correo
    private var userIdTemporal = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_registro)

        // Textos superiores
        val tvAtras = findViewById<TextView>(R.id.tvAtras)
        val tvPasoIndicador = findViewById<TextView>(R.id.tvPasoIndicador)

        // Contenedores (Los 4 pasos)
        val layoutPaso1 = findViewById<LinearLayout>(R.id.layoutPaso1)
        val layoutPaso2 = findViewById<LinearLayout>(R.id.layoutPaso2)
        val layoutPaso3 = findViewById<LinearLayout>(R.id.layoutPaso3)
        val layoutPaso4 = findViewById<LinearLayout>(R.id.layoutPaso4)

        // Inputs Paso 1
        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etFechaNac = findViewById<EditText>(R.id.etFechaNacimiento)
        val etGenero = findViewById<EditText>(R.id.etGenero)
        val btnSiguiente1 = findViewById<Button>(R.id.btnSiguiente1)

        // Inputs Paso 2
        val etPais = findViewById<EditText>(R.id.etPais)
        val etEstado = findViewById<EditText>(R.id.etEstado)
        val btnSiguiente2 = findViewById<Button>(R.id.btnSiguiente2)

        // Inputs Paso 3 (El de tu foto)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etCodigoVerificacion = findViewById<EditText>(R.id.etCodigoVerificacion)
        val btnEnviarCodigo = findViewById<Button>(R.id.btnEnviarCodigo)
        val cbTerminos = findViewById<CheckBox>(R.id.cbTerminos)
        val btnSiguiente3 = findViewById<Button>(R.id.btnSiguiente3)

        // Inputs Paso 4
        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnFinalizar = findViewById<Button>(R.id.btnFinalizar)

        // Selectores Nativos (Fecha, Género, País)
        etFechaNac.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                etFechaNac.setText("$day/${month + 1}/$year")
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        etGenero.setOnClickListener {
            val opciones = arrayOf("Masculino", "Femenino", "No binario", "Prefiero no decirlo")
            AlertDialog.Builder(this).setTitle("Selecciona tu género").setItems(opciones) { _, w -> etGenero.setText(opciones[w]) }.show()
        }

        etPais.setOnClickListener {
            val lista = Locale.getISOCountries().map { Locale("", it).displayCountry }.sorted().toTypedArray()
            AlertDialog.Builder(this).setTitle("Selecciona tu país").setItems(lista) { _, w -> etPais.setText(lista[w]) }.show()
        }

        // Función para cambiar de vistas
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
            } else finish()
        }

        btnSiguiente1.setOnClickListener {
            if (etNombre.text.isEmpty() || etFechaNac.text.isEmpty() || etGenero.text.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            pasoActual = 2; actualizarVistaPasos()
        }

        btnSiguiente2.setOnClickListener {
            if (etPais.text.isEmpty()) {
                Toast.makeText(this, "Selecciona tu país", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            pasoActual = 3; actualizarVistaPasos()
        }

        // ==========================================
        // LA LÓGICA DEL PASO 3 (ENVIAR Y VERIFICAR)
        // ==========================================
        btnEnviarCodigo.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty() || !email.contains("@")) {
                Toast.makeText(this, "Ingresa un correo válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnEnviarCodigo.text = "Enviando..."
            btnEnviarCodigo.isEnabled = false

            lifecycleScope.launch {
                try {
                    // Borramos sesiones previas por si acaso
                    try { AppwriteConfig.account.deleteSession("current") } catch (e: Exception) {}

                    // Si no hemos creado un ID temporal, generamos uno nuevo
                    if (userIdTemporal.isEmpty()) {
                        userIdTemporal = ID.unique()
                    }

                    // Le pedimos a Appwrite que mande el código al correo
                    val token = AppwriteConfig.account.createEmailToken(
                        userId = userIdTemporal,
                        email = email
                    )

                    // Guardamos el ID real que nos dio Appwrite
                    userIdTemporal = token.userId

                    runOnUiThread {
                        Toast.makeText(this@RegistroActivity, "Código enviado, revisa tu bandeja", Toast.LENGTH_LONG).show()
                        btnEnviarCodigo.text = "Reenviar"
                        btnEnviarCodigo.isEnabled = true
                    }
                } catch (error: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@RegistroActivity, "Error al enviar: ${error.message}", Toast.LENGTH_LONG).show()
                        btnEnviarCodigo.text = "Enviar código"
                        btnEnviarCodigo.isEnabled = true
                        // Si falla porque el correo ya existe, Appwrite nos lo dirá en el mensaje
                    }
                }
            }
        }

        btnSiguiente3.setOnClickListener {
            val codigo = etCodigoVerificacion.text.toString().trim()
            if (codigo.length < 6) {
                Toast.makeText(this, "Ingresa el código de 6 números", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!cbTerminos.isChecked) {
                Toast.makeText(this, "Debes aceptar los términos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSiguiente3.text = "Verificando..."
            btnSiguiente3.isEnabled = false

            lifecycleScope.launch {
                try {
                    // Verificamos el código. Si es correcto, Appwrite INICIA SESIÓN automáticamente.
                    AppwriteConfig.account.createSession(userIdTemporal, codigo)

                    runOnUiThread {
                        Toast.makeText(this@RegistroActivity, "¡Correo verificado!", Toast.LENGTH_SHORT).show()
                        btnSiguiente3.text = "Siguiente"
                        btnSiguiente3.isEnabled = true
                        pasoActual = 4
                        actualizarVistaPasos()
                    }
                } catch (error: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@RegistroActivity, "Código incorrecto o expirado", Toast.LENGTH_LONG).show()
                        btnSiguiente3.text = "Siguiente"
                        btnSiguiente3.isEnabled = true
                    }
                }
            }
        }

        // ==========================================
        // LA LÓGICA DEL PASO 4 (FINALIZAR Y BASE DE DATOS)
        // ==========================================
        btnFinalizar.setOnClickListener {
            val username = etUsername.text.toString().trim().lowercase()
            val password = etPassword.text.toString()

            if (username.isEmpty() || password.length < 8) {
                Toast.makeText(this, "Usuario y contraseña (mínimo 8) obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnFinalizar.text = "Guardando..."
            btnFinalizar.isEnabled = false

            lifecycleScope.launch {
                try {
                    // 1. Verificamos que el username no exista en la base de datos
                    val checkUsername = AppwriteConfig.databases.listDocuments(
                        databaseId = Constantes.DATABASE_ID,
                        collectionId = Constantes.COLECCION_PERFILES,
                        queries = listOf(Query.equal("username", username))
                    )

                    if (checkUsername.documents.isNotEmpty()) {
                        runOnUiThread {
                            Toast.makeText(this@RegistroActivity, "El usuario @$username ya está ocupado", Toast.LENGTH_LONG).show()
                            btnFinalizar.text = "Finalizar y crear cuenta"
                            btnFinalizar.isEnabled = true
                        }
                        return@launch
                    }

                    // 2. Como el usuario entró con Código, actualizamos su Nombre y le ponemos su Contraseña nueva
                    AppwriteConfig.account.updateName(name = etNombre.text.toString().trim())
                    AppwriteConfig.account.updatePassword(password = password)

                    // 3. Guardamos su perfil completo en la Base de Datos "Perfiles"
                    val datosPerfil = mapOf(
                        "usuario_id" to userIdTemporal,
                        "nombre" to etNombre.text.toString().trim(),
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
                        Toast.makeText(this@RegistroActivity, "¡Cuenta creada con éxito!", Toast.LENGTH_LONG).show()
                        val intent = android.content.Intent(this@RegistroActivity, com.voxly.app.ui.home.MuroTweetsActivity::class.java)
                        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent) // Regresa al Login para que pueda entrar con su nueva contraseña
                    }

                } catch (error: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@RegistroActivity, "Error al guardar: ${error.message}", Toast.LENGTH_LONG).show()
                        btnFinalizar.text = "Finalizar y crear cuenta"
                        btnFinalizar.isEnabled = true
                    }
                }
            }
        }
    }
}