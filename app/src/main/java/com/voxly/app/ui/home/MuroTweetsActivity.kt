package com.voxly.app.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.voxly.app.R
import com.voxly.app.ui.fragments.BuscarFragment
import com.voxly.app.ui.fragments.ChatsFragment
import com.voxly.app.ui.fragments.MuroFragment
import com.voxly.app.ui.fragments.NotificacionesFragment

class MuroTweetsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_muro_tweets)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        val fabRedactar = findViewById<FloatingActionButton>(R.id.fabRedactar)

        // El botón flotante abre la pantalla de crear post
        fabRedactar.setOnClickListener {
            startActivity(Intent(this, CrearPostActivity::class.java))
        }

        // Lógica de la barra inferior para cambiar de pantalla
        bottomNavigation.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_muro -> MuroFragment()
                R.id.nav_buscar -> BuscarFragment()
                R.id.nav_notificaciones -> NotificacionesFragment()
                R.id.nav_chats -> ChatsFragment()
                else -> MuroFragment()
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.contenedorFragments, fragment)
                .commit()

            true
        }

        // Cargar el muro por defecto al abrir la app (para que no salga en blanco)
        if (savedInstanceState == null) {
            bottomNavigation.selectedItemId = R.id.nav_muro
        }
    }
}