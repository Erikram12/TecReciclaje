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
import com.example.tecreciclaje.utils.CustomAlertDialog
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
        println("DEBUG: Contenido completo del QR: $qrContent")
        
        if (!qrContent.isNullOrEmpty() && qrContent.startsWith("VALE_")) {
            // Remover el prefijo "VALE_"
            val sinPrefijo = qrContent.removePrefix("VALE_")
            println("DEBUG: Contenido sin prefijo 'VALE_': $sinPrefijo")
            
            // Encontrar el último "_" que separa el valeId del userId
            // El userId está al final después del último "_"
            val lastUnderscoreIndex = sinPrefijo.lastIndexOf("_")
            println("DEBUG: Índice del último '_': $lastUnderscoreIndex")
            
            if (lastUnderscoreIndex > 0) {
                // Extraer todo lo que está antes del último "_" (el valeId completo)
                val valeId = sinPrefijo.substring(0, lastUnderscoreIndex)
                val userId = sinPrefijo.substring(lastUnderscoreIndex + 1)
                println("DEBUG: ValeId extraído (completo): $valeId")
                println("DEBUG: UserId extraído: $userId")
                return valeId
            } else {
                println("ERROR: Formato de QR inválido - no se encontró separador para userId")
            }
        } else {
            println("ERROR: QR no comienza con 'VALE_' o es null")
        }
        return null
    }

    private fun marcarComoReclamado(valeId: String) {
        println("Intentando marcar vale como reclamado. ValeId: $valeId")
        buscarValeEnDiferentesUbicaciones(valeId)
    }
    
    private fun buscarValeEnDiferentesUbicaciones(valeId: String) {
        println("DEBUG: Buscando vale con ID: '$valeId' en diferentes ubicaciones...")
        
        // Limpiar el valeId por si tiene algún prefijo (ej: "vales-")
        val valeIdLimpio = valeId.removePrefix("vales-").removePrefix("VALE_")
        println("DEBUG: ValeId limpio: '$valeIdLimpio'")
        
        val valeRef = FirebaseDatabase.getInstance().getReference("vales").child(valeIdLimpio)
        println("DEBUG: Buscando en ruta: /vales/$valeIdLimpio")
        valeRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                println("DEBUG: ✅ Vale encontrado en /vales/$valeIdLimpio")
                procesarValeEncontrado(valeRef, snapshot)
            } else {
                println("DEBUG: Vale no encontrado en /vales/$valeIdLimpio, buscando en usuarios...")
                buscarEnUsuarios(valeIdLimpio)
            }
        }.addOnFailureListener { e ->
            println("DEBUG: Error buscando en /vales/: ${e.message}")
            buscarEnUsuarios(valeIdLimpio)
        }
    }
    
    private fun buscarEnUsuarios(valeId: String) {
        println("DEBUG: Buscando vale '$valeId' en estructura de usuarios...")
        
        // Buscar en todos los usuarios
        val usuariosRef = FirebaseDatabase.getInstance().getReference("usuarios")
        usuariosRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var encontrado = false
                var usuariosRevisados = 0
                
                for (userSnap in snapshot.children) {
                    val userId = userSnap.key
                    if (userId != null) {
                        usuariosRevisados++
                        val valesSnap = userSnap.child("vales")
                        
                        if (valesSnap.exists()) {
                            println("DEBUG: Revisando usuario $userId, buscando vale '$valeId'")
                            val valeSnap = valesSnap.child(valeId)
                            if (valeSnap.exists()) {
                                println("DEBUG: ✅ Vale encontrado en usuarios/$userId/vales/$valeId")
                                val valeRef = usuariosRef.child(userId).child("vales").child(valeId)
                                procesarValeEncontrado(valeRef, valeSnap)
                                encontrado = true
                                break
                            }
                        }
                    }
                }
                
                println("DEBUG: Revisados $usuariosRevisados usuarios")
                if (!encontrado) {
                    println("DEBUG: ❌ Vale '$valeId' no encontrado en ninguna ubicación")
                    mostrarErrorValeNoEncontrado(valeId)
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                println("DEBUG: ❌ Error buscando en usuarios: ${error.message}")
                mostrarErrorValeNoEncontrado(valeId)
            }
        })
    }
    
    private fun procesarValeEncontrado(valeRef: DatabaseReference, valeSnap: DataSnapshot) {
        val estadoActual = valeSnap.child("vale_estado").getValue(String::class.java)
        val producto = valeSnap.child("vale_producto").getValue(String::class.java) ?: "Producto no especificado"
        val usuarioId = valeSnap.child("vale_usuario_id").getValue(String::class.java)
        
        println("DEBUG: Vale encontrado - Estado: $estadoActual, Producto: $producto, Usuario: $usuarioId")
        
        if (estadoActual == "Válido") {
            // Mostrar diálogo informativo con el producto y marcar como reclamado
            mostrarDialogoInformacionProducto(producto, valeRef)
        } else {
            println("DEBUG: Vale ya no está válido. Estado: $estadoActual")
            Toast.makeText(this, "Este vale ya no está válido (Estado: $estadoActual)", Toast.LENGTH_LONG).show()
            barcodeView.resume()
        }
    }
    
    private fun mostrarDialogoInformacionProducto(producto: String, valeRef: DatabaseReference) {
        CustomAlertDialog.createInfoProductoEntregaDialog(
            context = this,
            nombreProducto = producto,
            onOKClick = {
                // Cuando se presiona OK, marcar como reclamado
                marcarValeComoReclamado(valeRef)
            }
        ).show()
    }
    
    private fun marcarValeComoReclamado(valeRef: DatabaseReference) {
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
