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
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import android.view.View
import android.widget.ImageView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class EstadisticasActivity : AppCompatActivity() {

    // Constantes
    companion object {
        private const val PUNTOS_PLASTICO = 3
        private const val PUNTOS_ALUMINIO = 4
        private const val RANGO_MIN_PLAST = 3
        private const val RANGO_MAX_PLAST = 3
        private const val RANGO_MIN_ALUM = 4
        private const val RANGO_MAX_ALUM = 4
        
        // Valores exactos de equivalencias
        // PET: 0.175 MXN por botella, 3 puntos, 0.05833 MXN por punto
        private const val PRECIO_POR_BOTELLA_PLASTICO = 0.175
        private const val VALOR_POR_PUNTO_PLASTICO = 0.05833
        
        // Aluminio: 0.253 MXN por lata, 4 puntos, 0.06333 MXN por punto
        private const val PRECIO_POR_LATA_ALUMINIO = 0.253
        private const val VALOR_POR_PUNTO_ALUMINIO = 0.06333
        
        private const val ITEMS_POR_PAGINA = 5
    }

    // UI Components
    private lateinit var tvTotalPlasticos: TextView
    private lateinit var tvTotalAluminios: TextView
    private lateinit var tvTotalGanancias: TextView
    private lateinit var tvPromedioDiario: TextView
    private lateinit var tvTituloTabla: TextView
    private lateinit var recyclerViewEstadisticas: RecyclerView
    
    // Análisis de tendencias
    private lateinit var lineChartTendencias: LineChart
    private lateinit var tvSaldoTotal: TextView
    private lateinit var tvCambioPorcentual: TextView
    private lateinit var tvCambioAbsoluto: TextView
    private lateinit var tvPeriodoCambio: TextView
    private lateinit var ivTendencia: ImageView
    private lateinit var ivToggleVisibility: ImageView
    private lateinit var btn1Semana: com.google.android.material.button.MaterialButton
    private lateinit var btn1Mes: com.google.android.material.button.MaterialButton
    private lateinit var btn6Meses: com.google.android.material.button.MaterialButton
    private lateinit var btn1Anio: com.google.android.material.button.MaterialButton
    private lateinit var btnTotal: com.google.android.material.button.MaterialButton
    private lateinit var btnAnterior: Button
    private lateinit var btnSiguiente: Button
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
    
    // Período de tendencias
    private enum class PeriodoTendencias {
        SEMANA, MES, SEIS_MESES, ANIO, TOTAL
    }
    private var periodoActual = PeriodoTendencias.SEMANA
    private var saldoVisible = true

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
        setupTendencias()
        cargarEstadisticas()
        setupNavigation()
    }

    private fun initViews() {
        tvTotalPlasticos = findViewById(R.id.tvTotalPlasticos)
        tvTotalAluminios = findViewById(R.id.tvTotalAluminios)
        tvTotalGanancias = findViewById(R.id.tvTotalGanancias)
        tvPromedioDiario = findViewById(R.id.tvPromedioDiario)
        tvTituloTabla = findViewById(R.id.tvTituloTabla)
        recyclerViewEstadisticas = findViewById(R.id.recyclerViewEstadisticas)
        
        // Análisis de tendencias
        lineChartTendencias = findViewById(R.id.lineChartTendencias)
        tvSaldoTotal = findViewById(R.id.tvSaldoTotal)
        tvCambioPorcentual = findViewById(R.id.tvCambioPorcentual)
        tvCambioAbsoluto = findViewById(R.id.tvCambioAbsoluto)
        tvPeriodoCambio = findViewById(R.id.tvPeriodoCambio)
        ivTendencia = findViewById(R.id.ivTendencia)
        ivToggleVisibility = findViewById(R.id.ivToggleVisibility)
        btn1Semana = findViewById(R.id.btn1Semana)
        btn1Mes = findViewById(R.id.btn1Mes)
        btn6Meses = findViewById(R.id.btn6Meses)
        btn1Anio = findViewById(R.id.btn1Anio)
        btnTotal = findViewById(R.id.btnTotal)
        btnAnterior = findViewById(R.id.btnAnterior)
        btnSiguiente = findViewById(R.id.btnSiguiente)
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
                actualizarTendencias()
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

    private fun setupTendencias() {
        // Configurar LineChart
        configLineChart()
        
        // Configurar toggle de visibilidad
        ivToggleVisibility.setOnClickListener {
            saldoVisible = !saldoVisible
            actualizarVisibilidadSaldo()
        }
        
        // Configurar botones de período
        btn1Semana.setOnClickListener { cambiarPeriodo(PeriodoTendencias.SEMANA) }
        btn1Mes.setOnClickListener { cambiarPeriodo(PeriodoTendencias.MES) }
        btn6Meses.setOnClickListener { cambiarPeriodo(PeriodoTendencias.SEIS_MESES) }
        btn1Anio.setOnClickListener { cambiarPeriodo(PeriodoTendencias.ANIO) }
        btnTotal.setOnClickListener { cambiarPeriodo(PeriodoTendencias.TOTAL) }
        
        actualizarVisibilidadSaldo()
    }
    
    private fun configLineChart() {
        lineChartTendencias.description.isEnabled = false
        lineChartTendencias.setTouchEnabled(true)
        lineChartTendencias.setDragEnabled(true)
        lineChartTendencias.setScaleEnabled(false)
        lineChartTendencias.setPinchZoom(false)
        lineChartTendencias.setDrawGridBackground(false)
        
        // Configurar eje X
        val xAxis = lineChartTendencias.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(true)
        xAxis.textColor = getColor(R.color.dark_gray)
        xAxis.textSize = 10f
        xAxis.granularity = 1f
        
        // Configurar eje Y izquierdo
        val leftAxis = lineChartTendencias.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.gridColor = getColor(R.color.background_light)
        leftAxis.textColor = getColor(R.color.dark_gray)
        leftAxis.textSize = 10f
        leftAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return String.format(Locale.getDefault(), "%.2f", value)
            }
        }
        
        // Deshabilitar eje Y derecho
        lineChartTendencias.axisRight.isEnabled = false
        
        // Configurar leyenda
        val legend = lineChartTendencias.legend
        legend.isEnabled = false
    }
    
    private fun cambiarPeriodo(periodo: PeriodoTendencias) {
        periodoActual = periodo
        actualizarBotonesPeriodo()
        actualizarTendencias()
    }
    
    private fun actualizarBotonesPeriodo() {
        val colorSelected = getColor(R.color.primary)
        val colorUnselected = getColor(R.color.dark_gray)
        val bgSelected = android.content.res.ColorStateList.valueOf(colorSelected)
        val bgUnselected = android.content.res.ColorStateList.valueOf(android.R.color.transparent)
        
        btn1Semana.apply {
            backgroundTintList = if (periodoActual == PeriodoTendencias.SEMANA) bgSelected else bgUnselected
            setTextColor(if (periodoActual == PeriodoTendencias.SEMANA) getColor(R.color.white) else colorUnselected)
        }
        btn1Mes.apply {
            backgroundTintList = if (periodoActual == PeriodoTendencias.MES) bgSelected else bgUnselected
            setTextColor(if (periodoActual == PeriodoTendencias.MES) getColor(R.color.white) else colorUnselected)
        }
        btn6Meses.apply {
            backgroundTintList = if (periodoActual == PeriodoTendencias.SEIS_MESES) bgSelected else bgUnselected
            setTextColor(if (periodoActual == PeriodoTendencias.SEIS_MESES) getColor(R.color.white) else colorUnselected)
        }
        btn1Anio.apply {
            backgroundTintList = if (periodoActual == PeriodoTendencias.ANIO) bgSelected else bgUnselected
            setTextColor(if (periodoActual == PeriodoTendencias.ANIO) getColor(R.color.white) else colorUnselected)
        }
        btnTotal.apply {
            backgroundTintList = if (periodoActual == PeriodoTendencias.TOTAL) bgSelected else bgUnselected
            setTextColor(if (periodoActual == PeriodoTendencias.TOTAL) getColor(R.color.white) else colorUnselected)
        }
    }
    
    private fun actualizarTendencias() {
        val ahora = System.currentTimeMillis()
        val calendario = Calendar.getInstance()
        
        // Filtrar datos según el período
        val fechaLimite = when (periodoActual) {
            PeriodoTendencias.SEMANA -> ahora - (7 * 24 * 60 * 60 * 1000L)
            PeriodoTendencias.MES -> {
                calendario.add(Calendar.MONTH, -1)
                calendario.timeInMillis
            }
            PeriodoTendencias.SEIS_MESES -> {
                calendario.add(Calendar.MONTH, -6)
                calendario.timeInMillis
            }
            PeriodoTendencias.ANIO -> {
                calendario.add(Calendar.YEAR, -1)
                calendario.timeInMillis
            }
            PeriodoTendencias.TOTAL -> 0L
        }
        
        val datosFiltrados = if (fechaLimite > 0) {
            todasLasEstadisticas.filter { estadistica ->
                try {
                    val fechaEstadistica = if (vistaPorMes) {
                        monthFormat.parse(estadistica.fecha)?.time ?: 0L
                    } else {
                        dayFormat.parse(estadistica.fecha)?.time ?: 0L
                    }
                    fechaEstadistica >= fechaLimite
                } catch (e: Exception) {
                    false
                }
            }.sortedBy { estadistica ->
                if (vistaPorMes) {
                    monthFormat.parse(estadistica.fecha)?.time ?: 0L
                } else {
                    dayFormat.parse(estadistica.fecha)?.time ?: 0L
                }
            }
        } else {
            todasLasEstadisticas.sortedBy { estadistica ->
                if (vistaPorMes) {
                    monthFormat.parse(estadistica.fecha)?.time ?: 0L
                } else {
                    dayFormat.parse(estadistica.fecha)?.time ?: 0L
                }
            }
        }
        
        // Calcular saldo total acumulado
        var saldoAcumulado = 0.0
        val entries = mutableListOf<Entry>()
        val labels = mutableListOf<String>()
        
        datosFiltrados.forEachIndexed { index, estadistica ->
            saldoAcumulado += estadistica.ganancias
            entries.add(Entry(index.toFloat(), saldoAcumulado.toFloat()))
            
            labels.add(if (vistaPorMes) {
                monthOnlyFormat.format(monthFormat.parse(estadistica.fecha) ?: Date())
            } else {
                dayOnlyFormat.format(dayFormat.parse(estadistica.fecha) ?: Date())
            })
        }
        
        // Actualizar gráfica
        if (entries.isNotEmpty()) {
            val dataSet = LineDataSet(entries, "Ganancias Acumuladas")
            dataSet.color = getColor(R.color.primary)
            dataSet.lineWidth = 3f
            dataSet.setCircleColor(getColor(R.color.primary))
            dataSet.circleRadius = 4f
            dataSet.setDrawCircleHole(false)
            dataSet.setDrawValues(false)
            dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
            dataSet.cubicIntensity = 0.2f
            
            // Área sombreada
            dataSet.setDrawFilled(true)
            dataSet.fillColor = getColor(R.color.primary)
            dataSet.fillAlpha = 30
            
            val lineData = LineData(dataSet)
            lineChartTendencias.data = lineData
            
            val xAxis = lineChartTendencias.xAxis
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.labelCount = minOf(labels.size, 7)
            
            lineChartTendencias.invalidate()
        }
        
        // Calcular y mostrar saldo total y cambio
        val saldoActual = saldoAcumulado
        
        // Calcular saldo anterior (período previo equivalente)
        val saldoAnterior = when (periodoActual) {
            PeriodoTendencias.SEMANA -> {
                // Saldo de la semana anterior
                val semanaAnteriorInicio = ahora - (14 * 24 * 60 * 60 * 1000L)
                val semanaAnteriorFin = ahora - (7 * 24 * 60 * 60 * 1000L)
                todasLasEstadisticas.filter { estadistica ->
                    try {
                        val fechaEstadistica = if (vistaPorMes) {
                            monthFormat.parse(estadistica.fecha)?.time ?: 0L
                        } else {
                            dayFormat.parse(estadistica.fecha)?.time ?: 0L
                        }
                        fechaEstadistica >= semanaAnteriorInicio && fechaEstadistica < semanaAnteriorFin
                    } catch (e: Exception) {
                        false
                    }
                }.sumOf { it.ganancias }
            }
            PeriodoTendencias.MES -> {
                // Saldo del mes anterior
                val mesAnteriorInicio = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -2)
                }.timeInMillis
                val mesAnteriorFin = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -1)
                }.timeInMillis
                todasLasEstadisticas.filter { estadistica ->
                    try {
                        val fechaEstadistica = if (vistaPorMes) {
                            monthFormat.parse(estadistica.fecha)?.time ?: 0L
                        } else {
                            dayFormat.parse(estadistica.fecha)?.time ?: 0L
                        }
                        fechaEstadistica >= mesAnteriorInicio && fechaEstadistica < mesAnteriorFin
                    } catch (e: Exception) {
                        false
                    }
                }.sumOf { it.ganancias }
            }
            PeriodoTendencias.SEIS_MESES -> {
                // Saldo de los 6 meses anteriores
                val seisMesesAnteriorInicio = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -12)
                }.timeInMillis
                val seisMesesAnteriorFin = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -6)
                }.timeInMillis
                todasLasEstadisticas.filter { estadistica ->
                    try {
                        val fechaEstadistica = if (vistaPorMes) {
                            monthFormat.parse(estadistica.fecha)?.time ?: 0L
                        } else {
                            dayFormat.parse(estadistica.fecha)?.time ?: 0L
                        }
                        fechaEstadistica >= seisMesesAnteriorInicio && fechaEstadistica < seisMesesAnteriorFin
                    } catch (e: Exception) {
                        false
                    }
                }.sumOf { it.ganancias }
            }
            PeriodoTendencias.ANIO -> {
                // Saldo del año anterior
                val anioAnteriorInicio = Calendar.getInstance().apply {
                    add(Calendar.YEAR, -2)
                }.timeInMillis
                val anioAnteriorFin = Calendar.getInstance().apply {
                    add(Calendar.YEAR, -1)
                }.timeInMillis
                todasLasEstadisticas.filter { estadistica ->
                    try {
                        val fechaEstadistica = if (vistaPorMes) {
                            monthFormat.parse(estadistica.fecha)?.time ?: 0L
                        } else {
                            dayFormat.parse(estadistica.fecha)?.time ?: 0L
                        }
                        fechaEstadistica >= anioAnteriorInicio && fechaEstadistica < anioAnteriorFin
                    } catch (e: Exception) {
                        false
                    }
                }.sumOf { it.ganancias }
            }
            PeriodoTendencias.TOTAL -> {
                // Para total, comparar con el primer valor
                if (datosFiltrados.isNotEmpty()) {
                    datosFiltrados.first().ganancias
                } else 0.0
            }
        }
        
        val cambio = saldoActual - saldoAnterior
        val cambioPorcentual = if (saldoAnterior != 0.0) {
            (cambio / saldoAnterior) * 100
        } else 0.0
        
        // Actualizar UI
        tvSaldoTotal.text = String.format(Locale.getDefault(), "%.2f MXN", saldoActual)
        
        val colorCambio = if (cambio >= 0) getColor(R.color.verde_principal) else getColor(R.color.red_dark)
        tvCambioPorcentual.setTextColor(colorCambio)
        tvCambioAbsoluto.setTextColor(colorCambio)
        val signoPorcentual = if (cambioPorcentual >= 0) "+" else ""
        val signoAbsoluto = if (cambio >= 0) "+" else "-"
        tvCambioPorcentual.text = String.format(Locale.getDefault(), "$signoPorcentual%.2f %%", cambioPorcentual)
        tvCambioAbsoluto.text = String.format(Locale.getDefault(), "$signoAbsoluto%.2f MXN", Math.abs(cambio))
        
        // Actualizar icono de tendencia
        ivTendencia.setImageResource(if (cambio >= 0) R.drawable.ic_trending_up_24 else R.drawable.ic_trending_down_24)
        ivTendencia.setColorFilter(colorCambio)
        
        // Actualizar período del cambio
        tvPeriodoCambio.text = when (periodoActual) {
            PeriodoTendencias.SEMANA -> "(Última semana)"
            PeriodoTendencias.MES -> "(Último mes)"
            PeriodoTendencias.SEIS_MESES -> "(Últimos 6 meses)"
            PeriodoTendencias.ANIO -> "(Último año)"
            PeriodoTendencias.TOTAL -> "(Total)"
        }
        
        actualizarVisibilidadSaldo()
    }
    
    private fun actualizarVisibilidadSaldo() {
        if (saldoVisible) {
            tvSaldoTotal.visibility = View.VISIBLE
            ivToggleVisibility.setImageResource(R.drawable.ic_visibility)
        } else {
            tvSaldoTotal.visibility = View.INVISIBLE
            ivToggleVisibility.setImageResource(R.drawable.ic_visibility_off)
        }
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
