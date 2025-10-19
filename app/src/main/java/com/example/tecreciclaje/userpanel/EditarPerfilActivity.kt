package com.example.tecreciclaje.userpanel

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.example.tecreciclaje.R
import com.example.tecreciclaje.utils.CircleTransform
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.AdListener
import android.util.Log
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import java.util.*

class EditarPerfilActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var userRef: DatabaseReference
    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference

    private lateinit var btnGuardar: Button
    private lateinit var btnSelectImage: Button
    private lateinit var btnCancelar: Button
    private lateinit var editTextNombre: EditText
    private lateinit var editTextApellido: EditText
    private lateinit var editTextNumcontrol: EditText
    private lateinit var editTextCorreo: EditText
    private lateinit var editTextEdad: EditText
    private lateinit var editTextCarrera: AutoCompleteTextView
    private lateinit var imageViewProfile: ImageView
    private lateinit var textViewTitle: TextView
    private lateinit var adViewContainer: LinearLayout

    private var imageUri: Uri? = null
    private var imageUrlActual: String? = null
    private lateinit var loadingDialog: Dialog
    private var adView: AdView? = null

    companion object {
        // Use test ad unit ID for development
        private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111" // Test banner ID
        // private const val AD_UNIT_ID = "ca-app-pub-5372308920488383/2713952653" // Your real ID
    }

    private val cropImage: ActivityResultLauncher<CropImageContractOptions> =
        registerForActivityResult(CropImageContract()) { result ->
            if (result.isSuccessful) {
                val uriContent = result.uriContent
                imageUri = uriContent
                imageViewProfile.setImageURI(imageUri)
            } else {
                Toast.makeText(this, "Error al recortar imagen.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_perfil)

        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()
        storageRef = storage.reference

        initializeViews()
        setupCarreraDropdown()
        setupLoadingDialog()

        val currentUser = auth.currentUser
        if (currentUser == null) {
            finish()
            return
        }

        userRef = FirebaseDatabase.getInstance().getReference("usuarios").child(currentUser.uid)
        loadUserData()
        setupClickListeners()
        
        // Initialize MobileAds SDK
        MobileAds.initialize(this) { initializationStatus ->
            Log.d("AdMob", "MobileAds initialized")
            initializeAdMob()
        }
    }

    private fun initializeViews() {
        textViewTitle = findViewById(R.id.textViewTitle)
        imageViewProfile = findViewById(R.id.imageViewProfile)
        editTextNombre = findViewById(R.id.editTextNombre)
        editTextApellido = findViewById(R.id.editTextApellido)
        editTextNumcontrol = findViewById(R.id.editTextNumcontrol)
        editTextCorreo = findViewById(R.id.editTextCorreo)
        editTextEdad = findViewById(R.id.editTextEdad)
        editTextCarrera = findViewById(R.id.editTextCarrera)
        btnGuardar = findViewById(R.id.btnGuardar)
        btnSelectImage = findViewById(R.id.btnSelectImage)
        btnCancelar = findViewById(R.id.btnCancelar)
        adViewContainer = findViewById(R.id.adViewContainer)

        textViewTitle.text = "Editar Perfil"
    }

    private fun setupCarreraDropdown() {
        val carreras = arrayOf(
            "Ingeniería en Desarrollo Comunitario",
            "Ingeniería Forestal",
            "Ingeniería en Tecnologías de la Información y Comunicaciones",
            "Ingeniería en Innovación Agrícola Sustentable",
            "Ingeniería Administracion"
        )

        // Crear un adapter personalizado para mejor visualización
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, carreras)
        
        editTextCarrera.setAdapter(adapter)
        editTextCarrera.threshold = 1 // Mostrar dropdown con 1 carácter
        
        // Configurar para que muestre el dropdown al hacer clic
        editTextCarrera.setOnClickListener {
            editTextCarrera.showDropDown()
        }
        
        // Configurar para que muestre el dropdown al obtener el foco
        editTextCarrera.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                editTextCarrera.showDropDown()
            }
        }
        
        // Configurar para que muestre el dropdown al tocar
        editTextCarrera.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                editTextCarrera.showDropDown()
                true
            } else {
                false
            }
        }
        
        // Configurar el icono final para que también abra el dropdown
        val textInputLayout = findViewById<TextInputLayout>(R.id.textInputLayoutCarrera)
        textInputLayout?.setEndIconOnClickListener {
            editTextCarrera.showDropDown()
        }
    }

    private fun setupLoadingDialog() {
        loadingDialog = Dialog(this)
        loadingDialog.setContentView(R.layout.dialog_loading)
        loadingDialog.setCancelable(false)
    }

    private fun loadUserData() {
        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Cargar información personal
                    val nombre = snapshot.child("usuario_nombre").getValue(String::class.java)
                    val apellido = snapshot.child("usuario_apellido").getValue(String::class.java)
                    val numControl = snapshot.child("usuario_numControl").getValue(String::class.java)
                    val carrera = snapshot.child("usuario_carrera").getValue(String::class.java)
                    val email = snapshot.child("usuario_email").getValue(String::class.java)
                    val edad = snapshot.child("usuario_edad").getValue(String::class.java)
                    val imageUrl = snapshot.child("usuario_perfil").getValue(String::class.java)

                    editTextNombre.setText(nombre ?: "")
                    editTextApellido.setText(apellido ?: "")
                    editTextNumcontrol.setText(numControl ?: "")
                    editTextCarrera.setText(carrera ?: "")
                    editTextCorreo.setText(email ?: "")
                    editTextEdad.setText(edad ?: "")

                    // Cargar imagen de perfil
                    if (!imageUrl.isNullOrEmpty()) {
                        imageUrlActual = imageUrl
                        Picasso.get()
                            .load(imageUrl)
                            .transform(CircleTransform())
                            .placeholder(R.drawable.user)
                            .error(R.drawable.user)
                            .into(imageViewProfile)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@EditarPerfilActivity, "Error al cargar datos: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupClickListeners() {
        btnSelectImage.setOnClickListener { selectImage() }
        btnGuardar.setOnClickListener { guardarCambios() }
        btnCancelar.setOnClickListener { finish() }
    }

    private fun selectImage() {
        val cropOptions = CropImageContractOptions(null, CropImageOptions())
            .setGuidelines(CropImageView.Guidelines.ON)
            .setRequestedSize(400, 400)
            .setCropShape(CropImageView.CropShape.OVAL)
            .setAspectRatio(1, 1)

        cropImage.launch(cropOptions)
    }

    private fun guardarCambios() {
        val nombre = editTextNombre.text.toString().trim()
        val apellido = editTextApellido.text.toString().trim()
        val numControl = editTextNumcontrol.text.toString().trim()
        val carrera = editTextCarrera.text.toString().trim()
        val email = editTextCorreo.text.toString().trim()
        val edad = editTextEdad.text.toString().trim()

        // Validaciones básicas
        if (nombre.isEmpty() || apellido.isEmpty() || numControl.isEmpty() || carrera.isEmpty()) {
            Toast.makeText(this, "Por favor completa todos los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        loadingDialog.show()

        // Si hay una nueva imagen, subirla primero
        if (imageUri != null) {
            uploadImageAndSaveData(nombre, apellido, numControl, carrera, email, edad)
        } else {
            // Solo guardar datos sin cambiar imagen
            saveUserData(nombre, apellido, numControl, carrera, email, edad, imageUrlActual)
        }
    }

    private fun uploadImageAndSaveData(nombre: String, apellido: String, numControl: String, carrera: String, email: String, edad: String) {
        val imageFileName = "profile_images/${UUID.randomUUID()}.jpg"
        val imageRef = storageRef.child(imageFileName)

        imageRef.putFile(imageUri!!)
            .addOnSuccessListener { taskSnapshot ->
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    val newImageUrl = uri.toString()
                    saveUserData(nombre, apellido, numControl, carrera, email, edad, newImageUrl)
                }
            }
            .addOnFailureListener { e ->
                loadingDialog.dismiss()
                Toast.makeText(this, "Error al subir imagen: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveUserData(nombre: String, apellido: String, numControl: String, carrera: String, email: String, edad: String, imageUrl: String?) {
        val updates = hashMapOf<String, Any>(
            "usuario_nombre" to nombre,
            "usuario_apellido" to apellido,
            "usuario_numControl" to numControl,
            "usuario_carrera" to carrera,
            "usuario_email" to email,
            "usuario_edad" to edad
        )
        
        if (imageUrl != null) {
            updates["usuario_perfil"] = imageUrl
        }

        userRef.updateChildren(updates)
            .addOnSuccessListener {
                loadingDialog.dismiss()
                Toast.makeText(this, "Perfil actualizado exitosamente", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                loadingDialog.dismiss()
                Toast.makeText(this, "Error al actualizar perfil: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun initializeAdMob() {
        try {
            Log.d("AdMob", "Initializing AdMob banner")
            
            // Create a new ad view.
            adView = AdView(this)
            adView?.adUnitId = AD_UNIT_ID
            // Request an anchored adaptive banner with a width of 360.
            adView?.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, 360))

            // Add AdListener for debugging
            adView?.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d("AdMob", "✅ Ad loaded successfully with ID: $AD_UNIT_ID")
                }
                
                override fun onAdFailedToLoad(adError: com.google.android.gms.ads.LoadAdError) {
                    Log.e("AdMob", "❌ Ad failed to load:")
                    Log.e("AdMob", "   Error Code: ${adError.code}")
                    Log.e("AdMob", "   Error Message: ${adError.message}")
                    Log.e("AdMob", "   Ad Unit ID: $AD_UNIT_ID")
                    Log.e("AdMob", "   Domain: ${adError.domain}")
                }
                
                override fun onAdOpened() {
                    Log.d("AdMob", "Ad opened")
                }
                
                override fun onAdClosed() {
                    Log.d("AdMob", "Ad closed")
                }
            }

            // Replace ad container with new ad view.
            adViewContainer.removeAllViews()
            adView?.let { adView ->
                adViewContainer.addView(adView)
                
                // Make sure the container is visible
                adViewContainer.visibility = View.VISIBLE
                Log.d("AdMob", "Ad container visibility: ${adViewContainer.visibility}")
                
                // Create an ad request
                val adRequest = AdRequest.Builder().build()
                
                Log.d("AdMob", "Loading ad with unit ID: $AD_UNIT_ID")
                // Start loading the ad.
                adView.loadAd(adRequest)
            }
        } catch (e: Exception) {
            Log.e("AdMob", "Error initializing AdMob: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        adView?.resume()
    }

    override fun onPause() {
        super.onPause()
        adView?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        adView?.destroy()
    }
}
