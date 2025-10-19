package com.example.tecreciclaje.domain.repository

import android.util.Log
import com.example.tecreciclaje.domain.model.Producto
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface ProductoRepository {
    fun obtenerProductos(): Flow<List<Producto>>
    suspend fun obtenerProductoPorId(id: String): Producto?
    suspend fun crearProducto(producto: Producto): String
    suspend fun actualizarProducto(producto: Producto): Boolean
    suspend fun eliminarProducto(id: String): Boolean
}

class ProductoRepositoryImpl : ProductoRepository {
    private val database = FirebaseDatabase.getInstance()
    private val productosRef = database.getReference("productos")

    override fun obtenerProductos(): Flow<List<Producto>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val productos = mutableListOf<Producto>()
                for (productoSnapshot in snapshot.children) {
                    productoSnapshot.getValue(Producto::class.java)?.let { producto ->
                        productos.add(producto.copy(id = productoSnapshot.key ?: ""))
                    }
                }
                trySend(productos.filter { it.activo })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ProductoRepository", "Error obteniendo productos: ${error.message}", error.toException())
                trySend(emptyList()) // Enviar lista vacía en caso de error
                close()
            }
        }
        
        productosRef.addValueEventListener(listener)
        
        awaitClose {
            productosRef.removeEventListener(listener)
        }
    }

    override suspend fun obtenerProductoPorId(id: String): Producto? {
        return try {
            val snapshot = productosRef.child(id).get().await()
            snapshot.getValue(Producto::class.java)?.copy(id = id)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun crearProducto(producto: Producto): String {
        val newProductoRef = productosRef.push()
        val productoConFecha = producto.copy(
            fechaCreacion = System.currentTimeMillis(),
            fechaActualizacion = System.currentTimeMillis()
        )
        newProductoRef.setValue(productoConFecha).await()
        return newProductoRef.key ?: ""
    }

    override suspend fun actualizarProducto(producto: Producto): Boolean {
        return try {
            val productoActualizado = producto.copy(fechaActualizacion = System.currentTimeMillis())
            productosRef.child(producto.id).setValue(productoActualizado).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun eliminarProducto(id: String): Boolean {
        return try {
            productosRef.child(id).removeValue().await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
