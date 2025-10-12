package com.costura.pro.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.costura.pro.databinding.ActivityMainBinding
import com.costura.pro.utils.AppPreferences

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var preferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferences = AppPreferences(this)

        // Check if user is already logged in
        if (preferences.isLoggedIn) {
            redirectToDashboard()
            return
        }

        setupClickListeners()
    }
    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun redirectToDashboard() {
        val intent = when (preferences.userRole) {
            "ADMIN" -> Intent(this, com.costura.pro.ui.admin.AdminDashboardActivity::class.java)
            "WORKER" -> Intent(this, com.costura.pro.ui.worker.WorkerDashboardActivity::class.java)
            else -> Intent(this, LoginActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}