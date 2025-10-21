package com.example.tecreciclaje.adminpanel

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
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


    // ✅ Photo Picker sin permisos
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            imageUri = uri
            Toast.makeText(this, "Imagen seleccionada", Toast.LENGTH_SHORT).show()

            // Previsualizar la imagen seleccionada en el diálogo (circular)
            ivImagenDialog?.let {
                Glide.with(this)
                    .load(uri)
                    .transform(CenterCrop(), CircleCrop())
                    .placeholder(R.drawable.placeholder_producto)
                    .into(it)
            }
        } else {
            Toast.makeText(this, "No se seleccionó ninguna imagen", Toast.LENGTH_SHORT).show()
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

        // ✅ Abrir Photo Picker
        btnSeleccionarImagen.setOnClickListener {
            pickImage.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // ✅ Botón Guardar
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
                Toast.makeText(this, "El nombre es requerido", Toast.LENGTH_SHORT).show()
                return false
            }
            TextUtils.isEmpty(descripcion) -> {
                Toast.makeText(this, "La descripción es requerida", Toast.LENGTH_SHORT).show()
                return false
            }
            TextUtils.isEmpty(precio) -> {
                Toast.makeText(this, "El precio en puntos es requerido", Toast.LENGTH_SHORT).show()
                return false
            }
            !precio.matches("\\d+".toRegex()) -> {
                Toast.makeText(this, "El precio debe ser un número válido", Toast.LENGTH_SHORT).show()
                return false
            }
            precio.toInt() <= 0 -> {
                Toast.makeText(this, "El precio debe ser mayor a 0", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }

    // Agregar este método para mostrar el diálogo de carga
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

    // Agregar este método para ocultar el diálogo de carga
    private fun hideLoadingDialog() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    // Modificar el método crearProducto
    private fun crearProducto(nombre: String, descripcion: String, precioPuntos: Int) {
        lifecycleScope.launch {
            try {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    Toast.makeText(this@GestionProductosActivity, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // ✅ Mostrar diálogo de carga
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

                // ✅ Ocultar diálogo de carga
                hideLoadingDialog()

                if (productoId.isNotEmpty()) {
                    Toast.makeText(this@GestionProductosActivity, "Producto creado exitosamente", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@GestionProductosActivity, "Error al crear el producto", Toast.LENGTH_SHORT).show()
                }

                // Limpiar datos
                imageUri = null
                ivImagenDialog = null

            } catch (e: Exception) {
                // ✅ Ocultar diálogo de carga en caso de error
                hideLoadingDialog()

                Log.e("GestionProductos", "Error creando producto", e)
                Toast.makeText(this@GestionProductosActivity, "Error al crear el producto", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Modificar el método actualizarProducto
    private fun actualizarProducto(producto: Producto, nombre: String, descripcion: String, precioPuntos: Int) {
        lifecycleScope.launch {
            try {
                // ✅ Mostrar diálogo de carga
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

                // ✅ Ocultar diálogo de carga
                hideLoadingDialog()

                if (success) {
                    Toast.makeText(this@GestionProductosActivity, "Producto actualizado exitosamente", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@GestionProductosActivity, "Error al actualizar el producto", Toast.LENGTH_SHORT).show()
                }

                imageUri = null
                ivImagenDialog = null

            } catch (e: Exception) {
                // ✅ Ocultar diálogo de carga en caso de error
                hideLoadingDialog()

                Log.e("GestionProductos", "Error actualizando producto", e)
                Toast.makeText(this@GestionProductosActivity, "Error al actualizar el producto", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Agregar esto al final del onCreate para limpiar el diálogo si existe
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
                Log.e("GestionProductos", "Error subiendo imagen", e)
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
                            Toast.makeText(
                                this@GestionProductosActivity,
                                "Producto eliminado exitosamente",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this@GestionProductosActivity,
                                "Error al eliminar el producto",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        Log.e("GestionProductos", "Error eliminando producto", e)
                        Toast.makeText(
                            this@GestionProductosActivity,
                            "Error al eliminar el producto",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        ).show()
    }
}
