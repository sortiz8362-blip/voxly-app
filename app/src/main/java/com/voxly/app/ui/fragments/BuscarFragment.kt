package com.voxly.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
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

class BuscarFragment : Fragment() {

    private lateinit var adapter: TweetAdapter
    private lateinit var rvResultados: RecyclerView
    private lateinit var tvMensaje: TextView
    private var miUserId: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_buscar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val svBuscador = view.findViewById<SearchView>(R.id.svBuscador)
        rvResultados = view.findViewById(R.id.rvResultadosBusqueda)
        tvMensaje = view.findViewById(R.id.tvMensajeBuscar)

        rvResultados.layoutManager = LinearLayoutManager(requireContext())
        adapter = TweetAdapter(emptyList(), miUserId, { _ -> }, { _ -> }, { _ -> })
        rvResultados.adapter = adapter

        // Obtener ID del usuario actual para el adaptador
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                miUserId = AppwriteConfig.account.get().id
            } catch (e: Exception) { }
        }

        // Configurar el listener del teclado para cuando el usuario busque
        svBuscador.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    buscarEnAppwrite(query)
                    svBuscador.clearFocus() // Oculta el teclado
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Opcional: Podríamos buscar en tiempo real mientras escribe,
                // pero para no saturar Appwrite, esperaremos a que presione "Buscar"
                if (newText.isNullOrEmpty()) {
                    rvResultados.visibility = View.GONE
                    tvMensaje.visibility = View.VISIBLE
                    tvMensaje.text = "Busca temas, usuarios o tweets"
                }
                return true
            }
        })
    }

    private fun buscarEnAppwrite(palabraClave: String) {
        tvMensaje.text = "Buscando..."
        tvMensaje.visibility = View.VISIBLE
        rvResultados.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Usamos solo Query.search para probar si el error era por mezclarlo con orderDesc
                val respuesta = AppwriteConfig.databases.listDocuments(
                    databaseId = Constantes.DATABASE_ID,
                    collectionId = Constantes.COLECCION_TWEETS,
                    queries = listOf(
                        Query.search("texto", palabraClave)
                        // Hemos ocultado Query.orderDesc("fecha") temporalmente
                    )
                )

                val tweetsEncontrados = respuesta.documents.map { doc ->
                    val map = doc.data
                    TweetData(
                        id = doc.id,
                        usuarioId = map["usuario_id"]?.toString() ?: "",
                        nombre = map["nombre"]?.toString() ?: "",
                        username = map["username"]?.toString() ?: "",
                        texto = map["texto"]?.toString() ?: "",
                        fecha = map["fecha"]?.toString() ?: "",
                        likesUsuarios = (map["likes_usuarios"] as? List<*>)?.map { it.toString() } ?: emptyList(),
                        retweetsUsuarios = (map["retweets_usuarios"] as? List<*>)?.map { it.toString() } ?: emptyList(),
                        cantidadComentarios = (map["cantidad_comentarios"] as? Number)?.toInt() ?: 0,
                        mediaUrl = map["media_url"]?.toString()
                    )
                }

                activity?.runOnUiThread {
                    if (tweetsEncontrados.isEmpty()) {
                        tvMensaje.text = "No se encontraron resultados para '$palabraClave'"
                        tvMensaje.visibility = View.VISIBLE
                        rvResultados.visibility = View.GONE
                    } else {
                        tvMensaje.visibility = View.GONE
                        rvResultados.visibility = View.VISIBLE
                        adapter = TweetAdapter(tweetsEncontrados, miUserId, { _ -> }, { _ -> }, { _ -> })
                        rvResultados.adapter = adapter
                    }
                }

            } catch (e: Exception) {
                activity?.runOnUiThread {
                    // ¡AQUÍ ESTÁ EL TRUCO! Ahora mostraremos el error exacto que envía Appwrite
                    Toast.makeText(requireContext(), "Fallo Appwrite: ${e.message}", Toast.LENGTH_LONG).show()
                    tvMensaje.text = "Error de Appwrite:\n${e.message}"
                    tvMensaje.visibility = View.VISIBLE
                    rvResultados.visibility = View.GONE
                }
            }
        }
    }
}