package com.example.tecreciclaje

import android.app.Application
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import java.io.File

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Configuración global de Firebase con persistencia habilitada
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        // Configuración global de Picasso (Caché y Rendimiento)
        val builder = Picasso.Builder(this)
        builder.downloader(OkHttp3Downloader(this, Long.MAX_VALUE)) // Caché grande
        builder.indicatorsEnabled(false) // Desactiva los indicadores visuales de depuración
        builder.loggingEnabled(false) // Desactiva los logs en producción
        val built = builder.build()
        Picasso.setSingletonInstance(built)
    }
}
