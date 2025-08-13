package com.example.tecreciclaje;

import android.app.Application;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // ✅ Configuración global de Firebase con persistencia habilitada
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        // ✅ Configuración global de Picasso (Caché y Rendimiento)
        Picasso.Builder builder = new Picasso.Builder(this);
        builder.downloader(new OkHttp3Downloader(this, Integer.MAX_VALUE)); // Caché grande
        builder.indicatorsEnabled(false); // Desactiva los indicadores visuales de depuración
        builder.loggingEnabled(false); // Desactiva los logs en producción
        Picasso built = builder.build();
        Picasso.setSingletonInstance(built);
    }
}
