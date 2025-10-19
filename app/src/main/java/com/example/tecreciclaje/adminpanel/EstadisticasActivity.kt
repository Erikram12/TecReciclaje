package com.example.tecreciclaje.adminpanel

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tecreciclaje.AdminPanel
import com.example.tecreciclaje.Model.EstadisticasAdapter
import com.example.tecreciclaje.R
import com.example.tecreciclaje.domain.model.Estadisticas
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class EstadisticasActivity : AppCompatActivity() {

    // Constantes
    companion object {
        private const val PUNTOS_PLASTICO = 20
        private const val PUNTOS_ALUMINIO = 30
        private const val RANGO_MIN_PLAST = 20
        private const val RANGO_MAX_PLAST = 29
        private const val RANGO_MIN_ALUM = 30
        private const val RANGO_MAX_ALUM = 39
        private const val PRECIO_POR_BOTELLA_PLASTICO = 0.5
        private const val PRECIO_POR_LATA_ALUMINIO = 1.0
        private const val ITEMS_POR_PAGINA = 5
    }

    // UI Components
    private lateinit var tvTotalPlasticos: TextView
    private lateinit var tvTotalAluminios: TextView
    private lateinit var tvTotalGanancias: TextView
    private lateinit var tvPromedioDiario: TextView
    private lateinit var tvMejorDia: TextView
    private lateinit var tvPeorDia: TextView
    private lateinit var tvTituloTabla: TextView
    private lateinit var barChart: BarChart
    private lateinit var recyclerViewEstadisticas: RecyclerView
    private lateinit var btnAnterior: Button
    private lateinit var btnSiguiente: Button
    private lateinit var btnVistaDia: Button
    private lateinit var btnVistaMes: Button
    private lateinit var tvPaginaInfo: TextView

    // Data
    private val estadisticasList = mutableListOf<Estadisticas>()
    private val todasLasEstadisticas = mutableListOf<Estadisticas>()
    private lateinit var adapter: EstadisticasAdapter
    private val dayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val dayOnlyFormat = SimpleDateFormat("dd", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    private val monthOnlyFormat = SimpleDateFormat("MMM", Locale.getDefault())
    
    // Vista actual
    private var vistaPorMes = false
    
    // Paginación
    private var paginaActual = 0
    private var totalPaginas = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_estadisticas)

        // Configurar toolbar
        supportActionBar?.apply {
            title = "Estadísticas Detalladas"
            setDisplayHomeAsUpEnabled(true)
        }

        // Inicializar UI
        initViews()
        setupRecyclerView()
        setupPaginacion()
        setupVistaSelector()
        configChart()
        cargarEstadisticas()
        setupNavigation()
    }

    private fun initViews() {
        tvTotalPlasticos = findViewById(R.id.tvTotalPlasticos)
        tvTotalAluminios = findViewById(R.id.tvTotalAluminios)
        tvTotalGanancias = findViewById(R.id.tvTotalGanancias)
        tvPromedioDiario = findViewById(R.id.tvPromedioDiario)
        tvMejorDia = findViewById(R.id.tvMejorDia)
        tvPeorDia = findViewById(R.id.tvPeorDia)
        tvTituloTabla = findViewById(R.id.tvTituloTabla)
        barChart = findViewById(R.id.barChartEstadisticas)
        recyclerViewEstadisticas = findViewById(R.id.recyclerViewEstadisticas)
        btnAnterior = findViewById(R.id.btnAnterior)
        btnSiguiente = findViewById(R.id.btnSiguiente)
        btnVistaDia = findViewById(R.id.btnVistaDia)
        btnVistaMes = findViewById(R.id.btnVistaMes)
        tvPaginaInfo = findViewById(R.id.tvPaginaInfo)
    }

    private fun setupRecyclerView() {
        adapter = EstadisticasAdapter(estadisticasList)
        recyclerViewEstadisticas.layoutManager = LinearLayoutManager(this)
        recyclerViewEstadisticas.adapter = adapter
    }

    private fun setupPaginacion() {
        btnAnterior.setOnClickListener {
            if (paginaActual > 0) {
                paginaActual--
                actualizarTablaPaginada()
            }
        }

        btnSiguiente.setOnClickListener {
            if (paginaActual < totalPaginas - 1) {
                paginaActual++
                actualizarTablaPaginada()
            }
        }
    }

    private fun setupVistaSelector() {
        btnVistaDia.setOnClickListener {
            if (vistaPorMes) {
                vistaPorMes = false
                actualizarBotonesVista()
                cargarEstadisticas()
            }
        }

        btnVistaMes.setOnClickListener {
            if (!vistaPorMes) {
                vistaPorMes = true
                actualizarBotonesVista()
                cargarEstadisticas()
            }
        }
    }

    private fun actualizarBotonesVista() {
        if (vistaPorMes) {
            btnVistaDia.backgroundTintList = null
            btnVistaDia.setTextColor(getColor(R.color.dark_gray))
            btnVistaMes.backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.verde_principal))
            btnVistaMes.setTextColor(getColor(R.color.white))
            tvTituloTabla.text = "📅 Ganancias por Mes"
        } else {
            btnVistaDia.backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.verde_principal))
            btnVistaDia.setTextColor(getColor(R.color.white))
            btnVistaMes.backgroundTintList = null
            btnVistaMes.setTextColor(getColor(R.color.dark_gray))
            tvTituloTabla.text = "📅 Ganancias por Día"
        }
    }

    private fun configChart() {
        barChart.description.isEnabled = false
        barChart.axisRight.isEnabled = false
        barChart.isHighlightFullBarEnabled = false
        barChart.setPinchZoom(false)
        barChart.setDoubleTapToZoomEnabled(false)
        barChart.setScaleEnabled(false)

        val legend = barChart.legend
        legend.isEnabled = true
        legend.textSize = 12f
        legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false)
        legend.textColor = getColor(R.color.dark_gray)

        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(true)
        xAxis.textSize = 10f
        xAxis.textColor = getColor(R.color.dark_gray)
        xAxis.granularity = 1f
        xAxis.isGranularityEnabled = true
    }

    private fun cargarEstadisticas() {
        val usuariosRef = FirebaseDatabase.getInstance().getReference("usuarios")
        
        usuariosRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val estadisticasMap = mutableMapOf<String, Estadisticas>()
                
                Log.d("EstadisticasActivity", "Cargando estadísticas de ${snapshot.childrenCount} usuarios")
                
                for (usuarioSnap in snapshot.children) {
                    val puntosRef = usuarioSnap.child("puntos")
                    
                    for (puntoSnap in puntosRef.children) {
                        val punto = puntoSnap.getValue(com.example.tecreciclaje.domain.model.Historial::class.java)
                        if (punto != null && punto.getTipo == "ganado") {
                            val fecha = punto.getFecha
                            if (fecha != null) {
                                val fechaDate = when (fecha) {
                                    is com.google.firebase.Timestamp -> fecha.toDate()
                                    is Long -> Date(fecha)
                                    else -> Date()
                                }
                                
                                val fechaKey = if (vistaPorMes) {
                                    monthFormat.format(fechaDate)
                                } else {
                                    dayFormat.format(fechaDate)
                                }
                                
                                val cantidad = punto.getCantidad
                                val esPlastico = cantidad in RANGO_MIN_PLAST..RANGO_MAX_PLAST
                                val esAluminio = cantidad in RANGO_MIN_ALUM..RANGO_MAX_ALUM
                                
                                val estadistica = estadisticasMap[fechaKey] ?: Estadisticas(
                                    fecha = fechaKey,
                                    plasticos = 0,
                                    aluminios = 0,
                                    ganancias = 0.0
                                )
                                
                                if (esPlastico) {
                                    estadistica.plasticos++
                                } else if (esAluminio) {
                                    estadistica.aluminios++
                                }
                                
                                estadistica.ganancias = (estadistica.plasticos * PRECIO_POR_BOTELLA_PLASTICO) + 
                                                     (estadistica.aluminios * PRECIO_POR_LATA_ALUMINIO)
                                
                                estadisticasMap[fechaKey] = estadistica
                                
                                Log.d("EstadisticasActivity", "Procesado: $fechaKey - plásticos: ${estadistica.plasticos}, aluminios: ${estadistica.aluminios}")
                            }
                        }
                    }
                }
                
                todasLasEstadisticas.clear()
                todasLasEstadisticas.addAll(estadisticasMap.values.sortedByDescending { 
                    if (vistaPorMes) {
                        monthFormat.parse(it.fecha)?.time ?: 0L
                    } else {
                        dayFormat.parse(it.fecha)?.time ?: 0L
                    }
                })
                
                Log.d("EstadisticasActivity", "Total estadísticas cargadas: ${todasLasEstadisticas.size}")
                actualizarResumen()
                actualizarTablaPaginada()
                actualizarGrafico()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("EstadisticasActivity", "Error al cargar estadísticas", error.toException())
            }
        })
    }

    private fun actualizarResumen() {
        val totalPlasticos = todasLasEstadisticas.sumOf { it.plasticos }
        val totalAluminios = todasLasEstadisticas.sumOf { it.aluminios }
        val totalGanancias = todasLasEstadisticas.sumOf { it.ganancias }
        
        tvTotalPlasticos.text = totalPlasticos.toString()
        tvTotalAluminios.text = totalAluminios.toString()
        tvTotalGanancias.text = "$${String.format("%.2f", totalGanancias)}"
        
        val promedioDiario = if (todasLasEstadisticas.isNotEmpty()) {
            totalGanancias / todasLasEstadisticas.size
        } else 0.0
        tvPromedioDiario.text = "$${String.format("%.2f", promedioDiario)}"
        
        val mejorDia = todasLasEstadisticas.maxByOrNull { it.ganancias }
        val peorDia = todasLasEstadisticas.minByOrNull { it.ganancias }
        
        tvMejorDia.text = mejorDia?.let { "${it.fecha}: $${String.format("%.2f", it.ganancias)}" } ?: "N/A"
        tvPeorDia.text = peorDia?.let { "${it.fecha}: $${String.format("%.2f", it.ganancias)}" } ?: "N/A"
    }

    private fun actualizarTablaPaginada() {
        totalPaginas = (todasLasEstadisticas.size + ITEMS_POR_PAGINA - 1) / ITEMS_POR_PAGINA
        
        val inicio = paginaActual * ITEMS_POR_PAGINA
        val fin = minOf(inicio + ITEMS_POR_PAGINA, todasLasEstadisticas.size)
        
        estadisticasList.clear()
        if (inicio < todasLasEstadisticas.size) {
            estadisticasList.addAll(todasLasEstadisticas.subList(inicio, fin))
        }
        
        adapter.notifyDataSetChanged()
        
        // Actualizar controles de paginación
        btnAnterior.isEnabled = paginaActual > 0
        btnSiguiente.isEnabled = paginaActual < totalPaginas - 1
        tvPaginaInfo.text = "Página ${paginaActual + 1} de $totalPaginas"
    }

    private fun actualizarGrafico() {
        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        
        val datosParaGrafico = if (vistaPorMes) {
            todasLasEstadisticas.takeLast(12) // Últimos 12 meses
        } else {
            todasLasEstadisticas.takeLast(7) // Últimos 7 días
        }
        
        datosParaGrafico.forEachIndexed { index, estadistica ->
            entries.add(BarEntry(index.toFloat(), estadistica.ganancias.toFloat()))
            labels.add(if (vistaPorMes) {
                monthOnlyFormat.format(monthFormat.parse(estadistica.fecha) ?: Date())
            } else {
                dayOnlyFormat.format(dayFormat.parse(estadistica.fecha) ?: Date())
            })
        }
        
        val dataSet = BarDataSet(entries, "Ganancias")
        dataSet.color = getColor(R.color.verde_principal)
        dataSet.valueTextSize = 10f
        dataSet.valueTextColor = getColor(R.color.dark_gray)
        
        val data = BarData(dataSet)
        barChart.data = data
        
        val xAxis = barChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.labelCount = labels.size
        xAxis.setCenterAxisLabels(false)
        
        barChart.invalidate()
    }

    private fun setupNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.nav_stats
        
        bottomNavigationView.setOnItemSelectedListener { item ->
            val itemId = item.itemId
            val currentClass = this::class.java

            when {
                itemId == R.id.nav_dashboard && currentClass != AdminPanel::class.java -> {
                    startActivity(Intent(this, AdminPanel::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                itemId == R.id.nav_scan && currentClass != EscanearQrActivity::class.java -> {
                    startActivity(Intent(this, EscanearQrActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                itemId == R.id.nav_stats && currentClass != EstadisticasActivity::class.java -> {
                    startActivity(Intent(this, EstadisticasActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                itemId == R.id.nav_contenedores && currentClass != EstadoContenedoresActivity::class.java -> {
                    startActivity(Intent(this, EstadoContenedoresActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                itemId == R.id.nav_productos && currentClass != GestionProductosActivity::class.java -> {
                    startActivity(Intent(this, GestionProductosActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                else -> true
            }
        }
    }
}
