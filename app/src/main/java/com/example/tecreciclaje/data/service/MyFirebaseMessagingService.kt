package com.example.tecreciclaje.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.tecreciclaje.MainActivity
import com.example.tecreciclaje.R
import com.example.tecreciclaje.utils.FCMTokenManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
    
    companion object {
        private const val TAG = "MyFirebaseMsgService"
        private const val CHANNEL_ID = "TecReciclaje_Channel"
        private const val CHANNEL_NAME = "TecReciclaje Notifications"
        private const val CHANNEL_DESCRIPTION = "Canal de notificaciones para TecReciclaje"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Verificar si el mensaje contiene datos
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
        }

        // Verificar si el mensaje contiene notificación
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Message Notification Body: ${notification.body}")
            
            // Enviar notificación local
            sendNotification(
                notification.title,
                notification.body,
                remoteMessage.data
            )
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        
        // Verificar si hay un usuario autenticado antes de actualizar el token
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            FCMTokenManager.updateTokenInDatabase(currentUser.uid, token)
        } else {
            Log.w(TAG, "No hay usuario autenticado para actualizar token FCM")
            // Guardar el token temporalmente para usarlo cuando el usuario se autentique
            saveTokenForLaterUse(token)
        }
    }

    override fun onDeletedMessages() {
        Log.d(TAG, "Messages deleted from server")
        // Regenerar token cuando los mensajes se borran del servidor
        // Solo si hay un usuario autenticado
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            FCMTokenManager.updateTokenForCurrentUser()
        } else {
            Log.w(TAG, "No hay usuario autenticado para regenerar token")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                lightColor = Color.GREEN
                enableVibration(true)
                vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(title: String?, messageBody: String?, data: Map<String, String>) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            
            // Agregar datos extras si existen
            if (data.isNotEmpty()) {
                for ((key, value) in data) {
                    putExtra(key, value)
                }
            }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // ICONO PERSONALIZADO
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(getColor(R.color.notification_color)) // Color verde para TecReciclaje
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(0, notificationBuilder.build())
    }

    /**
     * Guarda el token FCM temporalmente para usarlo cuando el usuario se autentique
     */
    private fun saveTokenForLaterUse(token: String) {
        // Guardar en SharedPreferences para uso posterior
        getSharedPreferences("FCM_PREFS", MODE_PRIVATE)
            .edit()
            .putString("pending_token", token)
            .apply()
        Log.d(TAG, "Token FCM guardado temporalmente para uso posterior")
    }
}
