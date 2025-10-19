package com.example.tecreciclaje.domain.usecase

import com.example.tecreciclaje.domain.model.Usuario
import com.example.tecreciclaje.domain.repository.UserRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot

class GetUserUseCase(private val userRepository: UserRepository) {

    fun getCurrentUser(): FirebaseUser? = userRepository.getCurrentUser()

    fun getUserData(userId: String): Task<DocumentSnapshot> {
        return userRepository.getUserData(userId)
    }

    fun getUserAsObject(userId: String): Task<Usuario?> {
        return userRepository.getUserData(userId)
            .continueWith { task ->
                if (task.isSuccessful && task.result.exists()) {
                    task.result.toObject(Usuario::class.java)
                } else null
            }
    }

    fun isUserLoggedIn(): Boolean = getCurrentUser() != null

    fun getCurrentUserId(): String? = getCurrentUser()?.uid
}
