package com.example.tecreciclaje.userpanel

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tecreciclaje.Model.LogrosAdapter
import com.example.tecreciclaje.R
import com.example.tecreciclaje.domain.model.Historial
import com.example.tecreciclaje.domain.model.Logro
import com.example.tecreciclaje.utils.AppLogger
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.*

class LogrosActivity : AppCompatActivity(), LogrosAdapter.OnLogroClickListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var userRef: DatabaseReference
    private lateinit var logrosRef: DatabaseReference

    private lateinit var textViewLogrosDisponibles: TextView
    private lateinit var textViewLogrosDesbloqueados: TextView
    private lateinit var recyclerViewLogros: RecyclerView
    private lateinit var btnBack: ImageView
    private lateinit var buttonMostrarMas: Button

    private lateinit var logrosAdapter: LogrosAdapter
    private val logrosList = mutableListOf<Logro>()
    private val allLogrosList = mutableListOf<Logro>() // Lista completa de todos los logros
    private val displayedLogrosList = mutableListOf<Logro>() // Lista de logros mostrados actualmente
    private var currentDisplayCount = 3 // Número inicial de logros a mostrar
    private val INCREMENT_COUNT = 3 // Cuántos logros más mostrar cada vez

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logros)

        auth = FirebaseAuth.getInstance()
        initializeViews()
        setupClickListeners()
        setupRecyclerView()

        val currentUser = auth.currentUser
        if (currentUser == null) {
            finish()
            return
        }

        userRef = FirebaseDatabase.getInstance().getReference("usuarios").child(currentUser.uid)
        logrosRef = FirebaseDatabase.getInstance().getReference("usuarios").child(currentUser.uid).child("logros")

        loadUserStats()
        loadLogros()
        
        // Forzar verificación de logros al abrir la actividad
        forceCheckLogros()
    }

    private fun initializeViews() {
        textViewLogrosDisponibles = findViewById(R.id.textViewLogrosDisponibles)
        textViewLogrosDesbloqueados = findViewById(R.id.textViewLogrosDesbloqueados)
        recyclerViewLogros = findViewById(R.id.recyclerViewLogros)
        btnBack = findViewById(R.id.btnBack)
        buttonMostrarMas = findViewById(R.id.buttonMostrarMas)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }
        
        buttonMostrarMas.setOnClickListener {
            showMoreLogros()
        }
    }

    private fun setupRecyclerView() {
        logrosAdapter = LogrosAdapter(logrosList, this)
        recyclerViewLogros.layoutManager = LinearLayoutManager(this)
        recyclerViewLogros.adapter = logrosAdapter
    }

    private fun loadUserStats() {
        // Esta función ya no es necesaria ya que los logros se cargan en loadLogros()
        // Los contadores se actualizan automáticamente cuando se cargan los logros
    }

    private fun loadLogros() {
        // Crear lista de todos los logros posibles inmediatamente
        val allPossibleLogros = createAllPossibleLogros()
        allLogrosList.clear()
        allLogrosList.addAll(allPossibleLogros)
        
        // Ordenar logros: "Primer Reciclaje" siempre primero
        ordenarLogros()
        
        // Mostrar solo los primeros 3 logros inicialmente
        updateDisplayedLogros()
        
        // Actualizar contadores inmediatamente
        textViewLogrosDisponibles.text = allLogrosList.size.toString()
        textViewLogrosDesbloqueados.text = "0"
        logrosAdapter.notifyDataSetChanged()
        
        // Luego cargar desde Firebase para obtener el estado actual
        logrosRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var logrosDesbloqueados = 0
                
                if (snapshot.exists()) {
                    // NUEVO: Verificar y corregir logros incompletos
                    var needsUpdate = false
                    
                    // Mapear logros de Firebase a la lista completa
                    for (logro in allLogrosList) {
                        val logroSnapshot = snapshot.child(logro.id)
                        if (logroSnapshot.exists()) {
                            // Verificar si el logro está completo
                            if (logroSnapshot.hasChild("titulo") && 
                                logroSnapshot.hasChild("descripcion") && 
                                logroSnapshot.hasChild("icono") && 
                                logroSnapshot.hasChild("objetivo") && 
                                logroSnapshot.hasChild("recompensa") && 
                                logroSnapshot.hasChild("tipo")) {
                                
                                // Logro completo, cargar datos
                                val firebaseLogro = logroSnapshot.getValue(Logro::class.java)
                                if (firebaseLogro != null) {
                                    logro.progreso = firebaseLogro.progreso
                                    logro.desbloqueado = firebaseLogro.desbloqueado
                                    logro.reclamado = firebaseLogro.reclamado
                                }
                            } else {
                                // Logro incompleto, reemplazar con estructura completa
                                needsUpdate = true
                                
                                // Mantener solo el progreso si existe
                                if (logroSnapshot.hasChild("progreso")) {
                                    val progreso = logroSnapshot.child("progreso").getValue(Int::class.java)
                                    if (progreso != null) {
                                        logro.progreso = progreso
                                    }
                                }
                                
                                // Resetear estado
                                logro.desbloqueado = false
                                logro.reclamado = false
                            }
                        } else {
                            // Logro no existe, crear nuevo
                            needsUpdate = true
                        }
                        
                        if (logro.desbloqueado) {
                            logrosDesbloqueados++
                        }
                    }
                    
                    // Actualizar Firebase si es necesario
                    if (needsUpdate) {
                        for (logro in allLogrosList) {
                            logrosRef.child(logro.id).setValue(logro)
                        }
                    }
                } else {
                    // No hay logros en Firebase, crear todos
                    for (logro in allLogrosList) {
                        logrosRef.child(logro.id).setValue(logro)
                    }
                }
                
                // Después de cargar los logros, verificar y actualizar con datos reales del usuario
                checkAndUpdateLogrosWithUserData()

                textViewLogrosDesbloqueados.text = logrosDesbloqueados.toString()
                logrosAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                AppLogger.e("Error al cargar logros: ${error.message}")
            }
        })
    }

    override fun onLogroClick(logro: Logro) {
        if (logro.desbloqueado) {
            AppLogger.d("Logro desbloqueado: ${logro.titulo}")
        } else {
            AppLogger.d("Progreso: ${logro.textoProgreso}")
        }
    }

    override fun onReclamarClick(logro: Logro) {
        // Marcar temporalmente que se está reclamando una recompensa
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userRef = FirebaseDatabase.getInstance().getReference("usuarios")
                .child(currentUser.uid)
            
            // Agregar campo temporal para indicar recompensa
            userRef.child("reclamando_recompensa").setValue(logro.recompensa)
            
            // Marcar como reclamado y sumar puntos
            logro.reclamado = true
            logrosRef.child(logro.id).child("reclamado").setValue(true)
            
            // Sumar puntos de recompensa al user_puntos
            addRewardPoints(logro.recompensa)
            
            // Limpiar el campo temporal después de 2 segundos para dar tiempo al backend
            Handler(Looper.getMainLooper()).postDelayed({
                userRef.child("reclamando_recompensa").removeValue()
            }, 2000) // 2 segundos de delay
            
            // Actualizar la UI
            logrosAdapter.notifyDataSetChanged()
            
            AppLogger.d("Puntos reclamados: +${logro.recompensa} pts")
        }
    }

    // Método para actualizar el progreso de un logro
    fun updateLogroProgress(logroId: String, newProgress: Int) {
        logrosRef.child(logroId).child("progreso").setValue(newProgress)
    }

    // Método para verificar y actualizar logros usando el historial de puntos
    private fun checkAndUpdateLogrosWithUserData() {
        // Obtener datos del historial de puntos
        val puntosRef = FirebaseDatabase.getInstance()
            .getReference("usuarios").child(FirebaseAuth.getInstance().currentUser!!.uid).child("puntos")
        
        puntosRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalReciclajes = 0
                var totalPuntosGanados = 0
                val diasReciclaje = mutableSetOf<String>()
                
                // CORREGIDO: Solo contar puntos con punto_tipo:"ganado" (excluyendo recompensas)
                android.util.Log.d("LogrosActivity", "Verificando ${snapshot.childrenCount} registros de historial")
                for (historialSnapshot in snapshot.children) {
                    val historial = historialSnapshot.getValue(Historial::class.java)
                    if (historial != null) {
                        val tipo = historial.getTipo
                        android.util.Log.d("LogrosActivity", "Registro: tipo='$tipo', cantidad=${historial.getCantidad}")
                        // Solo contar entradas con tipo "ganado" (reciclajes reales, NO recompensas)
                        if (tipo == "ganado") {
                            totalReciclajes++
                            totalPuntosGanados += historial.getCantidad
                            android.util.Log.d("LogrosActivity", "Contado como reciclaje: total=$totalReciclajes, puntos=$totalPuntosGanados")
                            
                            // Agregar fecha para calcular días consecutivos
                            val fecha = historial.getFecha
                            if (fecha != null) {
                                val fechaStr = when (fecha) {
                                    is com.google.firebase.Timestamp -> fecha.toDate().toString().substring(0, 10)
                                    is Long -> java.util.Date(fecha).toString().substring(0, 10)
                                    else -> fecha.toString().substring(0, 10)
                                }
                                diasReciclaje.add(fechaStr)
                            }
                        }
                    }
                }
                
                // Actualizar progreso de cada logro basado en datos del historial
                android.util.Log.d("LogrosActivity", "Resumen: reciclajes=$totalReciclajes, puntos=$totalPuntosGanados, dias=${diasReciclaje.size}")
                for (logro in allLogrosList) {
                    var currentProgress = 0
                    
                    when (logro.tipo) {
                        "reciclajes" -> currentProgress = totalReciclajes
                        "puntos" -> currentProgress = totalPuntosGanados
                        "dias_consecutivos" -> currentProgress = calcularDiasConsecutivos(diasReciclaje)
                    }
                    
                    android.util.Log.d("LogrosActivity", "Logro ${logro.titulo}: progreso=$currentProgress/${logro.objetivo}, desbloqueado=${logro.desbloqueado}")
                    
                    // Solo actualizar si el progreso es diferente
                    if (currentProgress != logro.progreso) {
                        logro.progreso = currentProgress
                        updateLogroProgress(logro.id, currentProgress)
                        android.util.Log.d("LogrosActivity", "Actualizado progreso de ${logro.titulo} a $currentProgress")
                    }
                    
                    // Verificar si se desbloqueó
                    if (currentProgress >= logro.objetivo && !logro.desbloqueado) {
                        logro.desbloqueado = true
                        logrosRef.child(logro.id).child("desbloqueado").setValue(true)
                        android.util.Log.d("LogrosActivity", "¡Logro desbloqueado: ${logro.titulo}!")
                        
                        AppLogger.d("¡Logro desbloqueado: ${logro.titulo}! Reclama tus puntos")
                    }
                }
                
                // Actualizar contadores
                var logrosDesbloqueados = 0
                for (logro in allLogrosList) {
                    if (logro.desbloqueado) {
                        logrosDesbloqueados++
                    }
                }
                
                textViewLogrosDesbloqueados.text = logrosDesbloqueados.toString()
                logrosAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                AppLogger.e("Error al verificar logros: ${error.message}")
            }
        })
    }
    
    // Método para calcular días consecutivos de reciclaje
    private fun calcularDiasConsecutivos(diasReciclaje: Set<String>): Int {
        if (diasReciclaje.isEmpty()) return 0
        
        // Convertir fechas a lista y ordenar
        val fechasOrdenadas = diasReciclaje.toList().sorted()
        
        var maxConsecutivos = 0
        var consecutivosActuales = 1
        
        for (i in 1 until fechasOrdenadas.size) {
            val fechaAnterior = fechasOrdenadas[i - 1]
            val fechaActual = fechasOrdenadas[i]
            
            // Verificar si son días consecutivos
            if (sonDiasConsecutivos(fechaAnterior, fechaActual)) {
                consecutivosActuales++
            } else {
                maxConsecutivos = maxOf(maxConsecutivos, consecutivosActuales)
                consecutivosActuales = 1
            }
        }
        
        return maxOf(maxConsecutivos, consecutivosActuales)
    }
    
    // Método auxiliar para verificar si dos fechas son días consecutivos
    private fun sonDiasConsecutivos(fecha1: String, fecha2: String): Boolean {
        return try {
            // Formato esperado: YYYY-MM-DD
            val partes1 = fecha1.split("-")
            val partes2 = fecha2.split("-")
            
            val año1 = partes1[0].toInt()
            val mes1 = partes1[1].toInt()
            val dia1 = partes1[2].toInt()
            
            val año2 = partes2[0].toInt()
            val mes2 = partes2[1].toInt()
            val dia2 = partes2[2].toInt()
            
            // Calcular diferencia en días
            val diferencia = calcularDiferenciaDias(año1, mes1, dia1, año2, mes2, dia2)
            diferencia == 1L
            
        } catch (e: Exception) {
            false
        }
    }
    
    // Método para calcular diferencia en días entre dos fechas
    private fun calcularDiferenciaDias(año1: Int, mes1: Int, dia1: Int, año2: Int, mes2: Int, dia2: Int): Long {
        val cal1 = Calendar.getInstance()
        cal1.set(año1, mes1 - 1, dia1)
        
        val cal2 = Calendar.getInstance()
        cal2.set(año2, mes2 - 1, dia2)
        
        val tiempo1 = cal1.timeInMillis
        val tiempo2 = cal2.timeInMillis
        
        val diferencia = kotlin.math.abs(tiempo2 - tiempo1)
        return diferencia / (24 * 60 * 60 * 1000)
    }

    // Método para forzar verificación de logros usando historial
    private fun forceCheckLogros() {
        val puntosRef = FirebaseDatabase.getInstance()
            .getReference("usuarios").child(FirebaseAuth.getInstance().currentUser!!.uid).child("puntos")
        
        puntosRef.get().addOnSuccessListener { snapshot ->
            var totalReciclajes = 0
            var totalPuntosGanados = 0
            val diasReciclaje = mutableSetOf<String>()
            
            // Calcular estadísticas desde el historial de puntos
            for (historialSnapshot in snapshot.children) {
                val historial = historialSnapshot.getValue(Historial::class.java)
                if (historial != null && historial.isGanado) {
                    // Solo contar entradas con tipo "ganado" (reciclajes reales)
                    totalReciclajes++
                    totalPuntosGanados += historial.cantidad
                    
                    // Agregar fecha para calcular días consecutivos
                    if (historial.fecha != null) {
                        val fecha = historial.fecha
                        val fechaStr = when (fecha) {
                            is com.google.firebase.Timestamp -> fecha.toDate().toString().substring(0, 10)
                            is Long -> java.util.Date(fecha).toString().substring(0, 10)
                            else -> fecha.toString().substring(0, 10)
                        }
                        diasReciclaje.add(fechaStr)
                    }
                }
            }
            
            // CORREGIDO: Verificar cada logro usando solo puntos ganados (no recompensas)
            for (logro in allLogrosList) {
                var currentProgress = 0
                
                when (logro.tipo) {
                    "reciclajes" -> currentProgress = totalReciclajes
                    "puntos" -> currentProgress = totalPuntosGanados
                    "dias_consecutivos" -> currentProgress = calcularDiasConsecutivos(diasReciclaje)
                }
                
                // Actualizar progreso si es necesario
                if (currentProgress != logro.progreso) {
                    logro.progreso = currentProgress
                    updateLogroProgress(logro.id, currentProgress)
                }
                
                // Verificar desbloqueo
                if (currentProgress >= logro.objetivo && !logro.desbloqueado) {
                    logro.desbloqueado = true
                    logrosRef.child(logro.id).child("desbloqueado").setValue(true)
                }
            }
            
            // Actualizar contadores
            var logrosDesbloqueados = 0
            for (logro in allLogrosList) {
                if (logro.desbloqueado) {
                    logrosDesbloqueados++
                }
            }
            
            textViewLogrosDesbloqueados.text = logrosDesbloqueados.toString()
            logrosAdapter.notifyDataSetChanged()
        }
    }

    // CORREGIDO: Método para sumar puntos de recompensa al usuario_puntos
    private fun addRewardPoints(points: Int) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userRef = FirebaseDatabase.getInstance().getReference("usuarios")
                .child(currentUser.uid)
            
            // CORREGIDO: Usar usuario_puntos (campo normalizado)
            // El backend se encargará de detectar si es recompensa y agregar al nodo "puntos" con punto_tipo:"recompensa"
            userRef.child("usuario_puntos").get().addOnSuccessListener { snapshot ->
                val currentPoints = snapshot.getValue(Int::class.java)
                val newPoints = (currentPoints ?: 0) + points
                userRef.child("usuario_puntos").setValue(newPoints)
            }
        }
    }
    
    // Método para ordenar logros con "Primer Reciclaje" siempre primero
    private fun ordenarLogros() {
        allLogrosList.sortWith { logro1, logro2 ->
            // "Primer Reciclaje" siempre va primero
            when {
                logro1.id == "primer_reciclaje" -> -1
                logro2.id == "primer_reciclaje" -> 1
                else -> {
                    // Luego ordenar por tipo y objetivo
                    val tipoComparison = logro1.tipo.compareTo(logro2.tipo)
                    if (tipoComparison != 0) tipoComparison else logro1.objetivo.compareTo(logro2.objetivo)
                }
            }
        }
    }
    
    // Método para actualizar la lista de logros mostrados
    private fun updateDisplayedLogros() {
        displayedLogrosList.clear()
        val countToShow = minOf(currentDisplayCount, allLogrosList.size)
        
        for (i in 0 until countToShow) {
            displayedLogrosList.add(allLogrosList[i])
        }
        
        // Actualizar la lista del adapter
        logrosList.clear()
        logrosList.addAll(displayedLogrosList)
        
        // Mostrar u ocultar el botón "Mostrar Más"
        if (currentDisplayCount < allLogrosList.size) {
            buttonMostrarMas.visibility = View.VISIBLE
            buttonMostrarMas.text = "📋 Mostrar Más Logros (${allLogrosList.size - currentDisplayCount} restantes)"
        } else {
            buttonMostrarMas.visibility = View.GONE
        }
    }
    
    // Método para mostrar más logros
    private fun showMoreLogros() {
        currentDisplayCount += INCREMENT_COUNT
        updateDisplayedLogros()
        logrosAdapter.notifyDataSetChanged()
        
        // Hacer scroll suave hacia abajo para mostrar los nuevos logros
        recyclerViewLogros.smoothScrollToPosition(logrosList.size - 1)
    }
    
    // Método para crear todos los logros posibles
    private fun createAllPossibleLogros(): List<Logro> {
        val allLogros = mutableListOf<Logro>()
        
        // Logros basados en reciclajes (cada reciclaje = 20-30 puntos)
        allLogros.add(Logro("primer_reciclaje", "Primer Reciclaje", "Completa tu primer reciclaje", "primer_reciclaje", 1, 10, "reciclajes"))
        allLogros.add(Logro("reciclador_novato", "Reciclador Novato", "Completa 3 reciclajes", "reciclaje", 3, 25, "reciclajes"))
        allLogros.add(Logro("reciclador_experimentado", "Reciclador Experimentado", "Completa 5 reciclajes", "reciclaje", 5, 50, "reciclajes"))
        allLogros.add(Logro("reciclador_experto", "Reciclador Experto", "Completa 10 reciclajes", "reciclador_experto", 10, 100, "reciclajes"))
        allLogros.add(Logro("reciclador_maestro", "Reciclador Maestro", "Completa 20 reciclajes", "reciclador_maestro", 20, 200, "reciclajes"))
        
        // Logros basados en puntos acumulados (20 pts plástico, 30 pts aluminio)
        allLogros.add(Logro("puntos_60", "Acumulador de Puntos", "Acumula 60 puntos (3 plásticos)", "puntos", 60, 20, "puntos"))
        allLogros.add(Logro("puntos_150", "Gran Acumulador", "Acumula 150 puntos (5 plásticos)", "puntos", 150, 50, "puntos"))
        allLogros.add(Logro("puntos_300", "Maestro Acumulador", "Acumula 300 puntos (10 plásticos)", "puntos", 300, 100, "puntos"))
        allLogros.add(Logro("puntos_600", "Experto Acumulador", "Acumula 600 puntos (20 plásticos)", "puntos", 600, 150, "puntos"))
        allLogros.add(Logro("puntos_1000", "Leyenda Acumulador", "Acumula 1000 puntos", "puntos", 1000, 200, "puntos"))
        
        // Logros de consistencia
        allLogros.add(Logro("dias_3", "Constante", "Recicla durante 3 días consecutivos", "dias_consecutivos", 3, 30, "dias_consecutivos"))
        allLogros.add(Logro("dias_7", "Perseverante", "Recicla durante 7 días consecutivos", "dias_consecutivos", 7, 75, "dias_consecutivos"))
        
        return allLogros
    }
}
