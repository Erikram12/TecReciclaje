package com.example.tecreciclaje.Model

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.tecreciclaje.R
import com.example.tecreciclaje.userpanel.MisValesActivity
import com.example.tecreciclaje.utils.FluentIconsHelper
import com.example.tecreciclaje.domain.model.Vale
import com.google.firebase.Timestamp
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.*

class ValesAdapter(private val vales: List<Vale>, private val context: Context) : 
    RecyclerView.Adapter<ValesAdapter.ValeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ValeViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_vale, parent, false)
        return ValeViewHolder(view)
    }

    override fun onBindViewHolder(holder: ValeViewHolder, position: Int) {
        val vale = vales[position]
        
        // Configurar nombre del producto - usar producto si está disponible, sino descripción
        val nombreProducto = vale.vale_producto.ifEmpty { vale.vale_descripcion }
        holder.txtProducto.text = nombreProducto
        
        // Configurar estado con indicador visual
        val estado = vale.vale_estado
        holder.txtEstado.text = estado
        
        // Configurar badge de estado y colores
        when (estado.lowercase()) {
            "disponible", "válido" -> {
                holder.badgeEstado.text = "VÁLIDO"
                holder.badgeEstado.setBackgroundResource(R.drawable.circle_light_bg)
                holder.badgeEstado.setTextColor(ContextCompat.getColor(context, R.color.verde_principal))
                holder.indicadorEstado.setBackgroundResource(R.drawable.circle_background)
            }
            "expirado", "caducado" -> {
                // ESTANDARIZADO: Mostrar "EXPIRADO" para ambos estados
                holder.badgeEstado.text = "EXPIRADO"
                holder.badgeEstado.setBackgroundResource(R.drawable.circle_light_bg)
                holder.badgeEstado.setTextColor(ContextCompat.getColor(context, R.color.orange1))
                holder.indicadorEstado.setBackgroundColor(ContextCompat.getColor(context, R.color.orange1))
            }
            "canjeado" -> {
                holder.badgeEstado.text = "CANJEADO"
                holder.badgeEstado.setBackgroundResource(R.drawable.circle_light_bg)
                holder.badgeEstado.setTextColor(ContextCompat.getColor(context, R.color.dark_gray))
                holder.indicadorEstado.setBackgroundColor(ContextCompat.getColor(context, R.color.dark_gray))
            }
        }

        // NUEVO: Configurar imagen del producto usando Fluent System Icons
        configurarImagenProducto(holder.imgVale, vale)

        // Configurar fecha de expiración
        val fechaExpiracion = vale.vale_fecha_expiracion
        if (fechaExpiracion != null) {
            try {
                val sdf = SimpleDateFormat("dd/MM/yyyy")
                val fechaFormateada = when (fechaExpiracion) {
                    is Timestamp -> sdf.format(fechaExpiracion.toDate())
                    is Long -> sdf.format(Date(fechaExpiracion))
                    else -> fechaExpiracion.toString()
                }
                
                holder.txtExpiracion.text = "Expira en: $fechaFormateada"
            } catch (e: Exception) {
                holder.txtExpiracion.text = "Expira en: Fecha no disponible"
            }
        } else {
            holder.txtExpiracion.text = "Expira en: Sin fecha de expiración"
        }

        // Configurar click listener
        holder.itemView.setOnClickListener {
            when (estado.lowercase()) {
                "disponible", "válido" -> {
                    if (context is MisValesActivity) {
                        context.mostrarQrDialog(vale.vale_id)
                    }
                }
                else -> {
                    // MEJORADO: Mensaje más específico para vales expirados
                    val mensaje = if (estado.lowercase() in listOf("expirado", "caducado")) 
                        "Este vale ha expirado y ya no está disponible" 
                    else 
                        "Este vale ya no está disponible"
                    Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * MEJORADA: Configura la imagen del producto con prioridad a URL, fallback a iconos
     */
    private fun configurarImagenProducto(imageView: ImageView, vale: Vale) {
        // PRIORIDAD 1: Intentar cargar imagen desde URL si está disponible
        if (!vale.vale_imagen_url.isNullOrEmpty()) {
            try {
                Picasso.get()
                    .load(vale.vale_imagen_url)
                    .placeholder(R.drawable.cafesito1)
                    .error(R.drawable.cafesito1)
                    .into(imageView)
                return // Salir si se cargó exitosamente
            } catch (e: Exception) {
                println("DEBUG: Error cargando imagen desde URL: ${e.message}")
                // Continuar con fallback si falla la carga de URL
            }
        }
        
        // PRIORIDAD 2: Usar iconos basados en el tipo de producto
        val producto = (vale.vale_producto ?: vale.vale_descripcion ?: "").lowercase()
        
        val iconName = when {
            producto.contains("café") || producto.contains("cafe") || producto.contains("taza") -> 
                FluentIconsHelper.Icons.COFFEE
            producto.contains("comida") || producto.contains("desayuno") || producto.contains("almuerzo") -> 
                FluentIconsHelper.Icons.FOOD
            producto.contains("refresco") || producto.contains("bebida") || producto.contains("jugo") -> 
                FluentIconsHelper.Icons.DRINK
            else -> FluentIconsHelper.Icons.FOOD // Fallback a icono genérico
        }
        
        // Intentar usar Fluent System Icons
        try {
            val resourceId = FluentIconsHelper.getFluentIconResId(context, iconName)
            if (resourceId != 0) {
                imageView.setImageResource(resourceId)
            } else {
                // Fallback a icono por defecto
                imageView.setImageResource(R.drawable.cafesito1)
            }
        } catch (e: Exception) {
            // Fallback en caso de error
            imageView.setImageResource(R.drawable.cafesito1)
        }
    }

    override fun getItemCount(): Int = vales.size

    class ValeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtProducto: TextView = itemView.findViewById(R.id.txtProducto)
        val txtEstado: TextView = itemView.findViewById(R.id.txtEstado)
        val txtExpiracion: TextView = itemView.findViewById(R.id.txtExpiracion)
        val imgVale: ImageView = itemView.findViewById(R.id.imgVale)
        val badgeEstado: TextView = itemView.findViewById(R.id.badgeEstado)
        val indicadorEstado: View = itemView.findViewById(R.id.indicadorEstado)
    }
}
