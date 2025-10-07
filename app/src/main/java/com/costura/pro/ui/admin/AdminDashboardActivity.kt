package com.costura.pro.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.costura.pro.databinding.ActivityAdminDashboardBinding
import com.costura.pro.ui.attendance.QRGeneratorActivity
import com.costura.pro.ui.auth.LoginActivity
import com.costura.pro.utils.AppPreferences
import com.costura.pro.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var preferences: AppPreferences
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferences = AppPreferences(this)
        setupUI()
        setupClickListeners()
        loadDashboardData()
    }

    private fun setupUI() {
        // Set current date
        val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        binding.tvDate.text = "HOY - $currentDate"
    }

    private fun setupClickListeners() {
        binding.btnManageWorkers.setOnClickListener {
            val intent = Intent(this, ManageWorkersActivity::class.java)
            startActivity(intent)
        }

        binding.btnViewOperations.setOnClickListener {
            val intent = Intent(this, ManageOperationsActivity::class.java)
            startActivity(intent)
        }

        binding.btnSettings.setOnClickListener {
            Toast.makeText(this, "Configuración - Próximamente", Toast.LENGTH_SHORT).show()
        }

        binding.btnLogout.setOnClickListener {
            logout()
        }

        binding.btnGenerateQR.setOnClickListener {
            val intent = Intent(this, QRGeneratorActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadDashboardData() {
        // Load active workers count
        db.collection(Constants.COLLECTION_USERS)
            .whereEqualTo("role", "WORKER")
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { documents ->
                val activeWorkers = documents.size()
                binding.tvActiveWorkers.text = "$activeWorkers activos"

                // Load total payment (you might want to calculate this based on production)
                calculateTotalPayment()
            }
            .addOnFailureListener { exception ->
                binding.tvActiveWorkers.text = "Error"
                Toast.makeText(this, "Error cargando datos: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun calculateTotalPayment() {
        // This is a simplified calculation - you might want to implement more complex logic
        db.collection(Constants.COLLECTION_PRODUCTION)
            .get()
            .addOnSuccessListener { documents ->
                var totalPayment = 0.0
                for (document in documents) {
                    totalPayment += document.getDouble("totalPayment") ?: 0.0
                }
                binding.tvTotalPayment.text = "S/ ${String.format("%.2f", totalPayment)}"
            }
            .addOnFailureListener { exception ->
                binding.tvTotalPayment.text = "S/ 0.00"
            }
    }

    private fun logout() {
        preferences.clear()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}