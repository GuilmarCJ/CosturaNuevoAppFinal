package com.costura.pro.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.costura.pro.data.model.UserRole
import com.costura.pro.databinding.ActivityLoginBinding
import com.costura.pro.ui.admin.AdminDashboardActivity
import com.costura.pro.ui.worker.WorkerDashboardActivity
import com.costura.pro.utils.AppPreferences
import com.costura.pro.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var preferences: AppPreferences
    private val db = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(Dispatchers.Main)

    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferences = AppPreferences(this)
        setupClickListeners()

        // DEBUG: Mostrar datos de prueba
        Log.d(TAG, "🔍 App iniciada - Lista para login")
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            Log.d(TAG, "🔄 Intentando login con usuario: $username")

            if (validateInputs(username, password)) {
                loginUser(username, password)
            }
        }
    }

    private fun validateInputs(username: String, password: String): Boolean {
        if (username.isEmpty()) {
            binding.etUsername.error = "Ingresa tu usuario"
            return false
        }

        if (password.isEmpty()) {
            binding.etPassword.error = "Ingresa tu contraseña"
            return false
        }

        return true
    }

    private fun loginUser(username: String, password: String) {
        showLoading(true)

        // Check if it's admin login (LOCAL - no necesita Firebase)
        if (username == Constants.ADMIN_USERNAME && password == Constants.ADMIN_PASSWORD) {
            Log.d(TAG, "✅ Login ADMIN exitoso")
            handleAdminLogin()
            return
        }

        // Check worker login in Firestore con NUEVA ESTRUCTURA
        scope.launch {
            try {
                Log.d(TAG, "🔍 Buscando usuario en Firebase: $username")

                val documents = db.collection(Constants.COLLECTION_USERS)
                    .whereEqualTo("basicInfo.username", username)
                    .whereEqualTo("basicInfo.password", password)
                    .get()
                    .await()

                Log.d(TAG, "📊 Resultados de búsqueda: ${documents.size()} documentos")

                if (documents.isEmpty) {
                    showLoading(false)
                    Log.w(TAG, "❌ Usuario no encontrado o credenciales incorrectas")
                    Toast.makeText(
                        this@LoginActivity,
                        "Usuario o contraseña incorrectos",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                val document = documents.documents[0]
                Log.d(TAG, "📄 Documento encontrado: ${document.id}")

                // Obtener datos del mapa basicInfo
                val basicInfo = document.get("basicInfo") as? Map<String, Any>

                if (basicInfo == null) {
                    showLoading(false)
                    Log.e(TAG, "❌ Estructura basicInfo no encontrada")
                    Toast.makeText(
                        this@LoginActivity,
                        "Error: Estructura de usuario inválida",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                // Extraer datos del usuario
                val userId = document.id
                val userRole = basicInfo["role"] as? String ?: "WORKER"
                val userName = basicInfo["name"] as? String ?: "Trabajador"
                val userUsername = basicInfo["username"] as? String ?: ""

                Log.d(TAG, "✅ Usuario validado:")
                Log.d(TAG, "   - ID: $userId")
                Log.d(TAG, "   - Nombre: $userName")
                Log.d(TAG, "   - Username: $userUsername")
                Log.d(TAG, "   - Rol: $userRole")

                handleUserLogin(userId, userName, userRole, userUsername)

            } catch (exception: Exception) {
                showLoading(false)
                Log.e(TAG, "❌ Error en login: ${exception.message}", exception)
                Toast.makeText(
                    this@LoginActivity,
                    "Error de conexión: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun handleAdminLogin() {
        preferences.isLoggedIn = true
        preferences.userId = "admin"
        preferences.userRole = UserRole.ADMIN.name
        preferences.username = Constants.ADMIN_USERNAME

        showLoading(false)

        Log.d(TAG, "🚀 Redirigiendo a AdminDashboard")
        val intent = Intent(this, AdminDashboardActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun handleUserLogin(userId: String, name: String, role: String, username: String) {
        preferences.isLoggedIn = true
        preferences.userId = userId
        preferences.userRole = role
        preferences.username = name

        showLoading(false)

        Log.d(TAG, "🚀 Redirigiendo usuario: $name ($role)")

        val intent = if (role == "ADMIN") {
            Intent(this, AdminDashboardActivity::class.java)
        } else {
            Intent(this, WorkerDashboardActivity::class.java)
        }

        startActivity(intent)
        finish()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        binding.btnLogin.isEnabled = !show

        if (show) {
            Log.d(TAG, "⏳ Mostrando loading...")
        } else {
            Log.d(TAG, "✅ Ocultando loading...")
        }
    }
}