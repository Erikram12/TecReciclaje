package com.example.tecreciclaje.utils

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.example.tecreciclaje.R

object CustomAlertDialog {

    // Color azul principal
    private const val COLOR_AZUL = "#1B396A"
    private const val COLOR_ROJO = "#D32F2F"
    private const val COLOR_VERDE = "#4c8479"

    /**
     * Crea un AlertDialog personalizado con el tema de la aplicación
     */
    private fun createStyledDialog(
        context: Context,
        title: String,
        message: String,
        positiveText: String?,
        negativeText: String?,
        positiveColor: String = COLOR_AZUL,
        negativeColor: String = COLOR_AZUL,
        onPositiveClick: (() -> Unit)?,
        onNegativeClick: (() -> Unit)?
    ): AlertDialog {

        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setMessage(message)

        if (positiveText != null) {
            builder.setPositiveButton(positiveText) { _, _ ->
                onPositiveClick?.invoke()
            }
        }

        if (negativeText != null) {
            builder.setNegativeButton(negativeText) { dialog, _ ->
                onNegativeClick?.invoke()
                dialog.dismiss()
            }
        }

        val dialog = builder.create()

        // Aplicar estilos cuando se muestre el diálogo
        dialog.setOnShowListener {
            // Estilizar título
            val titleId = context.resources.getIdentifier("alertTitle", "id", "android")
            if (titleId > 0) {
                val titleView = dialog.findViewById<TextView>(titleId)
                titleView?.apply {
                    setTextColor(Color.BLACK)
                    textSize = 20f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
            }

            // Estilizar mensaje
            val messageView = dialog.findViewById<TextView>(android.R.id.message)
            messageView?.apply {
                setTextColor(Color.parseColor("#616161")) // dark_gray
                textSize = 16f
            }

            // Estilizar botón positivo
            if (positiveText != null) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                    setTextColor(Color.parseColor(positiveColor))
                    textSize = 16f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
            }

            // Estilizar botón negativo
            if (negativeText != null) {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
                    setTextColor(Color.parseColor(negativeColor))
                    textSize = 16f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
            }
        }

        return dialog
    }

    /**
     * Crea un AlertDialog personalizado con el tema de la aplicación (retorna Builder)
     */
    fun createCustomAlertDialog(context: Context): AlertDialog.Builder {
        return AlertDialog.Builder(context)
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

        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton(positiveText) { _, _ ->
            onPositiveClick?.invoke()
        }
        builder.setNegativeButton(negativeText) { dialog, _ ->
            onNegativeClick?.invoke()
            dialog.dismiss()
        }

        // Crear el diálogo para aplicar estilos
        val dialog = builder.create()
        dialog.setOnShowListener {
            // Estilizar título
            val titleId = context.resources.getIdentifier("alertTitle", "id", "android")
            if (titleId > 0) {
                val titleView = dialog.findViewById<TextView>(titleId)
                titleView?.apply {
                    setTextColor(Color.BLACK)
                    textSize = 20f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
            }

            // Estilizar mensaje
            val messageView = dialog.findViewById<TextView>(android.R.id.message)
            messageView?.apply {
                setTextColor(Color.parseColor("#616161"))
                textSize = 16f
            }

            // Estilizar botones
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                setTextColor(Color.parseColor(COLOR_AZUL))
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
                setTextColor(Color.parseColor(COLOR_AZUL))
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
        }

        return builder
    }

    /**
     * Crea un AlertDialog de información personalizado
     */
    fun createInfoDialog(
        context: Context,
        title: String,
        message: String,
        buttonText: String
    ): AlertDialog.Builder {

        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton(buttonText) { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()
        dialog.setOnShowListener {
            // Estilizar título
            val titleId = context.resources.getIdentifier("alertTitle", "id", "android")
            if (titleId > 0) {
                val titleView = dialog.findViewById<TextView>(titleId)
                titleView?.apply {
                    setTextColor(Color.BLACK)
                    textSize = 20f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
            }

            // Estilizar mensaje
            val messageView = dialog.findViewById<TextView>(android.R.id.message)
            messageView?.apply {
                setTextColor(Color.parseColor("#616161"))
                textSize = 16f
            }

            // Estilizar botón
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                setTextColor(Color.parseColor(COLOR_AZUL))
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
        }

        return builder
    }

    /**
     * Crea un AlertDialog de error personalizado
     */
    fun createErrorDialog(
        context: Context,
        title: String?,
        message: String
    ): AlertDialog.Builder {

        val builder = AlertDialog.Builder(context)
        builder.setTitle(title ?: "Error")
        builder.setMessage(message)
        builder.setPositiveButton("Aceptar") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()
        dialog.setOnShowListener {
            // Estilizar título en rojo
            val titleId = context.resources.getIdentifier("alertTitle", "id", "android")
            if (titleId > 0) {
                val titleView = dialog.findViewById<TextView>(titleId)
                titleView?.apply {
                    setTextColor(Color.parseColor(COLOR_ROJO))
                    textSize = 20f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
            }

            // Estilizar mensaje
            val messageView = dialog.findViewById<TextView>(android.R.id.message)
            messageView?.apply {
                setTextColor(Color.parseColor("#616161"))
                textSize = 16f
            }

            // Estilizar botón en rojo
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                setTextColor(Color.parseColor(COLOR_ROJO))
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
        }

        return builder
    }

    /**
     * Crea un AlertDialog de éxito personalizado
     */
    fun createSuccessDialog(
        context: Context,
        title: String?,
        message: String
    ): AlertDialog.Builder {

        val builder = AlertDialog.Builder(context)
        builder.setTitle(title ?: "Éxito")
        builder.setMessage(message)
        builder.setPositiveButton("Aceptar") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()
        dialog.setOnShowListener {
            // Estilizar título en verde
            val titleId = context.resources.getIdentifier("alertTitle", "id", "android")
            if (titleId > 0) {
                val titleView = dialog.findViewById<TextView>(titleId)
                titleView?.apply {
                    setTextColor(Color.parseColor(COLOR_VERDE))
                    textSize = 20f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
            }

            // Estilizar mensaje
            val messageView = dialog.findViewById<TextView>(android.R.id.message)
            messageView?.apply {
                setTextColor(Color.parseColor("#616161"))
                textSize = 16f
            }

            // Estilizar botón en verde
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                setTextColor(Color.parseColor(COLOR_VERDE))
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
        }

        return builder
    }

    /**
     * Crea un AlertDialog de cierre de sesión personalizado con animación de carga
     */
    fun createLogoutDialogWithAnimation(
        context: Context,
        onConfirmLogout: (() -> Unit)?
    ): AlertDialog {
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
            }, 1500)
        }

        return dialog
    }

    /**
     * Crea un AlertDialog de cierre de sesión personalizado
     */
    fun createLogoutDialog(
        context: Context,
        onConfirmLogout: (() -> Unit)?
    ): AlertDialog.Builder {
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
    fun createDeleteConfirmationDialog(
        context: Context,
        itemName: String,
        onConfirmDelete: (() -> Unit)?
    ): AlertDialog.Builder {

        val builder = AlertDialog.Builder(context)
        builder.setTitle("Confirmar eliminación")
        builder.setMessage("¿Estás seguro de que deseas eliminar $itemName?\n\nEsta acción no se puede deshacer.")
        builder.setPositiveButton("Eliminar") { _, _ ->
            onConfirmDelete?.invoke()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.setOnShowListener {
            // Estilizar título
            val titleId = context.resources.getIdentifier("alertTitle", "id", "android")
            if (titleId > 0) {
                val titleView = dialog.findViewById<TextView>(titleId)
                titleView?.apply {
                    setTextColor(Color.BLACK)
                    textSize = 20f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
            }

            // Estilizar mensaje
            val messageView = dialog.findViewById<TextView>(android.R.id.message)
            messageView?.apply {
                setTextColor(Color.parseColor("#616161"))
                textSize = 16f
            }

            // Estilizar botones
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                setTextColor(Color.parseColor(COLOR_ROJO)) // Rojo para eliminar
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
                setTextColor(Color.parseColor(COLOR_AZUL)) // Azul para cancelar
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
        }

        return builder
    }
    /**
     * Crea un AlertDialog de eliminación de producto personalizado con animación de carga
     */
    fun createDeleteProductDialog(
        context: Context,
        productName: String,
        onConfirmDelete: (() -> Unit)?
    ): AlertDialog {
        // Crear el diálogo personalizado
        val builder = AlertDialog.Builder(context, R.style.LogoutDialogStyle)
        val dialog = builder.create()

        // Inflar el layout personalizado
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_delete_producto, null)
        dialog.setView(dialogView)

        // Referencias a las vistas
        val tvMensaje = dialogView.findViewById<TextView>(R.id.tvMensajeEliminar)
        val btnCancelar = dialogView.findViewById<Button>(R.id.btnCancelar)
        val btnEliminar = dialogView.findViewById<Button>(R.id.btnEliminar)
        val loadingContainer = dialogView.findViewById<View>(R.id.loadingContainer)
        val buttonsContainer = dialogView.findViewById<View>(R.id.buttonsContainer)

        // Personalizar el mensaje con el nombre del producto
        tvMensaje.text = "¿Estás seguro de que deseas eliminar '$productName'?\n\nEsta acción no se puede deshacer."

        // Configurar botón cancelar
        btnCancelar.setOnClickListener { dialog.dismiss() }

        // Configurar botón eliminar
        btnEliminar.setOnClickListener {
            // Mostrar animación de carga
            buttonsContainer.visibility = View.GONE
            loadingContainer.visibility = View.VISIBLE

            // Ejecutar la acción de eliminación después de un pequeño delay
            Handler(Looper.getMainLooper()).postDelayed({
                onConfirmDelete?.invoke()
                dialog.dismiss()
            }, 1500)
        }

        return dialog
    }

    /**
     * Crea un AlertDialog para confirmar el vaciado de contenedor con animación de carga
     */
    fun createVaciarContenedorDialog(
        context: Context,
        nombreContenedor: String,
        onConfirmVaciar: (() -> Unit)?
    ): AlertDialog {
        // Crear el diálogo personalizado
        val builder = AlertDialog.Builder(context, R.style.LogoutDialogStyle)
        val dialog = builder.create()

        // Inflar el layout personalizado
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_vaciar_contedor, null)
        dialog.setView(dialogView)

        // Referencias a las vistas
        val tvTitulo = dialogView.findViewById<TextView>(R.id.tvTituloVaciar)
        val tvMensaje = dialogView.findViewById<TextView>(R.id.tvMensajeVaciar)
        val btnCancelar = dialogView.findViewById<Button>(R.id.btnCancelar)
        val btnVaciar = dialogView.findViewById<Button>(R.id.btnVaciar)
        val loadingContainer = dialogView.findViewById<View>(R.id.loadingContainer)
        val buttonsContainer = dialogView.findViewById<View>(R.id.buttonsContainer)

        // Personalizar el título y mensaje con el nombre del contenedor
        tvTitulo.text = "Vaciar Contenedor - $nombreContenedor"
        tvMensaje.text = "¿Confirmas que el contenedor de $nombreContenedor ha sido vaciado?\n\nEsto reiniciará los contadores automáticamente."

        // Configurar botón cancelar
        btnCancelar.setOnClickListener { dialog.dismiss() }

        // Configurar botón vaciar
        btnVaciar.setOnClickListener {
            // Mostrar animación de carga
            buttonsContainer.visibility = View.GONE
            loadingContainer.visibility = View.VISIBLE

            // Ejecutar la acción de vaciado después de un pequeño delay
            Handler(Looper.getMainLooper()).postDelayed({
                onConfirmVaciar?.invoke()
                dialog.dismiss()
            }, 1500)
        }

        return dialog
    }

    /**
     * Crea un AlertDialog para confirmar el canje de producto con animación de carga
     */
    fun createCanjeProductoDialog(
        context: Context,
        nombreProducto: String,
        puntosNecesarios: Int,
        onConfirmCanje: (() -> Unit)?
    ): AlertDialog {
        // Crear el diálogo personalizado
        val builder = AlertDialog.Builder(context, R.style.LogoutDialogStyle)
        val dialog = builder.create()

        // Inflar el layout personalizado
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_canje_producto, null)
        dialog.setView(dialogView)

        // Referencias a las vistas
        val tvMensaje = dialogView.findViewById<TextView>(R.id.tvMensajeCanje)
        val btnCancelar = dialogView.findViewById<Button>(R.id.btnCancelar)
        val btnCanjear = dialogView.findViewById<Button>(R.id.btnCanjear)
        val loadingContainer = dialogView.findViewById<View>(R.id.loadingContainer)
        val buttonsContainer = dialogView.findViewById<View>(R.id.buttonsContainer)

        // Personalizar el mensaje con el nombre del producto y puntos
        tvMensaje.text = "¿Estás seguro de que deseas canjear $puntosNecesarios puntos por '$nombreProducto'?\n\nSe generará un vale válido por 3 días."

        // Configurar botón cancelar
        btnCancelar.setOnClickListener { dialog.dismiss() }

        // Configurar botón canjear
        btnCanjear.setOnClickListener {
            // Mostrar animación de carga
            buttonsContainer.visibility = View.GONE
            loadingContainer.visibility = View.VISIBLE

            // Ejecutar la acción de canje después de un pequeño delay
            Handler(Looper.getMainLooper()).postDelayed({
                onConfirmCanje?.invoke()
                dialog.dismiss()
            }, 1500)
        }

        return dialog
    }

    /**
     * Crea un AlertDialog de canje exitoso con opciones de navegación
     */
    fun createCanjeExitosoDialog(
        context: Context,
        onVerVale: (() -> Unit)?,
        onCerrar: (() -> Unit)?
    ): AlertDialog {
        // Crear el diálogo personalizado
        val builder = AlertDialog.Builder(context, R.style.LogoutDialogStyle)
        val dialog = builder.create()

        // Inflar el layout personalizado
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_canje_exitoso, null)
        dialog.setView(dialogView)

        // Referencias a las vistas
        val btnCerrar = dialogView.findViewById<Button>(R.id.btnCerrar)
        val btnVerVale = dialogView.findViewById<Button>(R.id.btnVerVale)

        // Configurar botón cerrar
        btnCerrar.setOnClickListener {
            onCerrar?.invoke()
            dialog.dismiss()
        }

        // Configurar botón ver vale
        btnVerVale.setOnClickListener {
            onVerVale?.invoke()
            dialog.dismiss()
        }

        // Hacer que no se pueda cerrar tocando fuera
        dialog.setCancelable(false)

        return dialog
    }

    /**
     * Crea un AlertDialog para mostrar información del producto a entregar cuando se escanea un QR
     */
    fun createInfoProductoEntregaDialog(
        context: Context,
        nombreProducto: String,
        onOKClick: (() -> Unit)?
    ): AlertDialog {
        // Crear el diálogo personalizado
        val builder = AlertDialog.Builder(context, R.style.LogoutDialogStyle)
        val dialog = builder.create()

        // Inflar el layout personalizado
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_info_producto_entrega, null)
        dialog.setView(dialogView)

        // Referencias a las vistas
        val tvNombreProducto = dialogView.findViewById<TextView>(R.id.tvNombreProducto)
        val btnOK = dialogView.findViewById<Button>(R.id.btnOK)

        // Establecer el nombre del producto
        tvNombreProducto.text = nombreProducto

        // Configurar botón OK
        btnOK.setOnClickListener {
            onOKClick?.invoke()
            dialog.dismiss()
        }

        // Hacer que no se pueda cerrar tocando fuera
        dialog.setCancelable(false)

        return dialog
    }

    /**
     * Crea un AlertDialog de puntos insuficientes
     */
    fun createPuntosInsuficientesDialog(
        context: Context,
        puntosActuales: Int,
        puntosNecesarios: Int,
        nombreProducto: String
    ): AlertDialog {
        // Crear el diálogo personalizado
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Puntos Insuficientes")

        val puntosFaltantes = puntosNecesarios - puntosActuales
        builder.setMessage("Necesitas $puntosNecesarios puntos para canjear '$nombreProducto'.\n\nTienes: $puntosActuales puntos\nTe faltan: $puntosFaltantes puntos\n\n¡Sigue reciclando para obtener más puntos!")

        builder.setPositiveButton("Entendido") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()
        dialog.setOnShowListener {
            // Estilizar título en naranja
            val titleId = context.resources.getIdentifier("alertTitle", "id", "android")
            if (titleId > 0) {
                val titleView = dialog.findViewById<TextView>(titleId)
                titleView?.apply {
                    setTextColor(Color.parseColor("#FF9800")) // Naranja
                    textSize = 20f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
            }

            // Estilizar mensaje
            val messageView = dialog.findViewById<TextView>(android.R.id.message)
            messageView?.apply {
                setTextColor(Color.parseColor("#616161"))
                textSize = 16f
            }

            // Estilizar botón en naranja
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                setTextColor(Color.parseColor("#FF9800"))
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
        }

        return dialog
    }
}