package com.example.tecreciclaje.userpanel

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.tecreciclaje.LoginActivity
import com.example.tecreciclaje.R
import com.example.tecreciclaje.UserPanelDynamic
import com.example.tecreciclaje.domain.model.Producto
import com.example.tecreciclaje.utils.TutorialManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*

class ProductoDetalleActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var textViewProgreso: TextView
    private lateinit var textPrecioPuntos: TextView
    private lateinit var textProgresoDetallado: TextView
    private lateinit var btnCanjear: Button
    private lateinit var btnBack: ImageButton
    private lateinit var btnQuestion: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var ivImagenProducto: ImageView
    private lateinit var tvNombreProducto: TextView
    private lateinit var tvDescripcionProducto: TextView

    private var producto: Producto? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_producto_detalle)

        auth = FirebaseAuth.getInstance()
        initializeViews()

        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Obtener producto del intent
        producto = intent.getSerializableExtra("producto") as? Producto
        if (producto == null) {
            Toast.makeText(this, "Error: Producto no encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupProductInfo()
        getUserData(currentUser.uid)
        setupNavigation()
        setupBackButton()

        // MOSTRAR TUTORIAL SI ES NECESARIO
        producto?.let { prod ->
            if (prod.imagenUrl.isNotEmpty()) {
                TutorialManager.showProductoTutorialIfNeededConUrl(this, prod.nombre, prod.imagenUrl)
            } else {
                TutorialManager.showProductoTutorialIfNeeded(this, prod.nombre, R.drawable.placeholder_producto)
            }
        }
    }

    private fun initializeViews() {
        textViewProgreso = findViewById(R.id.textViewProgreso)
        textPrecioPuntos = findViewById(R.id.textPrecioPuntos)
        textProgresoDetallado = findViewById(R.id.textProgresoDetallado)
        btnCanjear = findViewById(R.id.btnCanjear)
        btnBack = findViewById(R.id.btnBack)
        btnQuestion = findViewById(R.id.btnQuestion)
        progressBar = findViewById(R.id.progressBar)
        ivImagenProducto = findViewById(R.id.ivImagenProducto)
        tvNombreProducto = findViewById(R.id.tvNombreProducto)
        tvDescripcionProducto = findViewById(R.id.tvDescripcionProducto)

        // Configurar botón de ayuda
        setupHelpButton()
    }

    private fun setupProductInfo() {
        producto?.let { prod ->
            tvNombreProducto.text = prod.nombre
            tvDescripcionProducto.text = prod.descripcion
            textPrecioPuntos.text = "${prod.precioPuntos} pts"

            // Cargar imagen
            if (prod.imagenUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(prod.imagenUrl)
                    .placeholder(R.drawable.placeholder_producto)
                    .error(R.drawable.placeholder_producto)
                    .into(ivImagenProducto)
            } else {
                ivImagenProducto.setImageResource(R.drawable.placeholder_producto)
            }
        }
    }

    private fun setupNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.nav_home
        bottomNavigationView.setOnItemSelectedListener { item ->
            val itemId = item.itemId
            when (itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, UserPanelDynamic::class.java))
                    true
                }
                R.id.nav_docs -> {
                    startActivity(Intent(this, MisValesActivity::class.java))
                    true
                }
                R.id.nav_histori -> {
                    startActivity(Intent(this, HistorialActivity::class.java))
                    true
                }
                R.id.nav_perfil -> {
                    startActivity(Intent(this, PerfilActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun setupBackButton() {
        btnBack.setOnClickListener {
            finish()
        }
    }

    /**
     * Configura el botón de ayuda para mostrar el tutorial
     */
    private fun setupHelpButton() {
        btnQuestion.setOnClickListener {
            // Mostrar tutorial manualmente con la imagen actual del producto
            producto?.let { prod ->
                if (prod.imagenUrl.isNotEmpty()) {
                    // Si tiene URL, usar el método con URL
                    TutorialManager.showProductoTutorialConUrl(this, prod.nombre, prod.imagenUrl)
                } else {
                    // Si no tiene URL, usar el método con recurso
                    TutorialManager.showProductoTutorial(this, prod.nombre, R.drawable.placeholder_producto)
                }
            }
        }
    }

    private fun getUserData(uid: String) {
        val userRef = FirebaseDatabase.getInstance().getReference("usuarios").child(uid)
        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // MEJORADO: Manejo seguro de puntos con logs
                    val puntosObj = snapshot.child("usuario_puntos").value
                    var puntos = 0

                    Log.d("ProductoDetalleActivity", "Datos de puntos recibidos: $puntosObj (tipo: ${puntosObj?.javaClass?.simpleName ?: "null"})")

                    puntos = when (puntosObj) {
                        is Int -> {
                            Log.d("ProductoDetalleActivity", "Puntos como Int: $puntosObj")
                            puntosObj
                        }
                        is String -> {
                            try {
                                val parsed = puntosObj.toInt()
                                Log.d("ProductoDetalleActivity", "Puntos como String convertido: $parsed")
                                parsed
                            } catch (e: NumberFormatException) {
                                Log.w("ProductoDetalleActivity", "Error parsing puntos: $puntosObj", e)
                                0
                            }
                        }
                        is Long -> {
                            val converted = puntosObj.toInt()
                            Log.d("ProductoDetalleActivity", "Puntos como Long convertido: $converted")
                            converted
                        }
                        null -> {
                            Log.w("ProductoDetalleActivity", "No se encontraron puntos para el usuario")
                            0
                        }
                        else -> {
                            Log.w("ProductoDetalleActivity", "Tipo de puntos no reconocido: ${puntosObj.javaClass.simpleName}")
                            0
                        }
                    }

                    val puntosNecesarios = producto?.precioPuntos ?: 0

                    // Actualizar puntos disponibles
                    textViewProgreso.text = "$puntos pts"

                    // Actualizar progreso detallado
                    textProgresoDetallado.text = "$puntos/$puntosNecesarios"

                    // Actualizar barra de progreso
                    val progreso = if (puntosNecesarios > 0) minOf((puntos * 100) / puntosNecesarios, 100) else 0
                    progressBar.progress = progreso

                    // Habilitar/deshabilitar botón de canje
                    btnCanjear.isEnabled = puntos >= puntosNecesarios

                    // Configurar el botón de canjear
                    setupCanjearButton(puntos, uid)

                    Log.d("ProductoDetalleActivity", "UI actualizada - Puntos mostrados: $puntos, Progreso: $progreso%")
                } else {
                    Log.w("ProductoDetalleActivity", "No se encontraron datos para el usuario: $uid")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ProductoDetalleActivity", "Error al obtener datos del usuario", error.toException())
            }
        })
    }

    private fun setupCanjearButton(puntos: Int, uid: String) {
        btnCanjear.setOnClickListener {
            val puntosNecesarios = producto?.precioPuntos ?: 0
            val nombreProducto = producto?.nombre ?: "producto"

            if (puntos >= puntosNecesarios) {
                AlertDialog.Builder(this)
                    .setTitle("Confirmar Canje")
                    .setMessage("¿Estás seguro de que deseas canjear $puntosNecesarios puntos por $nombreProducto?")
                    .setPositiveButton("Sí") { _, _ ->
                        realizarCanje(uid, puntos)
                    }
                    .setNegativeButton("No", null)
                    .show()
            } else {
                AlertDialog.Builder(this)
                    .setTitle("Puntos insuficientes")
                    .setMessage("Necesitas al menos $puntosNecesarios puntos para canjear este producto.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun realizarCanje(uid: String, puntosActuales: Int) {
        val userRef = FirebaseDatabase.getInstance().getReference("usuarios").child(uid)
        val historialRef = userRef.child("historial")
        val valesRef = FirebaseDatabase.getInstance().getReference("vales")

        val puntosNecesarios = producto?.precioPuntos ?: 0
        val nombreProducto = producto?.nombre ?: "Producto"
        val imagenUrl = producto?.imagenUrl ?: ""

        val nuevosPuntos = puntosActuales - puntosNecesarios
        val fechaActual = System.currentTimeMillis()
        val fechaExpira = fechaActual + (3 * 24 * 60 * 60 * 1000) // 3 días en milisegundos

        userRef.child("usuario_puntos").setValue(nuevosPuntos)

        // Crear vale
        val valeId = valesRef.push().key
        if (valeId != null) {
            val nuevoValeRef = valesRef.child(valeId)
            nuevoValeRef.child("vale_usuario_id").setValue(uid)
            nuevoValeRef.child("vale_producto").setValue(nombreProducto)
            nuevoValeRef.child("vale_estado").setValue("Válido")
            nuevoValeRef.child("vale_imagen_url").setValue(imagenUrl)
            nuevoValeRef.child("vale_fecha_creacion").setValue(fechaActual)
            nuevoValeRef.child("vale_fecha_expiracion").setValue(fechaExpira)
        }

        // Registrar en historial
        val historialId = historialRef.push().key
        if (historialId != null) {
            historialRef.child(historialId).child("historial_tipo").setValue("canjeado")
            historialRef.child(historialId).child("historial_cantidad").setValue(-puntosNecesarios)
            historialRef.child(historialId).child("historial_producto").setValue(nombreProducto)
            historialRef.child(historialId).child("historial_fecha").setValue(fechaActual)
            historialRef.child(historialId).child("historial_userId").setValue(uid)
        }

        AlertDialog.Builder(this)
            .setTitle("¡Canje exitoso!")
            .setMessage("Tu vale ha sido generado correctamente.")
            .setPositiveButton("Ver vale") { _, _ ->
                val intent = Intent(this, MisValesActivity::class.java)
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }
}