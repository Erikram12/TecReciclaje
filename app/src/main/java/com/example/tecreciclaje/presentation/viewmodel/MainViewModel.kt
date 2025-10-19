package com.example.tecreciclaje.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.tecreciclaje.domain.model.Usuario
import com.example.tecreciclaje.domain.usecase.GetUserUseCase

class MainViewModel(private val getUserUseCase: GetUserUseCase) : ViewModel() {
    
    private val _currentUser = MutableLiveData<Usuario>()
    val currentUser: LiveData<Usuario> = _currentUser
    
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    private val _userRole = MutableLiveData<String?>()
    val userRole: LiveData<String?> = _userRole

    fun checkUserAuthentication() {
        _isLoading.value = true
        
        val firebaseUser = getUserUseCase.getCurrentUser()
        if (firebaseUser != null) {
            loadUserData(firebaseUser.uid)
        } else {
            _isLoading.value = false
            _errorMessage.value = "Usuario no autenticado"
        }
    }

    private fun loadUserData(userId: String) {
        getUserUseCase.getUserAsObject(userId)
            .addOnSuccessListener { usuario ->
                _currentUser.value = usuario
                usuario?.let {
                    _userRole.value = it.usuario_role
                }
                _isLoading.value = false
            }
            .addOnFailureListener { exception ->
                _errorMessage.value = "Error al cargar datos del usuario: ${exception.message}"
                _isLoading.value = false
            }
    }

    fun isUserLoggedIn(): Boolean {
        return getUserUseCase.isUserLoggedIn()
    }

    fun getCurrentUserId(): String? {
        return getUserUseCase.getCurrentUserId()
    }

    fun isAdmin(): Boolean {
        val usuario = _currentUser.value
        return usuario?.isAdmin == true
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
