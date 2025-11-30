package com.example.tecreciclaje

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.example.tecreciclaje.utils.AppLogger
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tecreciclaje.domain.model.Producto
import com.example.tecreciclaje.domain.repository.ProductoRepositoryImpl
import com.example.tecreciclaje.userpanel.*
import com.example.tecreciclaje.utils.CustomAlertDialog
import com.example.tecreciclaje.utils.FCMTokenManager
import com.example.tecreciclaje.utils.LocaleHelper
import com.example.tecreciclaje.utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import kotlinx.coroutines.*

class UserPanelDynamic : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var currentUser: FirebaseUser? = null
    private lateinit var databaseRef: DatabaseReference
    private lateinit var textViewNombre: TextView
    private lateinit var textViewPuntos: TextView
    private lateinit var imageViewProfile: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvNoProductos: TextView
    private lateinit var productosAdapter: ProductosUserAdapter
    private lateinit var productoRepository: ProductoRepositoryImpl

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.attachBaseContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_panel_dynamic)

        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser

        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        productoRepository = ProductoRepositoryImpl()
        initializeViews()
        setupNavigation()
        setupLogoutButton()
        setupLanguageButton()
        getUserData(currentUser!!.uid)
        setupProductosRecyclerView()
        loadProductos()
        
        // ACTUALIZAR TOKEN FCM
        FCMTokenManager.updateTokenForCurrentUser()
    }

    private fun initializeViews() {
        textViewNombre = findViewById(R.id.textViewName)
        textViewPuntos = findViewById(R.id.textViewSaldo)
        imageViewProfile = findViewById(R.id.imageViewProfile)
        recyclerView = findViewById(R.id.recyclerViewProductos)
        tvNoProductos = findViewById(R.id.tvNoProductos)
    }

    private fun setupProductosRecyclerView() {
        productosAdapter = ProductosUserAdapter { producto ->
            // Navegar a la actividad de detalle del producto
            val intent = Intent(this, ProductoDetalleActivity::class.java)
            intent.putExtra("producto", producto)
            startActivity(intent)
        }
        
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = productosAdapter
    }

    private fun loadProductos() {
        lifecycleScope.launch {
            try {
                productoRepository.obtenerProductos().collect { productos ->
                    productosAdapter.updateProductos(productos)
                    
                    // Mostrar/ocultar mensaje de no productos
                    tvNoProductos.visibility = if (productos.isEmpty()) {
                        android.view.View.VISIBLE
                    } else {
                        android.view.View.GONE
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("Error cargando productos", e)
            }
        }
    }

    private fun setupNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.nav_home

        bottomNavigationView.setOnItemSelectedListener { item ->
            val itemId = item.itemId
            val currentClass = this::class.java

            when {
                itemId == R.id.nav_home && currentClass != UserPanelDynamic::class.java -> {
                    startActivity(Intent(this, UserPanelDynamic::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                itemId == R.id.nav_docs && currentClass != MisValesActivity::class.java -> {
                    startActivity(Intent(this, MisValesActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                itemId == R.id.nav_histori && currentClass != HistorialActivity::class.java -> {
                    startActivity(Intent(this, HistorialActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                itemId == R.id.nav_perfil && currentClass != PerfilActivity::class.java -> {
                    startActivity(Intent(this, PerfilActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                else -> true
            }
        }
    }

    private fun setupLogoutButton() {
        val logoutButton = findViewById<ImageButton>(R.id.logout_button)
        logoutButton.setOnClickListener {
            CustomAlertDialog.createLogoutDialogWithAnimation(this) {
                SessionManager.clearCompleteSession(this)
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }.show()
        }
    }

    private fun setupLanguageButton() {
        val btnCambiarIdioma = findViewById<ImageButton>(R.id.btnCambiarIdioma)
        btnCambiarIdioma.setOnClickListener {
            cambiarIdioma()
        }
    }

    private fun cambiarIdioma() {
        val idiomaActual = LocaleHelper.getCurrentLanguage(this)
        val nuevoIdioma = if (idiomaActual == "es") "zap" else "es"
        
        LocaleHelper.saveLanguage(this, nuevoIdioma)
        
        // Recargar la actividad para aplicar el nuevo idioma
        recreate()
    }

    private fun getUserData(uid: String) {
        val userRef = FirebaseDatabase.getInstance().getReference("usuarios").child(uid)

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val nombre = snapshot.child("usuario_nombre").getValue(String::class.java)
                    val apellido = snapshot.child("usuario_apellido").getValue(String::class.java)
                    
                    // MEJORADO: Manejo seguro de puntos con logs
                    val puntosObj = snapshot.child("usuario_puntos").value
                    var puntos: Int = 0
                    
                    AppLogger.d("Datos de puntos recibidos: $puntosObj (tipo: ${puntosObj?.javaClass?.simpleName ?: "null"})")
                    
                    puntos = when (puntosObj) {
                        is Int -> {
                            AppLogger.d("Puntos como Int: $puntosObj")
                            puntosObj
                        }
                        is String -> {
                            try {
                                val parsed = puntosObj.toInt()
                                AppLogger.d("Puntos como String convertido: $parsed")
                                parsed
                            } catch (e: NumberFormatException) {
                                AppLogger.w("Error parsing puntos: $puntosObj", e)
                                0
                            }
                        }
                        is Long -> {
                            val converted = puntosObj.toInt()
                            AppLogger.d("Puntos como Long convertido: $converted")
                            converted
                        }
                        null -> {
                            AppLogger.w("Puntos es null, estableciendo en 0")
                            0
                        }
                        else -> {
                            AppLogger.w("Tipo de puntos no reconocido: ${puntosObj.javaClass.simpleName}")
                            0
                        }
                    }

                    // Actualizar UI
                    val nombreCompleto = "${nombre ?: ""} ${apellido ?: ""}".trim()
                    textViewNombre.text = nombreCompleto
                    textViewPuntos.text = puntos.toString()

                    // Configurar imagen de perfil
                    val perfilUrl = snapshot.child("usuario_perfil").getValue(String::class.java)
                    if (!perfilUrl.isNullOrEmpty()) {
                        Picasso.get()
                            .load(perfilUrl)
                            .transform(com.example.tecreciclaje.utils.CircleTransform())
                            .placeholder(R.drawable.user)
                            .error(R.drawable.user)
                            .into(imageViewProfile)
                    } else {
                        imageViewProfile.setImageResource(R.drawable.user)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Solo registrar el error en LogCat sin mostrar al usuario
                AppLogger.e("Error obteniendo datos del usuario: ${error.message}")
            }
        })
    }

    override fun onBackPressed() {
        super.onBackPressed()
        CustomAlertDialog.createLogoutDialogWithAnimation(this) {
            SessionManager.clearCompleteSession(this)
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }.show()
    }
}
