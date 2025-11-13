package com.example.tecreciclaje.adminpanel

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import com.example.tecreciclaje.utils.AppLogger
import android.widget.*
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.example.tecreciclaje.R
import com.example.tecreciclaje.domain.model.Producto
import com.example.tecreciclaje.domain.repository.ProductoRepositoryImpl
import com.example.tecreciclaje.utils.CustomAlertDialog
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.util.*

class GestionProductosActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProductosAdapter
    private lateinit var fabAgregar: FloatingActionButton
    private lateinit var productoRepository: ProductoRepositoryImpl
    private var imageUri: Uri? = null
    private val storageRef: StorageReference = FirebaseStorage.getInstance().reference

    // Referencia al ImageView del diálogo (para previsualizar la imagen seleccionada)
    private var ivImagenDialog: ImageView? = null

    private var loadingDialog: AlertDialog? = null

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            // En lugar de usar la imagen directamente, abrimos el crop
            startCrop(uri)
        } else {
            AppLogger.d("No se seleccionó ninguna imagen")
        }
    }

    private val cropImage = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            // Obtener la URI de la imagen recortada
            val croppedUri = result.uriContent
            imageUri = croppedUri
            AppLogger.d("Imagen recortada correctamente")

            // Previsualizar la imagen recortada en el diálogo (circular)
            ivImagenDialog?.let {
                Glide.with(this)
                    .load(croppedUri)
                    .transform(CenterCrop(), CircleCrop())
                    .placeholder(R.drawable.placeholder_producto)
                    .into(it)
            }
        } else {
            // Error al recortar
            val exception = result.error
            AppLogger.e("Error al recortar imagen", exception)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gestion_productos)

        productoRepository = ProductoRepositoryImpl()
        setupViews()
        setupRecyclerView()
        setupNavigation()
        loadProductos()
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.recyclerViewProductos)
        fabAgregar = findViewById(R.id.fabAgregarProducto)

        fabAgregar.setOnClickListener {
            showProductoDialog()
        }
    }

    private fun setupRecyclerView() {
        adapter = ProductosAdapter(
            onEditClick = { producto -> showProductoDialog(producto) },
            onDeleteClick = { producto -> confirmarEliminarProducto(producto) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.nav_productos

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, com.example.tecreciclaje.AdminPanel::class.java))
                    true
                }
                R.id.nav_scan -> {
                    startActivity(Intent(this, EscanearQrActivity::class.java))
                    true
                }
                R.id.nav_stats -> {
                    startActivity(Intent(this, EstadisticasActivity::class.java))
                    true
                }
                R.id.nav_contenedores -> {
                    startActivity(Intent(this, EstadoContenedoresActivity::class.java))
                    true
                }
                R.id.nav_productos -> true
                else -> false
            }
        }
    }

    private fun loadProductos() {
        lifecycleScope.launch {
            productoRepository.obtenerProductos().collect { productos ->
                adapter.updateProductos(productos)
            }
        }
    }


    private fun startCrop(sourceUri: Uri) {
        val cropOptions = CropImageOptions().apply {
            guidelines = CropImageView.Guidelines.ON
            aspectRatioX = 1 // ✅ Aspecto cuadrado 1:1
            aspectRatioY = 1
            fixAspectRatio = true // Mantener aspecto fijo
            cropShape = CropImageView.CropShape.OVAL // ✅ Recorte circular
            maxZoom = 4
            autoZoomEnabled = true
            multiTouchEnabled = true
            centerMoveEnabled = true
            showCropOverlay = true
            showProgressBar = true
            cropMenuCropButtonTitle = "Recortar"
            outputCompressFormat = android.graphics.Bitmap.CompressFormat.JPEG
            outputCompressQuality = 90
            outputRequestWidth = 1080 // ✅ Tamaño máximo 1080x1080
            outputRequestHeight = 1080
        }

        val cropImageContractOptions = CropImageContractOptions(sourceUri, cropOptions)
        cropImage.launch(cropImageContractOptions)
    }

    private fun showProductoDialog(producto: Producto? = null) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_producto_form, null)

        val etNombre = dialogView.findViewById<EditText>(R.id.etNombre)
        val etDescripcion = dialogView.findViewById<EditText>(R.id.etDescripcion)
        val etPrecioPuntos = dialogView.findViewById<EditText>(R.id.etPrecioPuntos)
        val ivImagen = dialogView.findViewById<ImageView>(R.id.ivImagenPreview)
        val btnSeleccionarImagen = dialogView.findViewById<Button>(R.id.btnSeleccionarImagen)
        val btnCancelar = dialogView.findViewById<Button>(R.id.btnCancelar)
        val btnGuardar = dialogView.findViewById<Button>(R.id.btnGuardar)

        // Guardamos referencia para previsualizar la imagen seleccionada
        ivImagenDialog = ivImagen

        // Si es edición, llenar los campos
        if (producto != null) {
            etNombre.setText(producto.nombre)
            etDescripcion.setText(producto.descripcion)
            etPrecioPuntos.setText(producto.precioPuntos.toString())

            if (producto.imagenUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(producto.imagenUrl)
                    .transform(CenterCrop(), CircleCrop())
                    .placeholder(R.drawable.placeholder_producto)
                    .into(ivImagen)
            }
        }

        btnSeleccionarImagen.setOnClickListener {
            pickImage.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()


        btnGuardar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val descripcion = etDescripcion.text.toString().trim()
            val precioText = etPrecioPuntos.text.toString().trim()

            if (validarCampos(nombre, descripcion, precioText)) {
                val precioPuntos = precioText.toInt()
                if (producto == null) {
                    crearProducto(nombre, descripcion, precioPuntos)
                } else {
                    actualizarProducto(producto, nombre, descripcion, precioPuntos)
                }
                dialog.dismiss()
            }
        }

        // ✅ Botón Cancelar
        btnCancelar.setOnClickListener {
            ivImagenDialog = null
            imageUri = null
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun validarCampos(nombre: String, descripcion: String, precio: String): Boolean {
        when {
            TextUtils.isEmpty(nombre) -> {
                AppLogger.e("El nombre es requerido")
                return false
            }
            TextUtils.isEmpty(descripcion) -> {
                AppLogger.e("La descripción es requerida")
                return false
            }
            TextUtils.isEmpty(precio) -> {
                AppLogger.e("El precio en puntos es requerido")
                return false
            }
            !precio.matches("\\d+".toRegex()) -> {
                AppLogger.e("El precio debe ser un número válido")
                return false
            }
            precio.toInt() <= 0 -> {
                AppLogger.e("El precio debe ser mayor a 0")
                return false
            }
        }
        return true
    }

    private fun showLoadingDialog(mensaje: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_loading_producto, null)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tvLoadingMessage)
        tvMessage.text = mensaje

        loadingDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        loadingDialog?.show()
    }

    private fun hideLoadingDialog() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    private fun crearProducto(nombre: String, descripcion: String, precioPuntos: Int) {
        lifecycleScope.launch {
            try {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    AppLogger.e("Usuario no autenticado")
                    return@launch
                }

                showLoadingDialog("Creando producto...")

                var imagenUrl = ""
                if (imageUri != null) {
                    imagenUrl = subirImagen(imageUri!!, nombre)
                }

                val nuevoProducto = Producto(
                    nombre = nombre,
                    descripcion = descripcion,
                    precioPuntos = precioPuntos,
                    imagenUrl = imagenUrl
                )

                val productoId = productoRepository.crearProducto(nuevoProducto)

                hideLoadingDialog()

                if (productoId.isNotEmpty()) {
                    AppLogger.d("Producto creado exitosamente")
                } else {
                    AppLogger.e("Error al crear el producto")
                }

                // Limpiar datos
                imageUri = null
                ivImagenDialog = null

            } catch (e: Exception) {
                hideLoadingDialog()
                AppLogger.e("Error creando producto", e)
            }
        }
    }

    private fun actualizarProducto(producto: Producto, nombre: String, descripcion: String, precioPuntos: Int) {
        lifecycleScope.launch {
            try {
                showLoadingDialog("Actualizando producto...")

                var imagenUrl = producto.imagenUrl
                if (imageUri != null) {
                    imagenUrl = subirImagen(imageUri!!, nombre)
                }

                val productoActualizado = producto.copy(
                    nombre = nombre,
                    descripcion = descripcion,
                    precioPuntos = precioPuntos,
                    imagenUrl = imagenUrl
                )

                val success = productoRepository.actualizarProducto(productoActualizado)

                hideLoadingDialog()

                if (success) {
                    AppLogger.d("Producto actualizado exitosamente")
                } else {
                    AppLogger.e("Error al actualizar el producto")
                }

                imageUri = null
                ivImagenDialog = null

            } catch (e: Exception) {
                hideLoadingDialog()
                AppLogger.e("Error actualizando producto", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hideLoadingDialog()
    }

    private suspend fun subirImagen(imageUri: Uri, nombreProducto: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val fileName = "productos/${UUID.randomUUID()}_${nombreProducto.replace(" ", "_")}.jpg"
                val imageRef = storageRef.child(fileName)
                imageRef.putFile(imageUri).await()
                imageRef.downloadUrl.await().toString()
            } catch (e: Exception) {
                AppLogger.e("Error subiendo imagen", e)
                ""
            }
        }
    }

    private fun confirmarEliminarProducto(producto: Producto) {
        CustomAlertDialog.createDeleteProductDialog(
            this,
            producto.nombre,
            {
                lifecycleScope.launch {
                    try {
                        val success = productoRepository.eliminarProducto(producto.id)
                        if (success) {
                            AppLogger.d("Producto eliminado exitosamente")
                        } else {
                            AppLogger.e("Error al eliminar el producto")
                        }
                    } catch (e: Exception) {
                        AppLogger.e("Error eliminando producto", e)
                    }
                }
            }
        ).show()
    }
}