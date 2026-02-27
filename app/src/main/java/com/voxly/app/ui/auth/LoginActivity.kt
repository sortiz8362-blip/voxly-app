package com.voxly.app.ui.auth

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.voxly.app.R
import com.voxly.app.data.remote.AppwriteConfig
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Escondemos la barra superior morada por defecto de Android para un diseño más limpio
        supportActionBar?.hide()
        // Conectamos esta lógica con el XML visual que acabas de crear
        setContentView(R.layout.activity_login)

        // 1. Encendemos el motor de Appwrite
        AppwriteConfig.inicializar(this)

        // 2. Vinculamos los elementos del diseño a variables
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvRegistrarse = findViewById<TextView>(R.id.tvRegistrarse)

        // 3. ¿Qué pasa al hacer clic en Login?
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Escribe tu correo y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // En Android, las consultas a internet se hacen en un "hilo secundario" (lifecycleScope)
            lifecycleScope.launch {
                try {
                    // Borramos sesiones fantasma previas
                    try { AppwriteConfig.account.deleteSession("current") } catch (e: Exception) {}

                    // Intentamos hacer el login real
                    AppwriteConfig.account.createEmailPasswordSession(email, password)

                    // Si el login es exitoso, volvemos al hilo principal para mostrar un mensaje
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "¡Conectado a Appwrite con éxito!", Toast.LENGTH_LONG).show()
                        // En el siguiente paso haremos que te lleve a la pantalla del Muro
                    }
                } catch (error: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        tvRegistrarse.setOnClickListener {
            // El 'Intent' es el mensajero de Android que abre nuevas pantallas
            val intent = android.content.Intent(this, RegistroActivity::class.java)
            startActivity(intent)
        }
    }
}