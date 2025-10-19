package com.example.tecreciclaje.userpanel

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.tecreciclaje.LoginActivity
import com.example.tecreciclaje.R
import com.example.tecreciclaje.UserPanel
import com.example.tecreciclaje.utils.TutorialManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*

class RefrescoActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var textViewProgreso: TextView
    private lateinit var textPrecioPuntos: TextView
    private lateinit var textProgresoDetallado: TextView
    private lateinit var btnCanjear: Button
    private lateinit var btnBack: ImageButton
    private lateinit var progressBar: ProgressBar
    
    companion object {
        private const val PUNTOS_NECESARIOS = 150
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_refresco)

        auth = FirebaseAuth.getInstance()
        initializeViews()

        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        getUserData(currentUser.uid)
        setupNavigation()
        setupBackButton()
        
        // MOSTRAR TUTORIAL SI ES NECESARIO
        TutorialManager.showProductoTutorialIfNeeded(this, "Refresco", R.drawable.refresco)
    }

    private fun initializeViews() {
        textViewProgreso = findViewById(R.id.textViewProgreso)
        textPrecioPuntos = findViewById(R.id.textPrecioPuntos)
        textProgresoDetallado = findViewById(R.id.textProgresoDetallado)
        btnCanjear = findViewById(R.id.btnCanjear)
        btnBack = findViewById(R.id.btnBack)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.nav_home
        bottomNavigationView.setOnItemSelectedListener { item ->
            val itemId = item.itemId
            when (itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, UserPanel::class.java))
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

    private fun getUserData(uid: String) {
        val userRef = FirebaseDatabase.getInstance().getReference("usuarios").child(uid)
        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val puntosObj = snapshot.child("usuario_puntos").value
                    var puntos = 0
                    
                    puntos = when (puntosObj) {
                        is Int -> puntosObj
                        is String -> {
                            try {
                                puntosObj.toInt()
                            } catch (e: NumberFormatException) {
                                0
                            }
                        }
                        is Long -> puntosObj.toInt()
                        else -> 0
                    }
                    
                    textViewProgreso.text = "$puntos pts"
                    textProgresoDetallado.text = "$puntos/$PUNTOS_NECESARIOS"
                    
                    val progreso = minOf((puntos * 100) / PUNTOS_NECESARIOS, 100)
                    progressBar.progress = progreso
                    
                    btnCanjear.isEnabled = puntos >= PUNTOS_NECESARIOS
                    setupCanjearButton(puntos, uid)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("RefrescoActivity", "Error al obtener datos del usuario", error.toException())
            }
        })
    }

    private fun setupCanjearButton(puntos: Int, uid: String) {
        btnCanjear.setOnClickListener {
            if (puntos >= PUNTOS_NECESARIOS) {
                AlertDialog.Builder(this)
                    .setTitle("Confirmar Canje")
                    .setMessage("¿Estás seguro de que deseas canjear $PUNTOS_NECESARIOS puntos por un Refresco?")
                    .setPositiveButton("Sí") { _, _ ->
                        realizarCanje(uid, puntos)
                    }
                    .setNegativeButton("No", null)
                    .show()
            } else {
                AlertDialog.Builder(this)
                    .setTitle("Puntos insuficientes")
                    .setMessage("Necesitas al menos $PUNTOS_NECESARIOS puntos para canjear este producto.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun realizarCanje(uid: String, puntosActuales: Int) {
        val userRef = FirebaseDatabase.getInstance().getReference("usuarios").child(uid)
        val historialRef = userRef.child("historial")
        val valesRef = FirebaseDatabase.getInstance().getReference("vales")

        val nuevosPuntos = puntosActuales - PUNTOS_NECESARIOS
        val fechaActual = System.currentTimeMillis()
        val fechaExpira = fechaActual + (3 * 24 * 60 * 60 * 1000)

        userRef.child("usuario_puntos").setValue(nuevosPuntos)

        val valeId = valesRef.push().key
        if (valeId != null) {
            val nuevoValeRef = valesRef.child(valeId)
            nuevoValeRef.child("vale_usuario_id").setValue(uid)
            nuevoValeRef.child("vale_producto").setValue("Refresco")
            nuevoValeRef.child("vale_estado").setValue("Válido")
            nuevoValeRef.child("vale_imagen_url").setValue("https://firebasestorage.googleapis.com/v0/b/resiclaje-39011.firebasestorage.app/o/refresco.png?alt=media&token=example")
            nuevoValeRef.child("vale_fecha_creacion").setValue(fechaActual)
            nuevoValeRef.child("vale_fecha_expiracion").setValue(fechaExpira)
        }

        val historialId = historialRef.push().key
        if (historialId != null) {
            historialRef.child(historialId).child("historial_tipo").setValue("canjeado")
            historialRef.child(historialId).child("historial_cantidad").setValue(-PUNTOS_NECESARIOS)
            historialRef.child(historialId).child("historial_producto").setValue("Refresco")
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
