package com.example.tecreciclaje.di

import com.example.tecreciclaje.data.remote.FirebaseDataSource
import com.example.tecreciclaje.domain.repository.UserRepository
import com.example.tecreciclaje.domain.usecase.GetUserUseCase
import com.example.tecreciclaje.presentation.viewmodel.MainViewModel

object DependencyInjection {
    private var firebaseDataSource: FirebaseDataSource? = null
    private var userRepository: UserRepository? = null
    private var getUserUseCase: GetUserUseCase? = null

    fun provideFirebaseDataSource(): FirebaseDataSource {
        if (firebaseDataSource == null) {
            firebaseDataSource = FirebaseDataSource()
        }
        return firebaseDataSource!!
    }

    fun provideUserRepository(): UserRepository {
        if (userRepository == null) {
            userRepository = UserRepository(provideFirebaseDataSource())
        }
        return userRepository!!
    }

    fun provideGetUserUseCase(): GetUserUseCase {
        if (getUserUseCase == null) {
            getUserUseCase = GetUserUseCase(provideUserRepository())
        }
        return getUserUseCase!!
    }

    fun provideMainViewModel(): MainViewModel {
        return MainViewModel(provideGetUserUseCase())
    }

    // Método para limpiar las dependencias (útil para testing)
    fun clear() {
        firebaseDataSource = null
        userRepository = null
        getUserUseCase = null
    }
}
