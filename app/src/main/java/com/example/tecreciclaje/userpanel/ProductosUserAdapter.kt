package com.example.tecreciclaje.userpanel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.tecreciclaje.R
import com.example.tecreciclaje.domain.model.Producto

class ProductosUserAdapter(
    private val onProductClick: (Producto) -> Unit
) : RecyclerView.Adapter<ProductosUserAdapter.ProductoViewHolder>() {

    private var productos = listOf<Producto>()

    // Opciones de Glide reutilizables para mejor rendimiento
    private val glideOptions = RequestOptions()
        .diskCacheStrategy(DiskCacheStrategy.ALL) // Cachea tanto la imagen original como las transformadas
        .placeholder(R.drawable.placeholder_producto)
        .error(R.drawable.placeholder_producto)
        .centerCrop() // Mejora la presentación
        .override(500, 500) // Limita el tamaño para optimizar memoria

    /**
     * Actualiza la lista de productos usando DiffUtil para animaciones suaves
     * y actualizaciones eficientes
     */
    fun updateProductos(newProductos: List<Producto>) {
        val diffCallback = ProductoDiffCallback(productos, newProductos)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        productos = newProductos
        diffResult.dispatchUpdatesTo(this)
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

    /**
     * Limpia recursos de Glide cuando el ViewHolder es reciclado
     */
    override fun onViewRecycled(holder: ProductoViewHolder) {
        super.onViewRecycled(holder)
        holder.clearImage()
    }

    inner class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivImagen: ImageView = itemView.findViewById(R.id.ivImagenProducto)
        private val tvNombre: TextView = itemView.findViewById(R.id.tvNombreProducto)
        private val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcionProducto)
        private val tvPrecioPuntos: TextView = itemView.findViewById(R.id.tvPrecioPuntos)

        fun bind(producto: Producto) {
            tvNombre.text = producto.nombre
            tvDescripcion.text = producto.descripcion
            tvPrecioPuntos.text = "${producto.precioPuntos} pts"

            // Cargar imagen optimizada con Glide
            if (producto.imagenUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(producto.imagenUrl)
                    .apply(glideOptions)
                    .thumbnail(0.1f) // Carga una miniatura al 10% primero
                    .into(ivImagen)
            } else {
                // Cancelar cualquier carga pendiente y establecer placeholder
                Glide.with(itemView.context).clear(ivImagen)
                ivImagen.setImageResource(R.drawable.placeholder_producto)
            }

            itemView.setOnClickListener {
                onProductClick(producto)
            }
        }

        /**
         * Limpia la imagen cuando el ViewHolder es reciclado
         */
        fun clearImage() {
            Glide.with(itemView.context).clear(ivImagen)
        }
    }

    /**
     * DiffUtil Callback para calcular diferencias entre listas de productos
     * Esto permite actualizaciones eficientes y animaciones suaves
     */
    private class ProductoDiffCallback(
        private val oldList: List<Producto>,
        private val newList: List<Producto>
    ) : DiffUtil.Callback() {

        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}