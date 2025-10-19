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

class CanjeadosFragment : Fragment() {

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
            cargarHistorial("canjeado")
        }

        return view
    }

    private fun cargarHistorial(tipo: String) {
        // CORREGIDO: Usar nodo "historial" para puntos canjeados
        val ref = FirebaseDatabase.getInstance()
            .getReference("usuarios").child(user!!.uid).child("historial")

        // CORREGIDO: Filtrar por historial_tipo = "canjeado"
        ref.orderByChild("historial_tipo").equalTo(tipo)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    listaHistorial.clear()
                    Log.d("CanjeadosFragment", "Consulta ejecutada. Encontrados: ${snapshot.childrenCount} registros")
                    
                    for (s in snapshot.children) {
                        Log.d("CanjeadosFragment", "Procesando registro: ${s.key} = ${s.value}")
                        val item = s.getValue(Historial::class.java)
                        if (item != null) {
                            Log.d("CanjeadosFragment", "Tipo obtenido: '${item.getTipo}', historial_tipo: '${item.historial_tipo}', punto_tipo: '${item.punto_tipo}'")
                            Log.d("CanjeadosFragment", "Registro cargado: ${item.getCantidad} pts, tipo: ${item.getTipo}")
                            listaHistorial.add(item)
                        } else {
                            Log.w("CanjeadosFragment", "No se pudo convertir registro: ${s.value}")
                        }
                    }
                    
                    Log.d("CanjeadosFragment", "Total en lista: ${listaHistorial.size}")
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("CanjeadosFragment", "Error en consulta: ${error.message}")
                }
            })
    }
}
