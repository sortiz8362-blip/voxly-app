package com.voxly.app.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.voxly.app.R
import com.voxly.app.data.model.MensajeData

class MensajeAdapter(
    private val mensajes: List<MensajeData>,
    private val miUserId: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // Identificadores para saber qué burbuja pintar
    private val TIPO_ENVIADO = 1
    private val TIPO_RECIBIDO = 2

    override fun getItemViewType(position: Int): Int {
        // Si el remitente soy yo, es un mensaje enviado. Si no, recibido.
        return if (mensajes[position].remitenteId == miUserId) TIPO_ENVIADO else TIPO_RECIBIDO
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TIPO_ENVIADO) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_mensaje_enviado, parent, false)
            EnviadoViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_mensaje_recibido, parent, false)
            RecibidoViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mensaje = mensajes[position]
        if (holder is EnviadoViewHolder) {
            holder.tvTexto.text = mensaje.texto
        } else if (holder is RecibidoViewHolder) {
            holder.tvTexto.text = mensaje.texto
        }
    }

    override fun getItemCount() = mensajes.size

    class EnviadoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTexto: TextView = view.findViewById(R.id.tvTextoEnviado)
    }
    class RecibidoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTexto: TextView = view.findViewById(R.id.tvTextoRecibido)
    }
}