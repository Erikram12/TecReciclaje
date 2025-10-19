package com.example.tecreciclaje.Model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.tecreciclaje.R
import com.example.tecreciclaje.domain.model.Logro

class LogrosAdapter(
    private var logros: List<Logro>,
    private val listener: OnLogroClickListener?
) : RecyclerView.Adapter<LogrosAdapter.LogroViewHolder>() {

    interface OnLogroClickListener {
        fun onLogroClick(logro: Logro)
        fun onReclamarClick(logro: Logro)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogroViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_logro, parent, false)
        return LogroViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogroViewHolder, position: Int) {
        val logro = logros[position]
        holder.bind(logro)
    }

    override fun getItemCount(): Int = logros.size

    fun updateLogros(newLogros: List<Logro>) {
        this.logros = newLogros
        notifyDataSetChanged()
    }

    inner class LogroViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageViewLogroIcon: ImageView = itemView.findViewById(R.id.imageViewLogroIcon)
        private val imageViewLogroEstado: ImageView = itemView.findViewById(R.id.imageViewLogroEstado)
        private val textViewLogroTitulo: TextView = itemView.findViewById(R.id.textViewLogroTitulo)
        private val textViewLogroDescripcion: TextView = itemView.findViewById(R.id.textViewLogroDescripcion)
        private val textViewLogroProgreso: TextView = itemView.findViewById(R.id.textViewLogroProgreso)
        private val textViewLogroRecompensa: TextView = itemView.findViewById(R.id.textViewLogroRecompensa)
        private val progressBarLogro: android.widget.ProgressBar = itemView.findViewById(R.id.progressBarLogro)
        private val buttonReclamar: android.widget.Button = itemView.findViewById(R.id.buttonReclamar)
        private val layoutBotonReclamar: View = itemView.findViewById(R.id.layoutBotonReclamar)
        private val layoutYaReclamado: View = itemView.findViewById(R.id.layoutYaReclamado)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener?.onLogroClick(logros[position])
                }
            }

            buttonReclamar.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener?.onReclamarClick(logros[position])
                }
            }
        }

        fun bind(logro: Logro) {
            textViewLogroTitulo.text = logro.titulo
            textViewLogroDescripcion.text = logro.descripcion
            textViewLogroProgreso.text = logro.textoProgreso
            textViewLogroRecompensa.text = "🏆 ${logro.recompensa} pts"

            // Configurar barra de progreso
            val porcentaje = logro.porcentajeCompletado.toInt()
            progressBarLogro.progress = porcentaje

            // Configurar icono según el tipo de logro
            val iconResource = getIconResource(logro.icono)
            imageViewLogroIcon.setImageResource(iconResource)

            // Configurar estado del logro y botones
            if (logro.desbloqueado) {
                imageViewLogroEstado.setImageResource(R.drawable.baseline_area_chart_24)
                imageViewLogroEstado.setColorFilter(ContextCompat.getColor(itemView.context, R.color.verde_principal))
                textViewLogroTitulo.setTextColor(ContextCompat.getColor(itemView.context, R.color.verde_principal))
                
                // Mostrar botón de reclamar o mensaje de ya reclamado
                if (logro.reclamado) {
                    layoutBotonReclamar.visibility = View.GONE
                    layoutYaReclamado.visibility = View.VISIBLE
                } else {
                    layoutBotonReclamar.visibility = View.VISIBLE
                    layoutYaReclamado.visibility = View.GONE
                }
            } else {
                imageViewLogroEstado.setImageResource(R.drawable.baseline_lock_24)
                imageViewLogroEstado.setColorFilter(ContextCompat.getColor(itemView.context, R.color.dark_gray))
                textViewLogroTitulo.setTextColor(ContextCompat.getColor(itemView.context, R.color.black))
                
                // Ocultar botones si no está desbloqueado
                layoutBotonReclamar.visibility = View.GONE
                layoutYaReclamado.visibility = View.GONE
            }
        }

        private fun getIconResource(icono: String): Int {
            return when (icono) {
                "reciclaje" -> R.drawable.baseline_area_chart_24
                "puntos" -> R.drawable.baseline_area_chart_24
                "dias_consecutivos" -> R.drawable.baseline_calendar_today_24
                "primer_reciclaje" -> R.drawable.baseline_area_chart_24
                "reciclador_experto" -> R.drawable.baseline_area_chart_24
                "reciclador_maestro" -> R.drawable.baseline_area_chart_24
                else -> R.drawable.baseline_area_chart_24
            }
        }
    }
}
