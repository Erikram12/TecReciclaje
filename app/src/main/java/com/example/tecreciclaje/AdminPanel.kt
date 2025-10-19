package com.example.tecreciclaje

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.tecreciclaje.adminpanel.EscanearQrActivity
import com.example.tecreciclaje.adminpanel.EstadisticasActivity
import com.example.tecreciclaje.adminpanel.EstadoContenedoresActivity
import com.example.tecreciclaje.adminpanel.GestionProductosActivity
import com.example.tecreciclaje.utils.CustomAlertDialog
import com.example.tecreciclaje.utils.FCMTokenManager
import com.example.tecreciclaje.utils.SessionManager
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class AdminPanel : AppCompatActivity() {

    // Puntos por pieza
    companion object {
        private const val PUNTOS_PLASTICO = 20
        private const val PUNTOS_ALUMINIO = 30

        // Rango para interpretar "1 registro = 1 pieza"
        private const val RANGO_MIN_PLAST = 20
        private const val RANGO_MAX_PLAST = 29
        private const val RANGO_MIN_ALUM = 30
        private const val RANGO_MAX_ALUM = 39

        // Precios por pieza (ajusta a tu necesidad)
        private const val PRECIO_POR_BOTELLA_PLASTICO = 0.5 // 20 botellas = $10 -> $0.5 c/u
        private const val PRECIO_POR_LATA_ALUMINIO = 1.0 // ejemplo
    }

    private lateinit var tvAdminName: TextView
    private lateinit var tvPlasticosTotal: TextView
    private lateinit var tvAluminiosTotal: TextView
    private lateinit var tvGananciasTotal: TextView
    private lateinit var barChart: BarChart
    private lateinit var btnLogout: ImageButton
    private lateinit var cardScan: CardView
    private lateinit var cardStats: CardView
    private lateinit var cardContenedores: CardView

    private val dayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val dayOnlyFormat = SimpleDateFormat("dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_panel)

        // UI refs
        tvAdminName = findViewById(R.id.tvAdminName)
        btnLogout = findViewById(R.id.btnLogout)
        
        cardScan = findViewById(R.id.cardScan)
        cardStats = findViewById(R.id.cardStats)
        cardContenedores = findViewById(R.id.cardContenedores)

        tvPlasticosTotal = findViewById(R.id.tvPlasticosTotal)
        tvAluminiosTotal = findViewById(R.id.tvAluminiosTotal)
        tvGananciasTotal = findViewById(R.id.tvGananciasTotal)
        barChart = findViewById(R.id.barChartDiario)

        // Configurar barra de navegación moderna
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.nav_dashboard
        
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

        // Toolbar title (si lo usas por tema material3)
        title = "Panel de administrador"

        // acciones
        cargarNombreAdmin()     // muestra nombre admin
        setupLogout()           // botón logout
        setupModules()          // módulos administrativos

        configChart()
        cargarYProcesarDatos()  // tiempo real + últimos 7 días
        cargarDatosGrafica()    // gráfica con total histórico
    }

    // ---------- UI extra ----------
    private fun setupLogout() {
        btnLogout.setOnClickListener {
            CustomAlertDialog.createLogoutDialogWithAnimation(this) {
                // LIMPIEZA COMPLETA DE SESIÓN
                SessionManager.clearCompleteSession(this)
                
                Toast.makeText(this, "Sesión cerrada completamente", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }.show()
        }
    }

    private fun setupModules() {
        // Módulo Escanear QR
        cardScan.setOnClickListener {
            val i = Intent(this, EscanearQrActivity::class.java)
            startActivity(i)
        }

        // Módulo Estadísticas
        cardStats.setOnClickListener {
            val i = Intent(this, EstadisticasActivity::class.java)
            startActivity(i)
        }

        // Módulo Estado de Contenedores
        cardContenedores.setOnClickListener {
            val i = Intent(this, EstadoContenedoresActivity::class.java)
            startActivity(i)
        }
    }

    private fun cargarNombreAdmin() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            tvAdminName.text = "admin"
            return
        }

        val uid = user.uid
        val adminRef = FirebaseDatabase.getInstance()
            .getReference("usuarios")
            .child(uid)

        adminRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                val nombre = snap.child("usuario_nombre").getValue(String::class.java)
                val apellido = snap.child("usuario_apellido").getValue(String::class.java)

                when {
                    !nombre.isNullOrEmpty() && !apellido.isNullOrEmpty() -> {
                        tvAdminName.text = "$nombre $apellido"
                    }
                    !nombre.isNullOrEmpty() -> {
                        tvAdminName.text = nombre
                    }
                    !user.displayName.isNullOrEmpty() -> {
                        tvAdminName.text = user.displayName
                    }
                    user.email != null -> {
                        tvAdminName.text = user.email
                    }
                    else -> {
                        tvAdminName.text = "Admin"
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ADMIN", "No se pudo leer nombre admin", error.toException())
                val u = FirebaseAuth.getInstance().currentUser
                tvAdminName.text = if (u?.email != null) u.email else "Admin"
            }
        })
    }

    // ---------- Gráfica y datos ----------
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
        xAxis.granularity = 1f
        xAxis.setCenterAxisLabels(true)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textSize = 10f
        xAxis.labelRotationAngle = 45f
        xAxis.textColor = getColor(R.color.dark_gray)

        barChart.axisLeft.apply {
            setDrawGridLines(true)
            textSize = 10f
            axisMinimum = 0f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return value.toInt().toString()
                }
            }
        }
    }

    private fun startOfDayMillis(t: Long): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = t
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun ultimos7DiasLabels(): List<String> {
        val hoyInicio = startOfDayMillis(System.currentTimeMillis())
        val labels = mutableListOf<String>()
        for (i in 6 downTo 0) {
            labels.add(dayFormat.format(Date(hoyInicio - i * 24L * 60L * 60L * 1000L)))
        }
        return labels
    }

    private fun cargarYProcesarDatos() {
        val usersRef = FirebaseDatabase.getInstance().getReference("usuarios")
        val reiniciosRef = FirebaseDatabase.getInstance().getReference("reinicios_contadores")

        val corteMillis = startOfDayMillis(System.currentTimeMillis() - 6L * 24L * 60L * 60L * 1000L)
        val ejeX7dias = ultimos7DiasLabels()
        
        Log.d("ADMIN", "Corte de fecha: $corteMillis (${dayFormat.format(Date(corteMillis))})")
        Log.d("ADMIN", "Eje X 7 días: $ejeX7dias")

        // Primero obtener las fechas del último reinicio por tipo de contenedor
        reiniciosRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var ultimoReinicioPlastico = 0L
                var ultimoReinicioAluminio = 0L
                var ultimoReinicioManual = 0L
                
                if (snapshot.exists()) {
                    Log.d("ADMIN", "Registros de reinicio encontrados: ${snapshot.childrenCount}")
                    
                    // Buscar el reinicio más reciente por tipo
                    for (reinicioSnap in snapshot.children) {
                        val fecha = reinicioSnap.child("fecha").getValue(Long::class.java)
                        val tipoContenedor = reinicioSnap.child("tipoContenedor").getValue(String::class.java)
                        
                        if (fecha != null) {
                            when (tipoContenedor) {
                                "contePlastico" -> if (fecha > ultimoReinicioPlastico) {
                                    ultimoReinicioPlastico = fecha
                                    Log.d("ADMIN", "Nuevo último reinicio plástico: $fecha (${dayFormat.format(Date(fecha))})")
                                }
                                "conteAluminio" -> if (fecha > ultimoReinicioAluminio) {
                                    ultimoReinicioAluminio = fecha
                                    Log.d("ADMIN", "Nuevo último reinicio aluminio: $fecha (${dayFormat.format(Date(fecha))})")
                                }
                                "manual" -> if (fecha > ultimoReinicioManual) {
                                    ultimoReinicioManual = fecha
                                    Log.d("ADMIN", "Nuevo último reinicio manual: $fecha (${dayFormat.format(Date(fecha))})")
                                }
                            }
                        }
                    }
                    
                    Log.d("ADMIN", "Resumen reinicios - Plástico: ${if (ultimoReinicioPlastico > 0) dayFormat.format(Date(ultimoReinicioPlastico)) else "Ninguno"}, " +
                                  "Aluminio: ${if (ultimoReinicioAluminio > 0) dayFormat.format(Date(ultimoReinicioAluminio)) else "Ninguno"}, " +
                                  "Manual: ${if (ultimoReinicioManual > 0) dayFormat.format(Date(ultimoReinicioManual)) else "Ninguno"}")
                } else {
                    Log.d("ADMIN", "No se encontraron registros de reinicio")
                }
                
                // Ahora procesar los datos de usuarios considerando los reinicios específicos
                procesarDatosUsuarios(usersRef, corteMillis, ejeX7dias, ultimoReinicioPlastico, ultimoReinicioAluminio, ultimoReinicioManual)
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("ADMIN", "Error obteniendo reinicios", error.toException())
                // Si falla, procesar sin considerar reinicios
                procesarDatosUsuarios(usersRef, corteMillis, ejeX7dias, 0, 0, 0)
            }
        })
    }
    
    private fun procesarDatosUsuarios(usersRef: DatabaseReference, corteMillis: Long, ejeX7dias: List<String>, 
                                     ultimoReinicioPlastico: Long, ultimoReinicioAluminio: Long, ultimoReinicioManual: Long) {
        usersRef.addValueEventListener(object : ValueEventListener { // tiempo real
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalPlasticos = 0
                var totalAluminios = 0

                val plasticosPorDia = mutableMapOf<String, Int>()
                val aluminiosPorDia = mutableMapOf<String, Int>()
                for (d in ejeX7dias) {
                    plasticosPorDia[d] = 0
                    aluminiosPorDia[d] = 0
                }

                Log.d("ADMIN", "Procesando ${snapshot.childrenCount} usuarios")

                for (usuarioSnap in snapshot.children) {
                    if (usuarioSnap == null) continue
                    
                    Log.d("ADMIN", "Procesando usuario: ${usuarioSnap.key}")
                    
                    val ptsSnap = usuarioSnap.child("puntos")
                    if (!ptsSnap.exists()) {
                        Log.d("ADMIN", "Usuario ${usuarioSnap.key} no tiene puntos")
                        continue
                    }
                    
                    Log.d("ADMIN", "Usuario ${usuarioSnap.key} tiene ${ptsSnap.childrenCount} registros de puntos")

                    for (reg in ptsSnap.children) {
                        if (reg == null) continue
                        
                        Log.d("ADMIN", "Registro ID: ${reg.key}")
                        Log.d("ADMIN", "Registro completo: ${reg.value}")
                        
                        val cantidad = reg.child("punto_cantidad").getValue(Long::class.java)
                        val fechaObj = reg.child("punto_fecha").value
                        val tipo = reg.child("punto_tipo").getValue(String::class.java)
                        
                        Log.d("ADMIN", "Cantidad extraída: $cantidad")
                        Log.d("ADMIN", "Fecha extraída: $fechaObj (tipo: ${fechaObj?.javaClass?.simpleName ?: "null"})")
                        Log.d("ADMIN", "Tipo extraído: $tipo")
                        
                        // Solo procesar registros de tipo "ganado", excluir "recompensa"
                        if (cantidad == null || fechaObj == null || tipo == null || tipo != "ganado") {
                            Log.w("ADMIN", "Datos nulos o tipo incorrecto - Cantidad: $cantidad, Fecha: $fechaObj, Tipo: $tipo")
                            continue
                        }

                        // Manejar diferentes tipos de fecha
                        val fechaMillis = when (fechaObj) {
                            is Long -> fechaObj
                            is String -> try {
                                fechaObj.toLong()
                            } catch (e: NumberFormatException) {
                                Log.w("ADMIN", "Formato de fecha inválido: $fechaObj")
                                continue
                            }
                            else -> {
                                Log.w("ADMIN", "Tipo de fecha no soportado: ${fechaObj.javaClass.simpleName}")
                                continue
                            }
                        }

                        // Verificar que el registro esté después del reinicio correspondiente
                        val esPlastico = (cantidad in RANGO_MIN_PLAST..RANGO_MAX_PLAST) || 
                                       (cantidad % PUNTOS_PLASTICO == 0L && cantidad % PUNTOS_ALUMINIO != 0L)
                        val esAluminio = (cantidad in RANGO_MIN_ALUM..RANGO_MAX_ALUM) || 
                                       (cantidad % PUNTOS_ALUMINIO == 0L && cantidad % PUNTOS_PLASTICO != 0L)
                        
                        // Determinar qué reinicio aplicar
                        val reinicioAplicar = when {
                            esPlastico -> maxOf(ultimoReinicioPlastico, ultimoReinicioManual)
                            esAluminio -> maxOf(ultimoReinicioAluminio, ultimoReinicioManual)
                            else -> 0L
                        }
                        
                        if (reinicioAplicar > 0 && fechaMillis < reinicioAplicar) {
                            val tipoMaterial = if (esPlastico) "plástico" else "aluminio"
                            Log.d("ADMIN", "Registro antes del reinicio $tipoMaterial - Fecha: $fechaMillis (${dayFormat.format(Date(fechaMillis))}) < Reinicio: $reinicioAplicar (${dayFormat.format(Date(reinicioAplicar))})")
                            continue // ignorar registros antes del reinicio
                        } else if (reinicioAplicar > 0) {
                            val tipoMaterial = if (esPlastico) "plástico" else "aluminio"
                            Log.d("ADMIN", "Registro después del reinicio $tipoMaterial ✓ - Fecha: $fechaMillis (${dayFormat.format(Date(fechaMillis))}) >= Reinicio: $reinicioAplicar (${dayFormat.format(Date(reinicioAplicar))})")
                        }
                        
                        if (fechaMillis < corteMillis) {
                            Log.d("ADMIN", "Registro fuera de rango - Fecha: $fechaMillis (${dayFormat.format(Date(fechaMillis))}) < Corte: $corteMillis")
                            continue // fuera de los 7 días
                        }

                        val dia = dayFormat.format(Date(fechaMillis))
                        
                        // Log para debugging de cada registro
                        Log.d("ADMIN", "Registro - Cantidad: $cantidad, Fecha: $fechaMillis, Día: $dia, UID: ${usuarioSnap.key}")

                        // Caso A: por rangos (1 registro = 1 pieza)
                        if (cantidad in RANGO_MIN_PLAST..RANGO_MAX_PLAST) {
                            totalPlasticos += 1
                            plasticosPorDia[dia] = plasticosPorDia.getOrDefault(dia, 0) + 1
                            Log.d("ADMIN", "Plástico contado - Cantidad: $cantidad, Día: $dia")
                            continue
                        }
                        if (cantidad in RANGO_MIN_ALUM..RANGO_MAX_ALUM) {
                            totalAluminios += 1
                            aluminiosPorDia[dia] = aluminiosPorDia.getOrDefault(dia, 0) + 1
                            Log.d("ADMIN", "Aluminio contado - Cantidad: $cantidad, Día: $dia")
                            continue
                        }

                        // Caso B: múltiplos exactos (varias piezas en un registro)
                        val multiplePlast = (cantidad % PUNTOS_PLASTICO == 0L)
                        val multipleAlum = (cantidad % PUNTOS_ALUMINIO == 0L)

                        if (multiplePlast xor multipleAlum) {
                            if (multiplePlast) {
                                val piezas = (cantidad / PUNTOS_PLASTICO).toInt()
                                totalPlasticos += piezas
                                plasticosPorDia[dia] = plasticosPorDia.getOrDefault(dia, 0) + piezas
                                Log.d("ADMIN", "Múltiples plásticos contados - Cantidad: $cantidad, Piezas: $piezas, Día: $dia")
                            } else {
                                val piezas = (cantidad / PUNTOS_ALUMINIO).toInt()
                                totalAluminios += piezas
                                aluminiosPorDia[dia] = aluminiosPorDia.getOrDefault(dia, 0) + piezas
                                Log.d("ADMIN", "Múltiples aluminios contados - Cantidad: $cantidad, Piezas: $piezas, Día: $dia")
                            }
                        } else {
                            Log.w("ADMIN", "Registro ambiguo/ignorado: $cantidad uid=${usuarioSnap.key}")
                        }
                    }
                }

                val ganancias = totalPlasticos * PRECIO_POR_BOTELLA_PLASTICO + totalAluminios * PRECIO_POR_LATA_ALUMINIO

                // Log para debugging
                Log.d("ADMIN", "=== RESUMEN FINAL ===")
                Log.d("ADMIN", "Reinicio plástico: ${if (ultimoReinicioPlastico > 0) dayFormat.format(Date(ultimoReinicioPlastico)) else "Ninguno"}")
                Log.d("ADMIN", "Reinicio aluminio: ${if (ultimoReinicioAluminio > 0) dayFormat.format(Date(ultimoReinicioAluminio)) else "Ninguno"}")
                Log.d("ADMIN", "Reinicio manual: ${if (ultimoReinicioManual > 0) dayFormat.format(Date(ultimoReinicioManual)) else "Ninguno"}")
                Log.d("ADMIN", "Corte de fecha: ${dayFormat.format(Date(corteMillis))}")
                Log.d("ADMIN", "Datos procesados - Plásticos: $totalPlasticos, Aluminios: $totalAluminios, Ganancias: $${String.format(Locale.getDefault(), "%.2f", ganancias)}")
                Log.d("ADMIN", "===================")

                tvPlasticosTotal.text = totalPlasticos.toString()
                tvAluminiosTotal.text = totalAluminios.toString()
                tvGananciasTotal.text = String.format(Locale.getDefault(), "$%.2f", ganancias)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ADMIN", "Error leyendo datos", error.toException())
            }
        })
    }
    
    private fun cargarDatosGrafica() {
        val usersRef = FirebaseDatabase.getInstance().getReference("usuarios")
        val corteMillis = startOfDayMillis(System.currentTimeMillis() - 6L * 24L * 60L * 60L * 1000L)
        val ejeX7dias = ultimos7DiasLabels()
        
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val plasticosPorDia = mutableMapOf<String, Int>()
                val aluminiosPorDia = mutableMapOf<String, Int>()
                for (d in ejeX7dias) {
                    plasticosPorDia[d] = 0
                    aluminiosPorDia[d] = 0
                }

                for (usuarioSnap in snapshot.children) {
                    val ptsSnap = usuarioSnap.child("puntos")
                    if (!ptsSnap.exists()) continue

                    for (reg in ptsSnap.children) {
                        val cantidad = reg.child("punto_cantidad").getValue(Long::class.java)
                        val fechaObj = reg.child("punto_fecha").value
                        val tipo = reg.child("punto_tipo").getValue(String::class.java)
                        
                        // Solo procesar registros de tipo "ganado", excluir "recompensa"
                        if (cantidad == null || fechaObj == null || tipo == null || tipo != "ganado") {
                            continue
                        }

                        val fechaMillis = when (fechaObj) {
                            is Long -> fechaObj
                            is String -> try {
                                fechaObj.toLong()
                            } catch (e: NumberFormatException) {
                                continue
                            }
                            else -> continue
                        }

                        // Solo filtrar por rango de 7 días, NO por reinicios
                        if (fechaMillis < corteMillis) {
                            continue
                        }

                        val dia = dayFormat.format(Date(fechaMillis))
                        
                        // Contar para la gráfica (total histórico)
                        when {
                            cantidad in RANGO_MIN_PLAST..RANGO_MAX_PLAST -> {
                                plasticosPorDia[dia] = plasticosPorDia.getOrDefault(dia, 0) + 1
                                continue
                            }
                            cantidad in RANGO_MIN_ALUM..RANGO_MAX_ALUM -> {
                                aluminiosPorDia[dia] = aluminiosPorDia.getOrDefault(dia, 0) + 1
                                continue
                            }
                        }

                        val multiplePlast = (cantidad % PUNTOS_PLASTICO == 0L)
                        val multipleAlum = (cantidad % PUNTOS_ALUMINIO == 0L)

                        if (multiplePlast xor multipleAlum) {
                            if (multiplePlast) {
                                val piezas = (cantidad / PUNTOS_PLASTICO).toInt()
                                plasticosPorDia[dia] = plasticosPorDia.getOrDefault(dia, 0) + piezas
                            } else {
                                val piezas = (cantidad / PUNTOS_ALUMINIO).toInt()
                                aluminiosPorDia[dia] = aluminiosPorDia.getOrDefault(dia, 0) + piezas
                            }
                        }
                    }
                }

                // Actualizar gráfica con datos históricos
                if (barChart != null) {
                    renderBarChart7Dias(ejeX7dias, plasticosPorDia, aluminiosPorDia)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ADMIN", "Error leyendo datos para gráfica", error.toException())
            }
        })
    }

    private fun fechaCompletaADia(fechaCompleta: String): String {
        return try {
            val fecha = dayFormat.parse(fechaCompleta)
            dayOnlyFormat.format(fecha)
        } catch (e: Exception) {
            Log.w("ADMIN", "Error convirtiendo fecha: $fechaCompleta")
            fechaCompleta // Si falla, devuelve la fecha original
        }
    }

    private fun renderBarChart7Dias(fechasOrdenadas: List<String>,
                                     plasticosPorDia: Map<String, Int>,
                                     aluminiosPorDia: Map<String, Int>) {

        // Verificar que los parámetros no sean nulos
        if (fechasOrdenadas.isEmpty() || plasticosPorDia.isEmpty() || aluminiosPorDia.isEmpty()) {
            Log.e("ADMIN", "Datos nulos para la gráfica")
            return
        }

        val entriesPlast = mutableListOf<BarEntry>()
        val entriesAlum = mutableListOf<BarEntry>()

        for (i in fechasOrdenadas.indices) {
            val f = fechasOrdenadas[i]
            if (f != null) {
                entriesPlast.add(BarEntry(i.toFloat(), plasticosPorDia.getOrDefault(f, 0).toFloat()))
                entriesAlum.add(BarEntry(i.toFloat(), aluminiosPorDia.getOrDefault(f, 0).toFloat()))
            }
        }

        val dsPlast = BarDataSet(entriesPlast, "Plásticos")
        dsPlast.color = getColor(R.color.verde_principal)
        dsPlast.valueTextSize = 10f
        dsPlast.valueTextColor = getColor(R.color.verde_principal)
        dsPlast.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return value.toInt().toString()
            }
        }
        
        val dsAlum = BarDataSet(entriesAlum, "Aluminios")
        dsAlum.color = getColor(R.color.dark_gray)
        dsAlum.valueTextSize = 10f
        dsAlum.valueTextColor = getColor(R.color.dark_gray)
        dsAlum.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return value.toInt().toString()
            }
        }

        val data = BarData(dsPlast, dsAlum)

        val groupSpace = 0.20f
        val barSpace = 0.03f
        val barWidth = 0.37f
        data.barWidth = barWidth

        barChart.data = data

        // Convertir fechas completas a solo días para la visualización
        val fechasSoloDias = fechasOrdenadas.map { fechaCompletaADia(it) }

        val xAxis = barChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(fechasSoloDias)
        xAxis.granularity = 1f
        xAxis.setCenterAxisLabels(true)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)

        val start = 0f
        val end = start + fechasOrdenadas.size
        xAxis.axisMinimum = start
        xAxis.axisMaximum = end
        barChart.groupBars(start, groupSpace, barSpace)

        barChart.axisRight.isEnabled = false
        barChart.description.isEnabled = false
        barChart.invalidate()
    }
}
