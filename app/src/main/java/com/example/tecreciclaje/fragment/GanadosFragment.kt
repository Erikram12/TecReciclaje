package com.example.tecreciclaje.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tecreciclaje.Model.HistorialAdapter
import com.example.tecreciclaje.R
import com.example.tecreciclaje.domain.model.Historial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class GanadosFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private val listaHistorial = mutableListOf<Historial>()
    private lateinit var adapter: HistorialAdapter
    private var user: FirebaseUser? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_historial, container, false)

        recyclerView = view.findViewById(R.id.recyclerHistorial)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = HistorialAdapter(listaHistorial)
        recyclerView.adapter = adapter

        user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            cargarHistorialGanado(user!!.uid)
        }

        return view
    }

    private fun cargarHistorialGanado(uid: String) {
        val ref = FirebaseDatabase.getInstance()
            .getReference("usuarios").child(uid).child("puntos")

        // CORREGIDO: Mostrar tanto puntos ganados como recompensas
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaHistorial.clear()
                Log.d("GanadosFragment", "Consulta ejecutada. Encontrados: ${snapshot.childrenCount} registros")
                
                for (s in snapshot.children) {
                    Log.d("GanadosFragment", "Procesando registro: ${s.key} = ${s.value}")
                    val model = s.getValue(Historial::class.java)
                    if (model != null) {
                        val tipo = model.getTipo
                        Log.d("GanadosFragment", "Tipo obtenido: '$tipo', historial_tipo: '${model.historial_tipo}', punto_tipo: '${model.punto_tipo}'")
                        // NUEVO: Mostrar puntos ganados Y recompensas
                        if (tipo == "ganado" || tipo == "recompensa") {
                            Log.d("GanadosFragment", "Registro cargado: ${model.getCantidad} pts, tipo: $tipo")
                            listaHistorial.add(0, model) // Agrega al inicio
                        }
                    } else {
                        Log.w("GanadosFragment", "No se pudo convertir registro: ${s.value}")
                    }
                }
                
                Log.d("GanadosFragment", "Total en lista: ${listaHistorial.size}")
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("GanadosFragment", "Error en consulta: ${error.message}")
            }
        })
    }
}
