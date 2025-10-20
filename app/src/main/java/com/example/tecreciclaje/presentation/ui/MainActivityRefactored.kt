package com.example.tecreciclaje.presentation.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieAnimationView
import com.example.tecreciclaje.AdminPanel
import com.example.tecreciclaje.LoginActivity
import com.example.tecreciclaje.R
import com.example.tecreciclaje.UserPanelDynamic
import com.example.tecreciclaje.di.DependencyInjection
import com.example.tecreciclaje.domain.model.Usuario
import com.example.tecreciclaje.presentation.viewmodel.MainViewModel
import com.example.tecreciclaje.utils.FCMTokenManager

class MainActivityRefactored : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var loginAnimation: LottieAnimationView

    // Código de solicitud para permisos
    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar ViewModel usando inyección de dependencias
        viewModel = DependencyInjection.provideMainViewModel()

        // Inicializar animación de carga
        loginAnimation = findViewById(R.id.login_animation)
        loginAnimation.visibility = View.VISIBLE
        loginAnimation.playAnimation()

        // Configurar observadores
        setupObservers()

        // Verificar permisos de notificación primero
        checkNotificationPermission()
    }

    private fun setupObservers() {
        // Observar cambios en el usuario actual
        viewModel.currentUser.observe(this) { usuario ->
            usuario?.let { handleUserData(it) }
        }

        // Observar estado de carga
        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                loginAnimation.visibility = View.VISIBLE
                loginAnimation.playAnimation()
            } else {
                loginAnimation.visibility = View.GONE
                loginAnimation.pauseAnimation()
            }
        }

        // Observar mensajes de error
        viewModel.errorMessage.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        // Observar rol del usuario
        viewModel.userRole.observe(this) { role ->
            role?.let { redirectBasedOnRole(it) }
        }
    }

    private fun checkNotificationPermission() {
        // Para Android 13 (API 33) y superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {

                // Solicitar permiso
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            } else {
                // Permiso ya concedido, continuar con la verificación del usuario
                proceedWithUserVerification()
            }
        } else {
            // Para versiones anteriores a Android 13, no se necesita solicitar permiso
            proceedWithUserVerification()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permisos de notificación concedidos", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Las notificaciones fueron deshabilitadas. Puedes habilitarlas en configuración.",
                    Toast.LENGTH_LONG).show()
            }
            // Continuar con la verificación del usuario independientemente del resultado
            proceedWithUserVerification()
        }
    }

    private fun proceedWithUserVerification() {
        // Verificar usuario autenticado usando el ViewModel
        if (viewModel.isUserLoggedIn()) {
            // Actualizar token FCM del usuario
            FCMTokenManager.updateTokenForCurrentUser()
            // Cargar datos del usuario
            viewModel.checkUserAuthentication()
        } else {
            redirectToLogin()
        }
    }

    private fun handleUserData(usuario: Usuario) {
        // Aquí puedes manejar los datos del usuario si es necesario
        // Por ejemplo, actualizar la UI con información del usuario
        Toast.makeText(this, "Bienvenido, ${usuario.nombreCompleto}", Toast.LENGTH_SHORT).show()
    }

    private fun redirectBasedOnRole(role: String) {
        val intent = if (role == "admin") {
            Intent(this, AdminPanel::class.java)
        } else {
            Intent(this, UserPanelDynamic::class.java)
        }
        startActivity(intent)
        finish()
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
