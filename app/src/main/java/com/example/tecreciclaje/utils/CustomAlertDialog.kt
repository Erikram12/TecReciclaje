package com.example.tecreciclaje.utils

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import com.example.tecreciclaje.R

object CustomAlertDialog {

    /**
     * Crea un AlertDialog personalizado con el tema de la aplicación
     */
    fun createCustomAlertDialog(context: Context): AlertDialog.Builder {
        return AlertDialog.Builder(context, R.style.CustomAlertDialog)
    }

    /**
     * Crea un AlertDialog de confirmación personalizado
     */
    fun createConfirmationDialog(
        context: Context,
        title: String,
        message: String,
        positiveText: String,
        negativeText: String,
        onPositiveClick: (() -> Unit)?,
        onNegativeClick: (() -> Unit)?
    ): AlertDialog.Builder {
        val builder = createCustomAlertDialog(context)

        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveText) { _, _ ->
                onPositiveClick?.invoke()
            }
            .setNegativeButton(negativeText) { dialog, _ ->
                onNegativeClick?.invoke()
                dialog.dismiss()
            }

        return builder
    }

    /**
     * Crea un AlertDialog de información personalizado
     */
    fun createInfoDialog(context: Context, title: String, message: String, buttonText: String): AlertDialog.Builder {
        val builder = createCustomAlertDialog(context)

        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton(buttonText) { dialog, _ -> dialog.dismiss() }

        return builder
    }

    /**
     * Crea un AlertDialog de error personalizado
     */
    fun createErrorDialog(context: Context, title: String?, message: String): AlertDialog.Builder {
        val builder = createCustomAlertDialog(context)

        builder.setTitle(title ?: "Error")
            .setMessage(message)
            .setPositiveButton("Aceptar") { dialog, _ -> dialog.dismiss() }

        return builder
    }

    /**
     * Crea un AlertDialog de éxito personalizado
     */
    fun createSuccessDialog(context: Context, title: String?, message: String): AlertDialog.Builder {
        val builder = createCustomAlertDialog(context)

        builder.setTitle(title ?: "Éxito")
            .setMessage(message)
            .setPositiveButton("Aceptar") { dialog, _ -> dialog.dismiss() }

        return builder
    }

    /**
     * Crea un AlertDialog de cierre de sesión personalizado con animación de carga
     */
    fun createLogoutDialogWithAnimation(context: Context, onConfirmLogout: (() -> Unit)?): AlertDialog {
        // Crear el diálogo personalizado con estilo formal
        val builder = AlertDialog.Builder(context, R.style.LogoutDialogStyle)
        val dialog = builder.create()

        // Inflar el layout personalizado
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_logout_custom, null)
        dialog.setView(dialogView)

        // Referencias a las vistas
        val btnCancelar = dialogView.findViewById<Button>(R.id.btnCancelar)
        val btnCerrarSesion = dialogView.findViewById<Button>(R.id.btnCerrarSesion)
        val loadingContainer = dialogView.findViewById<View>(R.id.loadingContainer)
        val buttonsContainer = dialogView.findViewById<View>(R.id.buttonsContainer)

        // Configurar botón cancelar
        btnCancelar.setOnClickListener { dialog.dismiss() }

        // Configurar botón cerrar sesión
        btnCerrarSesion.setOnClickListener {
            // Mostrar animación de carga
            buttonsContainer.visibility = View.GONE
            loadingContainer.visibility = View.VISIBLE

            // Ejecutar la acción de cierre de sesión después de un pequeño delay
            Handler(Looper.getMainLooper()).postDelayed({
                onConfirmLogout?.invoke()
                dialog.dismiss()
            }, 1500) // 1.5 segundos de delay para mostrar la animación
        }

        return dialog
    }

    /**
     * Crea un AlertDialog de cierre de sesión personalizado (método anterior mantenido por compatibilidad)
     */
    fun createLogoutDialog(context: Context, onConfirmLogout: (() -> Unit)?): AlertDialog.Builder {
        return createConfirmationDialog(
            context,
            "Cerrar sesión",
            "¿Estás seguro de que deseas cerrar sesión?\n\nSe limpiarán todos los datos locales y la caché de Google.",
            "Sí, cerrar sesión",
            "Cancelar",
            onConfirmLogout,
            null
        )
    }

    /**
     * Crea un AlertDialog de confirmación de eliminación personalizado
     */
    fun createDeleteConfirmationDialog(context: Context, itemName: String, onConfirmDelete: (() -> Unit)?): AlertDialog.Builder {
        return createConfirmationDialog(
            context,
            "Confirmar eliminación",
            "¿Estás seguro de que deseas eliminar $itemName?\n\nEsta acción no se puede deshacer.",
            "Eliminar",
            "Cancelar",
            onConfirmDelete,
            null
        )
    }
}
