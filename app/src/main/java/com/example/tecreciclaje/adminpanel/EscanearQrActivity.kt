package com.example.tecreciclaje.adminpanel

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.tecreciclaje.AdminPanel
import com.example.tecreciclaje.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.*
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CompoundBarcodeView

class EscanearQrActivity : AppCompatActivity() {

    private lateinit var barcodeView: CompoundBarcodeView
    
    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_escanear_qr)

        barcodeView = findViewById(R.id.barcode_scanner)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        } else {
            iniciarEscaneo()
        }
        
        setupNavigation()
    }

    private fun iniciarEscaneo() {
        barcodeView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                val qrContent = result.text
                barcodeView.pause()
                
                // Extraer el valeId del contenido del QR
                val valeId = extraerValeId(qrContent)
                if (valeId != null) {
                    marcarComoReclamado(valeId)
                } else {
                    Toast.makeText(this@EscanearQrActivity, "QR inválido", Toast.LENGTH_SHORT).show()
                    barcodeView.resume()
                }
            }
        })
        barcodeView.resume()
    }

    private fun extraerValeId(qrContent: String?): String? {
        println("DEBUG: Contenido del QR: $qrContent")
        
        // El formato del QR es: "VALE_[valeId]_[userId]"
        // Ejemplo: "VALE_-OXkK1QU5xbSAv-aztK2_D45OSMp1V9cGaecHbFBmTYg1rvk2"
        
        if (!qrContent.isNullOrEmpty() && qrContent.startsWith("VALE_")) {
            val parts = qrContent.split("_")
            println("DEBUG: Partes del QR: ${parts.size}")
            for (i in parts.indices) {
                println("DEBUG: Parte $i: ${parts[i]}")
            }
            
            if (parts.size >= 3) {
                // parts[0] = "VALE"
                // parts[1] = valeId
                // parts[2] = userId
                val valeId = parts[1]
                println("DEBUG: ValeId extraído: $valeId")
                return valeId
            } else {
                println("DEBUG: Formato de QR inválido - no hay suficientes partes")
            }
        } else {
            println("DEBUG: QR no comienza con 'VALE_' o es null")
        }
        return null
    }

    private fun marcarComoReclamado(valeId: String) {
        println("DEBUG: Intentando marcar vale como reclamado. ValeId: $valeId")
        
        // MEJORADO: Buscar en múltiples ubicaciones posibles
        buscarValeEnDiferentesUbicaciones(valeId)
    }
    
    private fun buscarValeEnDiferentesUbicaciones(valeId: String) {
        println("DEBUG: Buscando vale en diferentes ubicaciones...")
        
        // 1. Buscar en /vales/{valeId} (ubicación principal)
        val valeRef = FirebaseDatabase.getInstance().getReference("vales").child(valeId)
        valeRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                println("DEBUG: Vale encontrado en /vales/$valeId")
                procesarValeEncontrado(valeRef, snapshot)
            } else {
                println("DEBUG: Vale no encontrado en /vales/, buscando en usuarios...")
                // 2. Buscar en usuarios/{userId}/vales/{valeId}
                buscarEnUsuarios(valeId)
            }
        }.addOnFailureListener { e ->
            println("DEBUG: Error buscando en /vales/: ${e.message}")
            buscarEnUsuarios(valeId)
        }
    }
    
    private fun buscarEnUsuarios(valeId: String) {
        println("DEBUG: Buscando vale en estructura de usuarios...")
        
        // Buscar en todos los usuarios
        val usuariosRef = FirebaseDatabase.getInstance().getReference("usuarios")
        usuariosRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var encontrado = false
                
                for (userSnap in snapshot.children) {
                    val userId = userSnap.key
                    if (userId != null) {
                        val valesSnap = userSnap.child("vales")
                        
                        if (valesSnap.exists()) {
                            val valeSnap = valesSnap.child(valeId)
                            if (valeSnap.exists()) {
                                println("DEBUG: Vale encontrado en usuarios/$userId/vales/$valeId")
                                val valeRef = usuariosRef.child(userId).child("vales").child(valeId)
                                procesarValeEncontrado(valeRef, valeSnap)
                                encontrado = true
                                break
                            }
                        }
                    }
                }
                
                if (!encontrado) {
                    println("DEBUG: Vale no encontrado en ninguna ubicación")
                    mostrarErrorValeNoEncontrado(valeId)
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                println("DEBUG: Error buscando en usuarios: ${error.message}")
                mostrarErrorValeNoEncontrado(valeId)
            }
        })
    }
    
    private fun procesarValeEncontrado(valeRef: DatabaseReference, valeSnap: DataSnapshot) {
        val estadoActual = valeSnap.child("vale_estado").getValue(String::class.java)
        val producto = valeSnap.child("vale_producto").getValue(String::class.java)
        val usuarioId = valeSnap.child("vale_usuario_id").getValue(String::class.java)
        
        println("DEBUG: Vale encontrado - Estado: $estadoActual, Producto: $producto, Usuario: $usuarioId")
        
        if (estadoActual == "Válido") {
            // Actualizar el estado a Reclamado
            valeRef.child("vale_estado").setValue("Reclamado")
                .addOnSuccessListener {
                    println("DEBUG: Vale marcado como RECLAMADO exitosamente")
                    Toast.makeText(this, "Vale marcado como RECLAMADO", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    println("DEBUG: Error al actualizar estado: ${e.message}")
                    Toast.makeText(this, "Error al actualizar el vale: ${e.message}", Toast.LENGTH_SHORT).show()
                    barcodeView.resume()
                }
        } else {
            println("DEBUG: Vale ya no está válido. Estado: $estadoActual")
            Toast.makeText(this, "Este vale ya no está válido (Estado: $estadoActual)", Toast.LENGTH_LONG).show()
            barcodeView.resume()
        }
    }
    
    private fun mostrarErrorValeNoEncontrado(valeId: String) {
        println("DEBUG: Vale no encontrado en ninguna ubicación. ValeId: $valeId")
        Toast.makeText(this, "Vale no encontrado en la base de datos. ID: $valeId", Toast.LENGTH_LONG).show()
        barcodeView.resume()
    }

    override fun onResume() {
        super.onResume()
        barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                iniciarEscaneo()
            } else {
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    
    private fun setupNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.nav_scan
        
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
