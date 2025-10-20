package com.example.tecreciclaje.userpanel

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tecreciclaje.LoginActivity
import com.example.tecreciclaje.R
import com.example.tecreciclaje.domain.model.Vale
import com.example.tecreciclaje.Model.ValesAdapter
import com.example.tecreciclaje.UserPanelDynamic
import com.example.tecreciclaje.utils.TutorialManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.Timestamp
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.journeyapps.barcodescanner.BarcodeEncoder

class MisValesActivity : AppCompatActivity() {

    private lateinit var recyclerVales: RecyclerView
    private lateinit var adapter: ValesAdapter
    private lateinit var auth: FirebaseAuth
    private var currentUser: FirebaseUser? = null
    private val valesList = mutableListOf<Vale>()
    private lateinit var spinnerFiltro: Spinner
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var btnAyuda: ImageButton
    private lateinit var btnVerTutorial: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_vales)

        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser

        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        initializeViews()
        obtenerVales(currentUser!!.uid)
        setupNavigation()
        setupSpinner()
        
        // MOSTRAR TUTORIAL SI ES NECESARIO
        TutorialManager.showMisValesTutorialIfNeeded(this)
    }

    private fun initializeViews() {
        recyclerVales = findViewById(R.id.recyclerVales)
        spinnerFiltro = findViewById(R.id.spinnerFiltro)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        btnAyuda = findViewById(R.id.btnAyuda)
        btnVerTutorial = findViewById(R.id.btnVerTutorial)

        adapter = ValesAdapter(valesList, this)
        recyclerVales.layoutManager = LinearLayoutManager(this)
        recyclerVales.adapter = adapter
        
        // Configurar botones
        setupHelpButton()
        setupTutorialButton()
    }

    private fun setupSpinner() {
        spinnerFiltro.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val filtroSeleccionado = parent?.getItemAtPosition(position).toString() ?: ""
                filtrarVales(filtroSeleccionado)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.nav_docs

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

    /**
     * Configura el botón de ayuda para mostrar el tutorial
     */
    private fun setupHelpButton() {
        btnAyuda.setOnClickListener {
            // Mostrar tutorial manualmente
            TutorialManager.showMisValesTutorial(this)
        }
    }

    /**
     * Configura el botón de tutorial en el estado vacío
     */
    private fun setupTutorialButton() {
        btnVerTutorial.setOnClickListener {
            // Mostrar tutorial manualmente
            TutorialManager.showMisValesTutorial(this)
        }
    }

    /**
     * Maneja la visibilidad del estado vacío y la lista de vales
     */
    private fun actualizarEstadoVista() {
        if (valesList.isEmpty()) {
            // Mostrar estado vacío
            emptyStateLayout.visibility = View.VISIBLE
            recyclerVales.visibility = View.GONE
        } else {
            // Mostrar lista de vales
            emptyStateLayout.visibility = View.GONE
            recyclerVales.visibility = View.VISIBLE
        }
    }

    private fun filtrarVales(estadoFiltro: String) {
        val filtrados = mutableListOf<Vale>()

        println("Filtrando vales con filtro: $estadoFiltro")
        println("Total de vales en lista original: ${valesList.size}")

        for (vale in valesList) {
            println("Procesando vale - Estado: ${vale.vale_estado}, Descripción: ${vale.vale_descripcion}")
            
            when {
                estadoFiltro.equals("Todos", ignoreCase = true) -> {
                    filtrados.add(vale)
                    println("Vale agregado (Todos)")
                }
                estadoFiltro.equals("Válido", ignoreCase = true) && vale.vale_estado.equals("Válido", ignoreCase = true) -> {
                    filtrados.add(vale)
                    println("Vale agregado (Válido)")
                }
                estadoFiltro.equals("Disponible", ignoreCase = true) && vale.vale_estado.equals("disponible", ignoreCase = true) -> {
                    filtrados.add(vale)
                    println("Vale agregado (Disponible)")
                }
                estadoFiltro.equals("Expirado", ignoreCase = true) && vale.vale_estado.equals("expirado", ignoreCase = true) -> {
                    filtrados.add(vale)
                    println("Vale agregado (Expirado)")
                }
                estadoFiltro.equals("Canjeado", ignoreCase = true) && vale.vale_estado.equals("canjeado", ignoreCase = true) -> {
                    filtrados.add(vale)
                    println("Vale agregado (Canjeado)")
                }
                else -> {
                    println("Vale NO agregado - Estado no coincide")
                }
            }
        }

        println("Vales filtrados: ${filtrados.size}")

        // Crear nuevo adaptador con los datos filtrados
        val nuevoAdapter = ValesAdapter(filtrados, this)
        recyclerVales.adapter = nuevoAdapter
        
        // Actualizar estado de la vista basado en los vales filtrados
        if (filtrados.isEmpty()) {
            emptyStateLayout.visibility = View.VISIBLE
            recyclerVales.visibility = View.GONE
        } else {
            emptyStateLayout.visibility = View.GONE
            recyclerVales.visibility = View.VISIBLE
        }
    }

    private fun obtenerVales(uid: String) {
        // Intentar primero la estructura original
        val valesRef = FirebaseDatabase.getInstance().getReference("vales")

        valesRef.orderByChild("vale_usuario_id").equalTo(uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    valesList.clear()
                    val ahora = System.currentTimeMillis()

                    // Log para depuración
                    println("Buscando vales para usuario: $uid")
                    println("Número de vales encontrados: ${snapshot.childrenCount}")

                    for (s in snapshot.children) {
                        println("Procesando vale con ID: ${s.key}")
                        
                        // Intentar obtener el vale con el nuevo modelo
                        val vale = s.getValue(Vale::class.java)

                        if (vale != null) {
                            vale.vale_id = s.key ?: ""
                            println("Vale encontrado - Estado: ${vale.vale_estado}, Descripción: ${vale.vale_descripcion}")
                            
                            // MEJORADO: Verificación de expiración más robusta
                            verificarExpiracionVale(vale, s)
                            
                            valesList.add(vale)
                        } else {
                            println("Error al deserializar vale con ID: ${s.key}")
                            // Intentar obtener datos manualmente para debug
                            for (child in s.children) {
                                println("Campo ${child.key} = ${child.value}")
                            }
                        }
                    }

                    println("Total de vales en lista: ${valesList.size}")

                    // Si no se encontraron vales, intentar en otra ubicación
                    if (valesList.isEmpty()) {
                        println("No se encontraron vales, buscando alternativo...")
                        buscarValesAlternativo(uid)
                    } else {
                        // Aplicar el filtro actual si hay uno seleccionado
                        if (spinnerFiltro.selectedItem != null) {
                            val filtroSeleccionado = spinnerFiltro.selectedItem.toString()
                            println("Aplicando filtro: $filtroSeleccionado")
                            filtrarVales(filtroSeleccionado)
                        } else {
                            // Si no hay filtro, mostrar todos
                            println("Mostrando todos los vales sin filtro")
                            adapter.notifyDataSetChanged()
                            actualizarEstadoVista()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    println("Error al obtener vales: ${error.message}")
                    Toast.makeText(this@MisValesActivity, 
                        "Error al obtener vales: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun buscarValesAlternativo(uid: String) {
        // Intentar en usuarios/{uid}/vales
        val ref = FirebaseDatabase.getInstance()
            .getReference("usuarios").child(uid).child("vales")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    valesList.clear()
                    for (s in snapshot.children) {
                        val vale = s.getValue(Vale::class.java)
                        if (vale != null) {
                            vale.vale_id = s.key ?: ""
                            valesList.add(vale)
                        }
                    }
                    
                    // Aplicar el filtro actual si hay uno seleccionado
                    if (spinnerFiltro.selectedItem != null) {
                        val filtroSeleccionado = spinnerFiltro.selectedItem.toString()
                        filtrarVales(filtroSeleccionado)
                    } else {
                        adapter.notifyDataSetChanged()
                        actualizarEstadoVista()
                    }
                } else {
                    // No se encontraron vales en ninguna ubicación
                    valesList.clear()
                    adapter.notifyDataSetChanged()
                    actualizarEstadoVista()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MisValesActivity, 
                    "Error al buscar vales alternativo: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * NUEVA FUNCIÓN: Verificación mejorada de expiración de vales
     */
    private fun verificarExpiracionVale(vale: Vale, snapshot: DataSnapshot) {
        val ahora = System.currentTimeMillis()
        var debeActualizar = false
        
        // Verificar por fecha de expiración específica
        if (vale.vale_fecha_expiracion != null) {
            val fechaExpiracion = when (val fecha = vale.vale_fecha_expiracion) {
                is Long -> fecha
                is Timestamp -> fecha.toDate().time
                else -> 0L
            }
            
            if (fechaExpiracion > 0L && ahora >= fechaExpiracion) {
                // El vale ha expirado por fecha específica
                if (!vale.vale_estado.equals("expirado", ignoreCase = true) && !vale.vale_estado.equals("Caducado", ignoreCase = true)) {
                    vale.vale_estado = "expirado"
                    debeActualizar = true
                    println("DEBUG: Vale ${vale.vale_id} expirado por fecha específica")
                }
            }
        }
        // Verificar por fecha de creación (3 días) como fallback
        else if (vale.vale_fechaCreacion != null) {
            val fechaCreacion = vale.vale_fechaCreacion!!.toDate().time
            val tresDiasEnMs = 3 * 24 * 60 * 60 * 1000L // 3 días
            val tiempoTranscurrido = ahora - fechaCreacion
            
            if (tiempoTranscurrido > tresDiasEnMs) {
                // El vale ha expirado por tiempo transcurrido
                if (!vale.vale_estado.equals("expirado", ignoreCase = true) && !vale.vale_estado.equals("Caducado", ignoreCase = true)) {
                    vale.vale_estado = "expirado"
                    debeActualizar = true
                    println("Vale ${vale.vale_id} expirado por tiempo transcurrido")
                }
            }
        }
        
        // NUEVO: Estandarizar "Caducado" a "expirado"
        if (vale.vale_estado.equals("Caducado", ignoreCase = true)) {
            vale.vale_estado = "expirado"
            debeActualizar = true
            println("Vale ${vale.vale_id} estandarizado de 'Caducado' a 'expirado'")
        }
        
        // Actualizar en la base de datos si es necesario
        if (debeActualizar) {
            snapshot.ref.child("vale_estado").setValue("expirado")
            if (vale.vale_fecha_expiracion == null) {
                snapshot.ref.child("vale_fecha_expiracion").setValue(ahora)
            }
            println("Vale ${vale.vale_id} actualizado como expirado en BD")
        }
    }

    fun mostrarQrDialog(valeId: String) {
        // Crear el contenido del QR
        val qrContent = "VALE_${valeId}_${currentUser!!.uid}"

        // Generar el QR
        val writer = MultiFormatWriter()
        try {
            val bitMatrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, 400, 400)
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.createBitmap(bitMatrix)

            // Mostrar el diálogo
            val builder = AlertDialog.Builder(this)
            val dialogView = layoutInflater.inflate(R.layout.dialog_qr, null)
            val qrImageView = dialogView.findViewById<ImageView>(R.id.qrImageView)
            qrImageView.setImageBitmap(bitmap)

            builder.setView(dialogView)
                .setTitle("Código QR del Vale")
                .setMessage("Muestra este código al personal para canjear tu vale")
                .setPositiveButton("Cerrar", null)
                .show()

        } catch (e: Exception) {
            Toast.makeText(this, "Error al generar QR", Toast.LENGTH_SHORT).show()
        }
    }
}
