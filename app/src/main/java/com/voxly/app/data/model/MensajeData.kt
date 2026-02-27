package com.voxly.app.data.model

data class MensajeData(
    val id: String,
    val remitenteId: String,
    val destinatarioId: String,
    val texto: String,
    val fecha: String
)