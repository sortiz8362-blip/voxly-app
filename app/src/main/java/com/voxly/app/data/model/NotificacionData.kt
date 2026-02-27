package com.voxly.app.data.model

data class NotificacionData(
    val id: String,
    val usuarioDestinoId: String,
    val nombreOrigen: String,
    val tipo: String, // Guardará "like" o "rt"
    val textoExtra: String,
    val fecha: String
)