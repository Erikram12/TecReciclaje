package com.example.tecreciclaje.utils

import android.content.Context
import android.widget.Toast
import com.example.tecreciclaje.domain.model.Logro
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LogrosManager private constructor(context: Context) {
    
    private val logrosRef: DatabaseReference?
    private val logrosList = mutableListOf<Logro>()
    private val context: Context
    
    init {
        this.context = context
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            logrosRef = FirebaseDatabase.getInstance().getReference("usuarios")
                .child(currentUser.uid).child("logros")
            loadLogros()
        } else {
            logrosRef = null
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: LogrosManager? = null
        
        fun getInstance(context: Context): LogrosManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LogrosManager(context).also { INSTANCE = it }
            }
        }
    }
    
    private fun loadLogros() {
        logrosRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                logrosList.clear()
                if (snapshot.exists()) {
                    for (logroSnapshot in snapshot.children) {
                        val logro = logroSnapshot.getValue(Logro::class.java)
                        if (logro != null) {
                            logrosList.add(logro)
                        }
                    }
                } else {
                    createDefaultLogros()
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                // Manejar error
            }
        })
    }
    
    private fun createDefaultLogros() {
        val defaultLogros = listOf(
            // Logros basados en reciclajes (cada reciclaje = 20-30 puntos)
            Logro("primer_reciclaje", "Primer Reciclaje", "Completa tu primer reciclaje", "primer_reciclaje", 1, 10, "reciclajes"),
            Logro("reciclador_novato", "Reciclador Novato", "Completa 3 reciclajes", "reciclaje", 3, 25, "reciclajes"),
            Logro("reciclador_experimentado", "Reciclador Experimentado", "Completa 5 reciclajes", "reciclaje", 5, 50, "reciclajes"),
            Logro("reciclador_experto", "Reciclador Experto", "Completa 10 reciclajes", "reciclador_experto", 10, 100, "reciclajes"),
            Logro("reciclador_maestro", "Reciclador Maestro", "Completa 20 reciclajes", "reciclador_maestro", 20, 200, "reciclajes"),
            
            // Logros basados en puntos acumulados (20 pts plástico, 30 pts aluminio)
            Logro("puntos_60", "Acumulador de Puntos", "Acumula 60 puntos (3 plásticos)", "puntos", 60, 20, "puntos"),
            Logro("puntos_150", "Gran Acumulador", "Acumula 150 puntos (5 plásticos)", "puntos", 150, 50, "puntos"),
            Logro("puntos_300", "Maestro Acumulador", "Acumula 300 puntos (10 plásticos)", "puntos", 300, 100, "puntos"),
            Logro("puntos_600", "Experto Acumulador", "Acumula 600 puntos (20 plásticos)", "puntos", 600, 150, "puntos"),
            Logro("puntos_1000", "Leyenda Acumulador", "Acumula 1000 puntos", "puntos", 1000, 200, "puntos"),
            
            // Logros de consistencia
            Logro("dias_3", "Constante", "Recicla durante 3 días consecutivos", "dias_consecutivos", 3, 30, "dias_consecutivos"),
            Logro("dias_7", "Perseverante", "Recicla durante 7 días consecutivos", "dias_consecutivos", 7, 75, "dias_consecutivos")
        )
        
        defaultLogros.forEach { logro ->
            logrosRef?.child(logro.id)?.setValue(logro)
        }
        
        logrosList.addAll(defaultLogros)
    }
    
    fun checkReciclajeLogros(totalReciclajes: Int) {
        logrosList.filter { it.tipo == "reciclajes" }.forEach { logro ->
            val currentProgress = logro.progreso
            if (totalReciclajes > currentProgress) {
                updateLogroProgress(logro.id, totalReciclajes)
                
                // Verificar si se desbloqueó un nuevo logro
                if (totalReciclajes >= logro.objetivo && !logro.desbloqueado) {
                    showLogroUnlocked(logro)
                }
            }
        }
    }
    
    fun checkPuntosLogros(puntosTotales: Int) {
        logrosList.filter { it.tipo == "puntos" }.forEach { logro ->
            val currentProgress = logro.progreso
            if (puntosTotales > currentProgress) {
                updateLogroProgress(logro.id, puntosTotales)
                
                // Verificar si se desbloqueó un nuevo logro
                if (puntosTotales >= logro.objetivo && !logro.desbloqueado) {
                    showLogroUnlocked(logro)
                }
            }
        }
    }
    
    fun checkDiasConsecutivosLogros(diasConsecutivos: Int) {
        logrosList.filter { it.tipo == "dias_consecutivos" }.forEach { logro ->
            val currentProgress = logro.progreso
            if (diasConsecutivos > currentProgress) {
                updateLogroProgress(logro.id, diasConsecutivos)
                
                // Verificar si se desbloqueó un nuevo logro
                if (diasConsecutivos >= logro.objetivo && !logro.desbloqueado) {
                    showLogroUnlocked(logro)
                }
            }
        }
    }
    
    private fun updateLogroProgress(logroId: String, newProgress: Int) {
        logrosRef?.child(logroId)?.child("progreso")?.setValue(newProgress)
    }
    
    private fun showLogroUnlocked(logro: Logro) {
        Toast.makeText(context, "🏆 ¡Logro desbloqueado: ${logro.titulo}!", Toast.LENGTH_LONG).show()
        
        // Aquí podrías agregar una notificación push o una animación
        // También podrías agregar los puntos de recompensa al usuario
        addRewardPoints(logro.recompensa)
    }
    
    // Método para sumar puntos de recompensa al usuario_puntos
    private fun addRewardPoints(points: Int) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userRef = FirebaseDatabase.getInstance().getReference("usuarios")
                .child(currentUser.uid)
            
            // Usar usuario_puntos (campo normalizado)
            // El backend se encargará de detectar si es recompensa y agregar al nodo "puntos" con punto_tipo:"recompensa"
            userRef.child("usuario_puntos").get().addOnSuccessListener { snapshot ->
                val currentPoints = snapshot.getValue(Int::class.java) ?: 0
                val newPoints = currentPoints + points
                userRef.child("usuario_puntos").setValue(newPoints)
            }
        }
    }
    
    fun checkAllLogros(totalReciclajes: Int, puntosTotales: Int, diasConsecutivos: Int) {
        checkReciclajeLogros(totalReciclajes)
        checkPuntosLogros(puntosTotales)
        checkDiasConsecutivosLogros(diasConsecutivos)
    }
    
    // Método para verificar logros después de un reciclaje específico
    fun checkLogrosAfterRecycle(materialType: String, puntosGanados: Int) {
        // Verificar logros de puntos basados en el material reciclado
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userRef = FirebaseDatabase.getInstance().getReference("usuarios")
                .child(currentUser.uid)
            
            // Usar usuario_puntos (campo normalizado)
            userRef.child("usuario_puntos").get().addOnSuccessListener { snapshot ->
                val currentPoints = snapshot.getValue(Int::class.java) ?: 0
                val totalPoints = currentPoints + puntosGanados
                
                // Verificar logros de puntos
                checkPuntosLogros(totalPoints)
                
                // Mostrar mensaje específico según el material
                val materialName = if (materialType == "plastico") "plástico" else "aluminio"
                Toast.makeText(context, "♻️ Reciclaje de $materialName completado (+$puntosGanados pts)", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
