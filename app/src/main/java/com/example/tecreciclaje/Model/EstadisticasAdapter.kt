package com.example.tecreciclaje.Model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.tecreciclaje.R
import com.example.tecreciclaje.domain.model.Estadisticas
import java.util.*

class EstadisticasAdapter(private var estadisticasList: List<Estadisticas>) : 
    RecyclerView.Adapter<EstadisticasAdapter.EstadisticasViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EstadisticasViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_estadisticas, parent, false)
        return EstadisticasViewHolder(view)
    }

    override fun onBindViewHolder(holder: EstadisticasViewHolder, position: Int) {
        val estadistica = estadisticasList[position]
        
        holder.tvFecha.text = estadistica.fecha
        holder.tvPlasticos.text = estadistica.plasticos.toString()
        holder.tvAluminios.text = estadistica.aluminios.toString()
        holder.tvGanancias.text = String.format(Locale.getDefault(), "$%.2f", estadistica.ganancias)
        
        // Alternar colores de fondo para mejor legibilidad
        if (position % 2 == 0) {
            holder.itemView.setBackgroundResource(R.drawable.white_box)
        } else {
            holder.itemView.setBackgroundResource(R.drawable.circle_light_bg)
        }
        
        // Aplicar colores específicos según el tipo de dato
        // Las ganancias se destacan con un color más llamativo
        if (estadistica.ganancias > 0) {
            holder.tvGanancias.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.verde_principal))
        } else {
            holder.tvGanancias.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.dark_gray))
        }
    }

    override fun getItemCount(): Int = estadisticasList.size

    fun updateData(newData: List<Estadisticas>) {
        this.estadisticasList = newData
        notifyDataSetChanged()
    }

    class EstadisticasViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFecha: TextView = itemView.findViewById(R.id.tvFecha)
        val tvPlasticos: TextView = itemView.findViewById(R.id.tvPlasticos)
        val tvAluminios: TextView = itemView.findViewById(R.id.tvAluminios)
        val tvGanancias: TextView = itemView.findViewById(R.id.tvGanancias)
    }
}
