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

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var preferences: AppPreferences
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferences = AppPreferences(this)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

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

        // Check if it's admin login
        if (username == Constants.ADMIN_USERNAME && password == Constants.ADMIN_PASSWORD) {
            handleAdminLogin()
            return
        }

        // Check worker login in Firestore
        db.collection(Constants.COLLECTION_USERS)
            .whereEqualTo("username", username)
            .whereEqualTo("password", password)
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    showLoading(false)
                    Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val user = documents.documents[0]
                handleWorkerLogin(user.id, user.getString("username") ?: "")
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Toast.makeText(this, "Error al iniciar sesión: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun handleAdminLogin() {
        preferences.isLoggedIn = true
        preferences.userId = "admin"
        preferences.userRole = UserRole.ADMIN.name
        preferences.username = Constants.ADMIN_USERNAME

        showLoading(false)
        val intent = Intent(this, AdminDashboardActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun handleWorkerLogin(userId: String, username: String) {
        preferences.isLoggedIn = true
        preferences.userId = userId
        preferences.userRole = UserRole.WORKER.name
        preferences.username = username

        showLoading(false)
        val intent = Intent(this, WorkerDashboardActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        binding.btnLogin.isEnabled = !show
    }
}