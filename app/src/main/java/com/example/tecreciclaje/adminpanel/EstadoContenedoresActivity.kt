package com.example.tecreciclaje.adminpanel

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.tecreciclaje.AdminPanel
import com.example.tecreciclaje.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class EstadoContenedoresActivity : AppCompatActivity() {

    private lateinit var tvEstadoPlastico: TextView
    private lateinit var tvEstadoAluminio: TextView
    private lateinit var tvUltimaActualizacionPlastico: TextView
    private lateinit var tvUltimaActualizacionAluminio: TextView
    private lateinit var cardPlastico: CardView
    private lateinit var cardAluminio: CardView
    private lateinit var btnActualizarPlastico: Button
    private lateinit var btnActualizarAluminio: Button
    private lateinit var contenedorRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_estado_contenedores)

        // Configurar toolbar
        supportActionBar?.apply {
            title = "Estado de Contenedores"
            setDisplayHomeAsUpEnabled(true)
        }

        // Inicializar vistas
        initViews()
        
        // Configurar Firebase
        contenedorRef = FirebaseDatabase.getInstance().getReference("contenedor")
        
        // Cargar datos
        cargarEstadoContenedores()
        
        // Configurar botones
        configurarBotones()
        
        // Configurar navegación
        setupNavigation()
    }

    private fun initViews() {
        tvEstadoPlastico = findViewById(R.id.tvEstadoPlastico)
        tvEstadoAluminio = findViewById(R.id.tvEstadoAluminio)
        tvUltimaActualizacionPlastico = findViewById(R.id.tvUltimaActualizacionPlastico)
        tvUltimaActualizacionAluminio = findViewById(R.id.tvUltimaActualizacionAluminio)
        cardPlastico = findViewById(R.id.cardPlastico)
        cardAluminio = findViewById(R.id.cardAluminio)
        btnActualizarPlastico = findViewById(R.id.btnActualizarPlastico)
        btnActualizarAluminio = findViewById(R.id.btnActualizarAluminio)
    }

    private fun cargarEstadoContenedores() {
        contenedorRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(this@EstadoContenedoresActivity, 
                        "No se encontraron datos de contenedores", Toast.LENGTH_SHORT).show()
                    return
                }

                // Procesar contenedor de plástico
                val plasticoSnap = snapshot.child("contePlastico")
                if (plasticoSnap.exists()) {
                    val estadoPlastico = plasticoSnap.child("estado").getValue(String::class.java)
                    val updatedAtPlastico = plasticoSnap.child("updatedAt").getValue(Long::class.java)
                    
                    actualizarUIContenedor(tvEstadoPlastico, tvUltimaActualizacionPlastico, 
                        cardPlastico, estadoPlastico, updatedAtPlastico, "Plástico")
                }

                // Procesar contenedor de aluminio
                val aluminioSnap = snapshot.child("conteAluminio")
                if (aluminioSnap.exists()) {
                    val estadoAluminio = aluminioSnap.child("estado").getValue(String::class.java)
                    val updatedAtAluminio = aluminioSnap.child("updatedAt").getValue(Long::class.java)
                    
                    actualizarUIContenedor(tvEstadoAluminio, tvUltimaActualizacionAluminio, 
                        cardAluminio, estadoAluminio, updatedAtAluminio, "Aluminio")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CONTENEDORES", "Error al cargar estado de contenedores", error.toException())
                Toast.makeText(this@EstadoContenedoresActivity, 
                    "Error al cargar datos: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun actualizarUIContenedor(tvEstado: TextView, tvUltimaActualizacion: TextView, 
                                     card: CardView, estado: String?, updatedAt: Long?, tipo: String) {
        
        val estadoFinal = estado ?: "Desconocido"

        // Actualizar estado
        tvEstado.text = estadoFinal
        
        // Configurar color según estado
        val (colorEstado, colorFondo) = when (estadoFinal.lowercase()) {
            "vacio" -> Pair(getColor(R.color.verde_principal), getColor(R.color.verde_claro))
            "medio" -> Pair(getColor(R.color.naranja), getColor(R.color.naranja_claro))
            "lleno" -> Pair(getColor(R.color.rojo), getColor(R.color.rojo_claro))
            else -> Pair(getColor(R.color.dark_gray), getColor(R.color.gris_claro))
        }
        
        tvEstado.setTextColor(colorEstado)
        card.setCardBackgroundColor(colorFondo)

        // Actualizar última actualización
        if (updatedAt != null && updatedAt > 0) {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val fechaFormateada = sdf.format(Date(updatedAt))
            tvUltimaActualizacion.text = "Última actualización: $fechaFormateada"
        } else {
            tvUltimaActualizacion.text = "Sin datos de actualización"
        }
    }

    private fun configurarBotones() {
        btnActualizarPlastico.setOnClickListener { 
            mostrarDialogoActualizarEstado("contePlastico", "Plástico") 
        }
        btnActualizarAluminio.setOnClickListener { 
            mostrarDialogoActualizarEstado("conteAluminio", "Aluminio") 
        }
    }

    private fun mostrarDialogoActualizarEstado(tipoContenedor: String, nombreContenedor: String) {
        AlertDialog.Builder(this)
            .setTitle("Vaciar Contenedor - $nombreContenedor")
            .setMessage("¿Confirmas que el contenedor ha sido vaciado?")
            .setPositiveButton("Sí, Vaciar") { _, _ ->
                Log.d("CONTENEDORES", "Contenedor vaciado: $nombreContenedor")
                actualizarEstadoContenedor(tipoContenedor, "Vacío")
            }
            .setNegativeButton("Cancelar", null)
            .setCancelable(true)
            .show()
    }

    private fun actualizarEstadoContenedor(tipoContenedor: String, nuevoEstado: String) {
        Log.d("CONTENEDORES", "Actualizando $tipoContenedor a estado: $nuevoEstado")
        
        // Crear un Map con los datos del contenedor
        val contenedorData = hashMapOf<String, Any>(
            "estado" to nuevoEstado,
            "updatedAt" to System.currentTimeMillis()
        )
        
        Log.d("CONTENEDORES", "Datos a enviar: $contenedorData")
        
        contenedorRef.child(tipoContenedor).updateChildren(contenedorData)
            .addOnSuccessListener {
                Log.d("CONTENEDORES", "Estado actualizado exitosamente")
                Toast.makeText(this, "Estado actualizado correctamente", Toast.LENGTH_SHORT).show()
                
                // Si el estado es "Vacío", reiniciar contadores
                if (nuevoEstado == "Vacío") {
                    reiniciarContadores(tipoContenedor)
                }
            }
            .addOnFailureListener { e ->
                Log.e("CONTENEDORES", "Error al actualizar estado", e)
                Toast.makeText(this, "Error al actualizar estado: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun reiniciarContadores(tipoContenedor: String) {
        Log.d("CONTENEDORES", "Reiniciando contadores para: $tipoContenedor")
        
        // Crear un registro de reinicio para auditoría
        val reinicioData = hashMapOf<String, Any>(
            "fecha" to System.currentTimeMillis(),
            "tipoContenedor" to tipoContenedor,
            "adminUid" to (FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"),
            "accion" to "vaciado_contenedor"
        )
        
        // Guardar en una colección de reinicios para auditoría
        val reiniciosRef = FirebaseDatabase.getInstance().getReference("reinicios_contadores")
        reiniciosRef.push().setValue(reinicioData)
            .addOnSuccessListener {
                Log.d("CONTENEDORES", "Registro de reinicio guardado")
                
                // Mostrar mensaje de éxito
                val mensaje = if (tipoContenedor == "contePlastico") {
                    "Contenedor de plástico vaciado ✓"
                } else {
                    "Contenedor de aluminio vaciado ✓"
                }
                    
                Toast.makeText(this@EstadoContenedoresActivity, 
                    "$mensaje\nLos contadores se reiniciarán automáticamente", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                Log.e("CONTENEDORES", "Error guardando registro de reinicio", e)
                Toast.makeText(this@EstadoContenedoresActivity, 
                    "Error al registrar el reinicio", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    private fun setupNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.nav_contenedores
        
        bottomNavigationView.setOnItemSelectedListener { item ->
            val itemId = item.itemId
            val currentClass = this::class.java

            when {
                itemId == R.id.nav_dashboard && currentClass != AdminPanel::class.java -> {
                    startActivity(Intent(this, AdminPanel::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                itemId == R.id.nav_scan && currentClass != EscanearQrActivity::class.java -> {
                    startActivity(Intent(this, EscanearQrActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                itemId == R.id.nav_stats && currentClass != EstadisticasActivity::class.java -> {
                    startActivity(Intent(this, EstadisticasActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                itemId == R.id.nav_contenedores && currentClass != EstadoContenedoresActivity::class.java -> {
                    startActivity(Intent(this, EstadoContenedoresActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                itemId == R.id.nav_productos && currentClass != GestionProductosActivity::class.java -> {
                    startActivity(Intent(this, GestionProductosActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                else -> true
            }
        }
    }
}
