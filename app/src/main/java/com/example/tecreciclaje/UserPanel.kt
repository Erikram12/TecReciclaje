package com.example.tecreciclaje

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.tecreciclaje.userpanel.*
import com.example.tecreciclaje.utils.CustomAlertDialog
import com.example.tecreciclaje.utils.FCMTokenManager
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

class UserPanel : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var currentUser: FirebaseUser? = null
    private lateinit var databaseRef: DatabaseReference
    private lateinit var textViewNombre: TextView
    private lateinit var textViewPuntos: TextView
    private lateinit var imageViewProfile: ImageView
    private lateinit var btnCafe: CardView
    private lateinit var btnDesayuno: CardView
    private lateinit var btnComida: CardView
    private lateinit var btnRefresco: CardView
    private lateinit var imgCafe: ImageView
    private lateinit var imgDesayuno: ImageView
    private lateinit var imgComida: ImageView
    private lateinit var imgRefresco: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_panel)

        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser

        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        initializeViews()
        setupNavigation()
        setupLogoutButton()
        getUserData(currentUser!!.uid)
        setupProductButtons()
        
        // ACTUALIZAR TOKEN FCM
        FCMTokenManager.updateTokenForCurrentUser()
    }

    private fun initializeViews() {
        textViewNombre = findViewById(R.id.textViewName)
        textViewPuntos = findViewById(R.id.textViewSaldo)
        imageViewProfile = findViewById(R.id.imageViewProfile)
        
        // Botones de productos (CardViews)
        btnCafe = findViewById(R.id.clothingCard)
        btnDesayuno = findViewById(R.id.clothingCard2)
        btnComida = findViewById(R.id.clothingCard3)
        btnRefresco = findViewById(R.id.clothingCard4)
        
        // Imágenes de productos
        imgCafe = findViewById(R.id.clothingImage)
        imgDesayuno = findViewById(R.id.elecImage)
        imgComida = findViewById(R.id.homeImage)
        imgRefresco = findViewById(R.id.beautyImage)
        
        // NUEVO: Configurar iconos Fluent System Icons
        configurarIconosProductos()
    }

    /**
     * NUEVA FUNCIÓN: Configura las imágenes de productos
     * Usa las imágenes originales definidas en el layout XML
     */
    private fun configurarIconosProductos() {
        try {
            // Configurar imagen de café (usando la imagen original del layout)
            imgCafe.setImageResource(R.drawable.cafesito1)
            
            // Configurar imagen de desayuno (usando la imagen original del layout)
            imgDesayuno.setImageResource(R.drawable.desayuno)
            
            // Configurar imagen de comida (usando la imagen original del layout)
            imgComida.setImageResource(R.drawable.comida)
            
            // Configurar imagen de refresco (usando la imagen original del layout)
            imgRefresco.setImageResource(R.drawable.refresco)
            
            Log.d("UserPanel", "Imágenes de productos configuradas correctamente")
            
        } catch (e: Exception) {
            Log.e("UserPanel", "Error configurando imágenes de productos: ${e.message}")
            // Las imágenes ya están configuradas en el layout XML como fallback
        }
    }

    private fun setupNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.nav_home

        bottomNavigationView.setOnItemSelectedListener { item ->
            val itemId = item.itemId
            val currentClass = this::class.java

            when {
                itemId == R.id.nav_home && currentClass != UserPanel::class.java -> {
                    startActivity(Intent(this, UserPanel::class.java))
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

    private fun setupProductButtons() {
        btnCafe.setOnClickListener { startActivity(Intent(this, CafeActivity::class.java)) }
        btnDesayuno.setOnClickListener { startActivity(Intent(this, DesayunoActivity::class.java)) }
        btnComida.setOnClickListener { startActivity(Intent(this, ComidaActivity::class.java)) }
        btnRefresco.setOnClickListener { startActivity(Intent(this, RefrescoActivity::class.java)) }
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
                    
                    Log.d("UserPanel", "Datos de puntos recibidos: $puntosObj (tipo: ${puntosObj?.javaClass?.simpleName ?: "null"})")
                    
                    puntos = when (puntosObj) {
                        is Int -> {
                            Log.d("UserPanel", "Puntos como Int: $puntosObj")
                            puntosObj
                        }
                        is String -> {
                            try {
                                val parsed = puntosObj.toInt()
                                Log.d("UserPanel", "Puntos como String convertido: $parsed")
                                parsed
                            } catch (e: NumberFormatException) {
                                Log.w("UserPanel", "Error parsing puntos: $puntosObj", e)
                                0
                            }
                        }
                        is Long -> {
                            val converted = puntosObj.toInt()
                            Log.d("UserPanel", "Puntos como Long convertido: $converted")
                            converted
                        }
                        null -> {
                            Log.w("UserPanel", "Puntos es null, estableciendo en 0")
                            0
                        }
                        else -> {
                            Log.w("UserPanel", "Tipo de puntos no reconocido: ${puntosObj.javaClass.simpleName}")
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
                Log.e("UserPanel", "Error obteniendo datos del usuario: ${error.message}")
                Toast.makeText(this@UserPanel, "Error obteniendo datos del usuario", Toast.LENGTH_SHORT).show()
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
