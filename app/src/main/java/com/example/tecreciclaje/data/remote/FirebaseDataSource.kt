package com.example.tecreciclaje.data.remote

import com.example.tecreciclaje.domain.model.Contenedor
import com.example.tecreciclaje.domain.model.Usuario
import com.example.tecreciclaje.domain.model.Vale
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class FirebaseDataSource {
    private val auth: FirebaseAuth
    private val firestore: FirebaseFirestore
    private val databaseRef: DatabaseReference

    init {
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        databaseRef = FirebaseDatabase.getInstance().reference
    }

    // Métodos de autenticación
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun getUserData(userId: String): Task<DocumentSnapshot> {
        return firestore.collection("usuarios").document(userId).get()
    }

    fun saveUserData(userId: String, usuario: Usuario): Task<Void> {
        return firestore.collection("usuarios").document(userId).set(usuario)
    }

    // Métodos para contenedores
    fun getContenedores(): Task<QuerySnapshot> {
        return firestore.collection("contenedores").get()
    }

    fun updateContenedor(contenedorId: String, contenedor: Contenedor): Task<Void> {
        return firestore.collection("contenedores").document(contenedorId).set(contenedor)
    }

    // Métodos para vales
    fun getUserVales(userId: String): Task<QuerySnapshot> {
        return firestore.collection("vales").whereEqualTo("userId", userId).get()
    }

    fun saveVale(vale: Vale): Task<DocumentReference> {
        return firestore.collection("vales").add(vale)
    }

    // Métodos para estadísticas
    fun getEstadisticas(): Task<QuerySnapshot> {
        return firestore.collection("estadisticas").get()
    }

    // Métodos para historial
    fun getUserHistorial(userId: String): Task<QuerySnapshot> {
        return firestore.collection("historial").whereEqualTo("userId", userId).get()
    }
}
