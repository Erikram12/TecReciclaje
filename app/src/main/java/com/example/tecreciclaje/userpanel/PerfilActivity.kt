package com.example.tecreciclaje.userpanel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tecreciclaje.LoginActivity
import com.example.tecreciclaje.R
import com.example.tecreciclaje.utils.CircleTransform
import com.example.tecreciclaje.utils.FCMTokenManager
import com.example.tecreciclaje.utils.TutorialManager
import com.example.tecreciclaje.Model.UpdateNfcBottomSheetDialogFragment
import com.example.tecreciclaje.UserPanelDynamic
import com.example.tecreciclaje.utils.AppLogger
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso

class PerfilActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var userRef: DatabaseReference

    private lateinit var textViewUserName: TextView
    private lateinit var textViewUserEmail: TextView
    private lateinit var textViewUserCarrera: TextView
    private lateinit var textViewUserNumControl: TextView
    private lateinit var textViewUserEdad: TextView
    private lateinit var imageViewProfile2: ImageView
    private lateinit var optionEditProfile: View
    private lateinit var optionUpdateNfc: View
    private lateinit var optionVerNip: View
    private lateinit var textViewNip: TextView
    private lateinit var optionVerLogros: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        auth = FirebaseAuth.getInstance()
        initializeViews()
        setupNavigation()
        setupClickListeners()

        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        
        // MOSTRAR TUTORIAL SI ES NECESARIO
        TutorialManager.showPerfilTutorialIfNeeded(this)

        // VALIDAR TOKEN FCM AL ABRIR PERFIL
        FCMTokenManager.validateCurrentToken()

        userRef = FirebaseDatabase.getInstance().getReference("usuarios").child(currentUser.uid)
        loadUserData()
    }

    private fun initializeViews() {
        textViewUserName = findViewById(R.id.textViewUserName)
        textViewUserEmail = findViewById(R.id.textViewUserEmail)
        textViewUserCarrera = findViewById(R.id.textViewUserCarrera)
        textViewUserNumControl = findViewById(R.id.textViewUserNumControl)
        textViewUserEdad = findViewById(R.id.textViewUserEdad)
        imageViewProfile2 = findViewById(R.id.imageViewProfile2)
        
        optionEditProfile = findViewById(R.id.optionEditProfile)
        optionUpdateNfc = findViewById(R.id.optionUpdateNfc)
        optionVerNip = findViewById(R.id.optionVerNip)
        textViewNip = findViewById(R.id.textViewNip)
        optionVerLogros = findViewById(R.id.optionVerLogros)
    }

    private fun setupNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.nav_perfil

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
                else -> false
            }
        }
    }

    private fun setupClickListeners() {
        // Opción: Editar Perfil
        optionEditProfile.setOnClickListener {
            val intent = Intent(this, EditarPerfilActivity::class.java)
            startActivity(intent)
        }

        // Opción: Actualizar NFC
        optionUpdateNfc.setOnClickListener {
            val dialog = UpdateNfcBottomSheetDialogFragment()
            dialog.show(supportFragmentManager, "UpdateNfcDialog")
        }

        // Opción: Ver NIP (copiar al portapapeles)
        optionVerNip.setOnClickListener {
            val nip = textViewNip.text.toString().replace("NIP: ", "")
            if (nip.isNotEmpty() && nip != "--------") {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("NIP", nip)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "NIP copiado al portapapeles", Toast.LENGTH_SHORT).show()
            }
        }

        // Opción: Ver Logros
        optionVerLogros.setOnClickListener {
            val intent = Intent(this, LogrosActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadUserData() {
        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Cargar información del usuario
                    val nombre = snapshot.child("usuario_nombre").getValue(String::class.java)
                    val apellido = snapshot.child("usuario_apellido").getValue(String::class.java)
                    val email = snapshot.child("usuario_email").getValue(String::class.java)
                    val imageUrl = snapshot.child("usuario_perfil").getValue(String::class.java)
                    val carrera = snapshot.child("usuario_carrera").getValue(String::class.java)
                    val numControl = snapshot.child("usuario_numControl").getValue(String::class.java)
                    val edad = snapshot.child("usuario_edad").getValue(String::class.java)
                    val nfcUid = snapshot.child("usuario_nfcUid").getValue(String::class.java) ?: ""
                    val nip = snapshot.child("usuario_nip").getValue(String::class.java) ?: ""

                    // Actualizar UI
                    val nombreCompleto = "${nombre ?: ""} ${apellido ?: ""}".trim()
                    textViewUserName.text = if (nombreCompleto.isEmpty()) "Usuario" else nombreCompleto
                    textViewUserEmail.text = email ?: "usuario@email.com"
                    
                    // Actualizar nuevos campos
                    textViewUserCarrera.text = "Carrera: ${carrera ?: "No especificada"}"
                    textViewUserNumControl.text = "Número de Control: ${numControl ?: "No especificado"}"
                    textViewUserEdad.text = "Edad: ${if (edad != null) "$edad años" else "No especificada"}"

                    // Mostrar/ocultar módulos según si tiene NFC o NIP
                    if (nfcUid.isEmpty() && nip.isNotEmpty()) {
                        // Usuario sin NFC pero con NIP - mostrar módulo NIP
                        optionVerNip.visibility = View.VISIBLE
                        optionUpdateNfc.visibility = View.GONE
                        textViewNip.text = "NIP: $nip"
                    } else if (nfcUid.isNotEmpty()) {
                        // Usuario con NFC - mostrar módulo NFC, ocultar NIP
                        optionVerNip.visibility = View.GONE
                        optionUpdateNfc.visibility = View.VISIBLE
                    } else {
                        // Usuario sin NFC ni NIP - ocultar ambos
                        optionVerNip.visibility = View.GONE
                        optionUpdateNfc.visibility = View.GONE
                    }

                    // Cargar imagen de perfil
                    if (!imageUrl.isNullOrEmpty()) {
                        Picasso.get()
                            .load(imageUrl)
                            .transform(CircleTransform())
                            .placeholder(R.drawable.user)
                            .error(R.drawable.user)
                            .into(imageViewProfile2)
                    } else {
                        imageViewProfile2.setImageResource(R.drawable.user)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                AppLogger.e("Error al cargar datos del perfil: ${error.message}")
            }
        })
    }
}
