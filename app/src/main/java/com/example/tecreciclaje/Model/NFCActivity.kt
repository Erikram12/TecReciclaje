package com.example.tecreciclaje.Model

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
// Logs eliminados para producción
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tecreciclaje.MainActivity
import com.example.tecreciclaje.R
import com.example.tecreciclaje.domain.model.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class NFCActivity : AppCompatActivity() {

    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var nombre: String
    private lateinit var apellido: String
    private lateinit var numControl: String
    private lateinit var carrera: String
    private lateinit var email: String
    private lateinit var password: String
    private lateinit var role: String
    private lateinit var perfil: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfc)

        nombre = intent.getStringExtra("nombre") ?: ""
        apellido = intent.getStringExtra("apellido") ?: ""
        numControl = intent.getStringExtra("numControl") ?: ""
        carrera = intent.getStringExtra("carrera") ?: ""
        email = intent.getStringExtra("email") ?: ""
        password = intent.getStringExtra("password") ?: ""
        role = intent.getStringExtra("role") ?: ""
        perfil = intent.getStringExtra("perfil") ?: ""

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (nfcAdapter == null) {
            Toast.makeText(this, "Este dispositivo no soporta NFC", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        val intent = Intent(this, this::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_MUTABLE
        )
        val tagDetected = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        val filters = arrayOf(tagDetected)
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, filters, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            val nfcUid = bytesToHex(tag?.id ?: byteArrayOf())
            registrarUsuario(nombre, apellido, numControl, carrera, email, password, role, perfil, nfcUid)
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02X".format(it) }
    }

    private fun registrarUsuario(
        nombre: String, apellido: String, numControl: String, carrera: String,
        email: String, password: String, role: String, perfil: String, nfcUid: String
    ) {
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful()) {
                    val authUid = FirebaseAuth.getInstance().currentUser?.uid ?: return@addOnCompleteListener

                    // Crear usuario sin usuario_puntos inicialmente
                    val usuario = Usuario(authUid, nombre, apellido, numControl, carrera, email, role, perfil, nfcUid)
                    // Establecer usuario_puntos en 0 para que se cree después
                    usuario.usuario_puntos = 0 // Esto se sobrescribirá
                    usuario.usuario_provider = "email" // Establecer provider para email

                    val db = FirebaseDatabase.getInstance().reference
                    db.child("usuarios").child(authUid).setValue(usuario)
                        .addOnSuccessListener {
                            // Guardar índice NFC
                            db.child("nfc_index").child(nfcUid).setValue(authUid)
                                .addOnSuccessListener {
                                    // Crear user_puntos después para activar la Cloud Function
                                    // Para usuarios nuevos, crear directamente con valor 0
                                    // La Cloud Function se activará pero no creará historial para valor 0
                                    db.child("usuarios").child(authUid).child("usuario_puntos").setValue(0)
                                        .addOnSuccessListener {
                                            // Log de depuración eliminado
                                            
                                            Toast.makeText(this, "Registro completo con NFC", Toast.LENGTH_SHORT).show()
                                            // Redirige a MainActivity
                                            val intent = Intent(this, MainActivity::class.java)
                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            startActivity(intent)
                                        }
                                        .addOnFailureListener { e ->
                                            // Log de error eliminado
                                        }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Error guardando índice NFC: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error guardando datos: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
