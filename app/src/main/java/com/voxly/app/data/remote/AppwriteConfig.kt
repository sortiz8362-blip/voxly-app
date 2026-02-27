package com.voxly.app.data.remote

import android.content.Context
import io.appwrite.Client
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Storage
import com.voxly.app.utils.Constantes

object AppwriteConfig {
    lateinit var client: Client
    lateinit var account: Account
    lateinit var databases: Databases
    lateinit var storage: Storage

    // Esta función la llamaremos apenas se abra la app
    fun inicializar(context: Context) {
        client = Client(context)
            .setEndpoint(Constantes.ENDPOINT)
            .setProject(Constantes.PROJECT_ID)

        account = Account(client)
        databases = Databases(client)
        storage = Storage(client)
    }
}