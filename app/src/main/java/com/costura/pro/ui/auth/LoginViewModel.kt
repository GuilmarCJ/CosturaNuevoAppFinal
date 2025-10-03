package com.costura.pro.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costura.pro.data.repository.UserRepository
import com.costura.pro.utils.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    fun loginUser(username: String, password: String) {
        _loginState.value = LoginState.Loading

        viewModelScope.launch {
            // Check admin credentials first
            if (username == Constants.ADMIN_USERNAME && password == Constants.ADMIN_PASSWORD) {
                _loginState.value = LoginState.Success(
                    userId = "admin",
                    username = Constants.ADMIN_USERNAME,
                    role = "ADMIN"
                )
                return@launch
            }

            // Check worker credentials
            val user = userRepository.authenticateUser(username, password)
            if (user != null) {
                _loginState.value = LoginState.Success(
                    userId = user.id,
                    username = user.username,
                    role = user.role.name
                )
            } else {
                _loginState.value = LoginState.Error("Usuario o contraseña incorrectos")
            }
        }
    }

    sealed class LoginState {
        object Idle : LoginState()
        object Loading : LoginState()
        data class Success(val userId: String, val username: String, val role: String) : LoginState()
        data class Error(val message: String) : LoginState()
    }
}