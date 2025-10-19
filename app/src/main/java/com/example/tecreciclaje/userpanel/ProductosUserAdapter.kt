package com.example.tecreciclaje.userpanel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tecreciclaje.R
import com.example.tecreciclaje.domain.model.Producto

class ProductosUserAdapter(
    private val onProductClick: (Producto) -> Unit
) : RecyclerView.Adapter<ProductosUserAdapter.ProductoViewHolder>() {
    
    private var productos = listOf<Producto>()
    
    fun updateProductos(newProductos: List<Producto>) {
        productos = newProductos
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto_user, parent, false)
        return ProductoViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        holder.bind(productos[position])
    }
    
    override fun getItemCount() = productos.size
    
    inner class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivImagen: ImageView = itemView.findViewById(R.id.ivImagenProducto)
        private val tvNombre: TextView = itemView.findViewById(R.id.tvNombreProducto)
        private val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcionProducto)
        private val tvPrecioPuntos: TextView = itemView.findViewById(R.id.tvPrecioPuntos)
        
        fun bind(producto: Producto) {
            tvNombre.text = producto.nombre
            tvDescripcion.text = producto.descripcion
            tvPrecioPuntos.text = "${producto.precioPuntos} pts"
            
            // Cargar imagen con Glide
            if (producto.imagenUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(producto.imagenUrl)
                    .placeholder(R.drawable.placeholder_producto)
                    .error(R.drawable.placeholder_producto)
                    .into(ivImagen)
            } else {
                ivImagen.setImageResource(R.drawable.placeholder_producto)
            }
            
            itemView.setOnClickListener {
                onProductClick(producto)
            }
        }
    }
}
