package com.example.tecreciclaje.domain.repository

import com.example.tecreciclaje.data.remote.FirebaseDataSource
import com.example.tecreciclaje.domain.model.Usuario
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot

class UserRepository(private val firebaseDataSource: FirebaseDataSource) {

    fun getCurrentUser(): FirebaseUser? = firebaseDataSource.getCurrentUser()

    fun getUserData(userId: String): Task<DocumentSnapshot> {
        return firebaseDataSource.getUserData(userId)
    }

    fun saveUserData(userId: String, usuario: Usuario): Task<Void> {
        return firebaseDataSource.saveUserData(userId, usuario)
    }

    fun updateUserProfile(
        userId: String,
        nombre: String,
        apellido: String,
        numControl: String,
        carrera: String,
        perfil: String
    ): Task<Void> {
        val usuario = Usuario().apply {
            usuario_id = userId
            usuario_nombre = nombre
            usuario_apellido = apellido
            usuario_numControl = numControl
            usuario_carrera = carrera
            usuario_perfil = perfil
        }
        
        return firebaseDataSource.saveUserData(userId, usuario)
    }

    fun updateUserPoints(userId: String, puntos: Int): Task<Void>? {
        return firebaseDataSource.getUserData(userId)
            .continueWithTask { task ->
                if (task.isSuccessful && task.result.exists()) {
                    val usuario = task.result.toObject(Usuario::class.java)
                    if (usuario != null) {
                        usuario.usuario_puntos = puntos
                        firebaseDataSource.saveUserData(userId, usuario)
                    } else null
                } else null
            }
    }

    fun updateNfcUid(userId: String, nfcUid: String): Task<Void>? {
        return firebaseDataSource.getUserData(userId)
            .continueWithTask { task ->
                if (task.isSuccessful && task.result.exists()) {
                    val usuario = task.result.toObject(Usuario::class.java)
                    if (usuario != null) {
                        usuario.usuario_nfcUid = nfcUid
                        firebaseDataSource.saveUserData(userId, usuario)
                    } else null
                } else null
            }
    }
}
