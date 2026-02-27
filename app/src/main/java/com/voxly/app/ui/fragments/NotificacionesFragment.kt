package com.voxly.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.voxly.app.R
import com.voxly.app.data.model.NotificacionData
import com.voxly.app.data.remote.AppwriteConfig
import com.voxly.app.utils.Constantes
import io.appwrite.Query
import kotlinx.coroutines.launch

class NotificacionesFragment : Fragment() {

    private lateinit var rvNotificaciones: RecyclerView
    private lateinit var llSinNotificaciones: LinearLayout
    private var miUserId: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_notificaciones, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvNotificaciones = view.findViewById(R.id.rvNotificaciones)
        llSinNotificaciones = view.findViewById(R.id.llSinNotificaciones)

        rvNotificaciones.layoutManager = LinearLayoutManager(requireContext())

        // Descargar notificaciones al abrir la pestaña
        descargarNotificaciones()
    }

    private fun descargarNotificaciones() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 1. Obtener mi ID
                miUserId = AppwriteConfig.account.get().id

                // 2. Buscar en Appwrite donde el destino sea yo
                val respuesta = AppwriteConfig.databases.listDocuments(
                    databaseId = Constantes.DATABASE_ID,
                    collectionId = Constantes.COLECCION_NOTIFICACIONES,
                    queries = listOf(
                        Query.equal("usuario_destino_id", miUserId),
                        Query.orderDesc("fecha")
                    )
                )

                val listaAvisos = respuesta.documents.map { doc ->
                    val map = doc.data
                    NotificacionData(
                        id = doc.id,
                        usuarioDestinoId = map["usuario_destino_id"]?.toString() ?: "",
                        nombreOrigen = map["nombre_origen"]?.toString() ?: "Alguien",
                        tipo = map["tipo"]?.toString() ?: "",
                        textoExtra = map["texto_extra"]?.toString() ?: "",
                        fecha = map["fecha"]?.toString() ?: ""
                    )
                }

                activity?.runOnUiThread {
                    if (listaAvisos.isEmpty()) {
                        llSinNotificaciones.visibility = View.VISIBLE
                        rvNotificaciones.visibility = View.GONE
                    } else {
                        llSinNotificaciones.visibility = View.GONE
                        rvNotificaciones.visibility = View.VISIBLE
                        rvNotificaciones.adapter = NotificacionAdapter(listaAvisos)
                    }
                }

            } catch (e: Exception) {
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "Error cargando notificaciones", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}