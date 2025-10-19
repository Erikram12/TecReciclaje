package com.example.tecreciclaje.Model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.tecreciclaje.R
import com.example.tecreciclaje.domain.model.Historial
import java.text.SimpleDateFormat
import java.util.*

class HistorialAdapter(private val historialList: List<Historial>) : 
    RecyclerView.Adapter<HistorialAdapter.HistorialViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistorialViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_historial, parent, false)
        return HistorialViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistorialViewHolder, position: Int) {
        val item = historialList[position]

        // CORREGIDO: Usar getters inteligentes que manejan campos normalizados
        val cantidad = item.getCantidad
        holder.txtCantidad.text = "${if (cantidad > 0) "+" else ""}$cantidad pts"

        // Fecha formateada
        val fecha = item.getFecha
        if (fecha != null) {
            val fechaFormateada = when (fecha) {
                is com.google.firebase.Timestamp -> {
                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        .format(fecha.toDate())
                }
                is Long -> {
                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        .format(Date(fecha))
                }
                else -> {
                    "Fecha no válida"
                }
            }
            holder.txtFecha.text = fechaFormateada
        } else {
            holder.txtFecha.text = "Fecha no disponible"
        }

        // CORREGIDO: Configurar icono y tipo según el tipo de transacción
        val tipo = item.getTipo
        when (tipo.lowercase()) {
            "canjeado" -> {
                holder.iconoHistorial.text = "🛒"
                holder.txtTipo.text = "Canjeado"
                holder.txtCantidad.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.orange1))
            }
            "ganado" -> {
                holder.iconoHistorial.text = "💰"
                holder.txtTipo.text = "Ganado"
                holder.txtCantidad.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.verde_principal))
            }
            "recompensa" -> {
                holder.iconoHistorial.text = "🏆"
                holder.txtTipo.text = "Recompensa"
                holder.txtCantidad.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.verde_principal))
            }
            else -> {
                holder.iconoHistorial.text = "❓"
                holder.txtTipo.text = "Desconocido"
                holder.txtCantidad.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.verde_principal))
            }
        }

        // CORREGIDO: Mostrar producto si es tipo canjeado
        if (tipo.lowercase() == "canjeado" && !item.getProducto.isNullOrEmpty()) {
            holder.txtProducto.visibility = View.VISIBLE
            holder.txtProducto.text = item.getProducto
        } else {
            holder.txtProducto.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = historialList.size

    class HistorialViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtCantidad: TextView = itemView.findViewById(R.id.txtCantidad)
        val txtFecha: TextView = itemView.findViewById(R.id.txtFecha)
        val txtProducto: TextView = itemView.findViewById(R.id.txtProducto)
        val txtTipo: TextView = itemView.findViewById(R.id.txtTipo)
        val iconoHistorial: TextView = itemView.findViewById(R.id.iconoHistorial)
    }
}
