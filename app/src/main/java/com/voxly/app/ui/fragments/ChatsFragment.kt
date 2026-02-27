package com.voxly.app.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.voxly.app.R
import com.voxly.app.data.remote.AppwriteConfig
import com.voxly.app.ui.chat.ChatActivity
import com.voxly.app.utils.Constantes
import io.appwrite.Query
import kotlinx.coroutines.launch

class ChatsFragment : Fragment() {

    private lateinit var rvChatsRecientes: RecyclerView
    private var miUserId: String = ""

    // Clase de datos temporal para la lista
    data class ChatReciente(val otroUsuarioId: String, val otroUsuarioNombre: String, val ultimoMensaje: String)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_notificaciones, container, false) // Reutilizamos el diseño base de lista vacía

        view.findViewById<TextView>(R.id.tvTituloNotificaciones).text = "Mensajes Directos"
        view.findViewById<TextView>(R.id.tvTituloNotificaciones).id = View.generateViewId() // Evitar conflicto de IDs

        rvChatsRecientes = view.findViewById(R.id.rvNotificaciones)
        rvChatsRecientes.layoutManager = LinearLayoutManager(requireContext())
        return view
    }

    override fun onResume() {
        super.onResume()
        cargarBandejaDeEntrada()
    }

    private fun cargarBandejaDeEntrada() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                miUserId = AppwriteConfig.account.get().id

                // Descargamos los últimos mensajes de toda la base de datos
                val respuesta = AppwriteConfig.databases.listDocuments(
                    databaseId = Constantes.DATABASE_ID,
                    collectionId = Constantes.COLECCION_MENSAJES,
                    queries = listOf(Query.orderDesc("fecha"), Query.limit(300))
                )

                // Filtramos solo los mensajes donde yo soy remitente o destinatario
                val misMensajes = respuesta.documents.map { it.data }.filter {
                    it["remitente_id"].toString() == miUserId || it["destinatario_id"].toString() == miUserId
                }

                // Agrupamos para obtener solo la última conversación con cada persona
                val chatsAgrupados = mutableMapOf<String, ChatReciente>()

                for (msg in misMensajes) {
                    val soyRemitente = msg["remitente_id"].toString() == miUserId
                    val elOtroId = if (soyRemitente) msg["destinatario_id"].toString() else msg["remitente_id"].toString()
                    val elOtroNombre = if (soyRemitente) "Usuario" else "Usuario" // Idealmente sacaríamos el nombre de la colección 'perfiles'

                    if (!chatsAgrupados.containsKey(elOtroId)) {
                        val preview = if (soyRemitente) "Tú: ${msg["texto"]}" else msg["texto"].toString()
                        chatsAgrupados[elOtroId] = ChatReciente(elOtroId, "Chat ID: ${elOtroId.take(5)}...", preview)
                    }
                }

                activity?.runOnUiThread {
                    rvChatsRecientes.visibility = View.VISIBLE
                    rvChatsRecientes.adapter = BandejaAdapter(chatsAgrupados.values.toList())
                }
            } catch (e: Exception) { }
        }
    }

    // Adaptador interno para la bandeja de entrada
    inner class BandejaAdapter(private val chats: List<ChatReciente>) : RecyclerView.Adapter<BandejaAdapter.ViewHolder>() {
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvNombre: TextView = view.findViewById(R.id.tvNombreChat)
            val tvUltimo: TextView = view.findViewById(R.id.tvUltimoMensaje)
            init {
                view.setOnClickListener {
                    val chat = chats[adapterPosition]
                    val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                        putExtra("DESTINATARIO_ID", chat.otroUsuarioId)
                        putExtra("DESTINATARIO_NOMBRE", "Usuario")
                    }
                    startActivity(intent)
                }
            }
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_chat_reciente, parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.tvNombre.text = chats[position].otroUsuarioNombre
            holder.tvUltimo.text = chats[position].ultimoMensaje
        }
        override fun getItemCount() = chats.size
    }
}