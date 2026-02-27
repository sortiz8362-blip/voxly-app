package com.voxly.app

import android.app.Application
import com.voxly.app.data.remote.AppwriteConfig

class VoxlyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inicializamos Appwrite una sola vez para toda la vida de la app
        AppwriteConfig.inicializar(this)
    }
}