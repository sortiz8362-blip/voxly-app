package com.voxly.app.ui.fragments

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.voxly.app.R
import com.voxly.app.data.model.NotificacionData

class NotificacionAdapter(private val notificaciones: List<NotificacionData>) :
    RecyclerView.Adapter<NotificacionAdapter.NotificacionViewHolder>() {

    class NotificacionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcono: ImageView = view.findViewById(R.id.ivIconoNotificacion)
        val tvTitulo: TextView = view.findViewById(R.id.tvTituloNotificacion)
        val tvTextoExtra: TextView = view.findViewById(R.id.tvTextoExtra)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificacionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notificacion, parent, false)
        return NotificacionViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificacionViewHolder, position: Int) {
        val notificacion = notificaciones[position]

        // Diferenciar si es Like o Retweet
        if (notificacion.tipo == "like") {
            holder.ivIcono.setImageResource(android.R.drawable.ic_menu_myplaces) // Simulando un corazón
            holder.ivIcono.setColorFilter(Color.parseColor("#E0245E")) // Rojo
            holder.tvTitulo.text = "A ${notificacion.nombreOrigen} le gustó tu post"
        } else if (notificacion.tipo == "rt") {
            holder.ivIcono.setImageResource(android.R.drawable.ic_menu_revert) // Simulando RT
            holder.ivIcono.setColorFilter(Color.parseColor("#17BF63")) // Verde
            holder.tvTitulo.text = "${notificacion.nombreOrigen} reposteó tu publicación"
        }

        holder.tvTextoExtra.text = "\"${notificacion.textoExtra}\""
    }

    override fun getItemCount() = notificaciones.size
}