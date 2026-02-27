package com.voxly.app.data.model

data class TweetData(
    val id: String,
    val usuarioId: String,
    val nombre: String,      // ¡Estaban perdidos!
    val username: String,    // ¡Estaban perdidos!
    val texto: String,
    val fecha: String,
    var likesUsuarios: List<String> = emptyList(),
    var retweetsUsuarios: List<String> = emptyList(),
    var cantidadComentarios: Int = 0,
    val mediaUrl: String? = null
)