package com.costura.pro.ui.worker

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.costura.pro.data.model.Operation
import com.costura.pro.databinding.ActivityWorkerDashboardBinding
import com.costura.pro.ui.auth.LoginActivity
import com.costura.pro.utils.AppPreferences
import com.costura.pro.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class WorkerDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorkerDashboardBinding
    private lateinit var preferences: AppPreferences
    private val db = FirebaseFirestore.getInstance()
    private val operationsList = mutableListOf<Operation>()
    private lateinit var operationsAdapter: WorkerOperationsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkerDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferences = AppPreferences(this)
        setupUI()
        setupClickListeners()
        loadOperations()
        loadTotalEarned()
    }

    private fun setupUI() {
        val username = preferences.username ?: "TRABAJADOR"
        binding.tvWelcome.text = "¡HOLA ${username.uppercase()}!"
    }

    private fun setupClickListeners() {
        binding.btnLogout.setOnClickListener {
            logout()
        }
    }

    private fun loadOperations() {
        showLoading(true)
        db.collection(Constants.COLLECTION_OPERATIONS)
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { documents ->
                operationsList.clear()
                for (document in documents) {
                    val operation = Operation(
                        id = document.id,
                        name = document.getString("name") ?: "",
                        paymentPerUnit = document.getDouble("paymentPerUnit") ?: 0.0,
                        isActive = document.getBoolean("isActive") ?: true
                    )
                    operationsList.add(operation)
                }

                setupOperationsRecyclerView()
                showLoading(false)
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Toast.makeText(this, "Error cargando operaciones: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupOperationsRecyclerView() {
        operationsAdapter = WorkerOperationsAdapter(operationsList, preferences.userId ?: "") { operation, quantity ->
            registerProduction(operation, quantity)
        }
        binding.rvOperations.apply {
            layoutManager = LinearLayoutManager(this@WorkerDashboardActivity)
            adapter = operationsAdapter
        }
    }

    private fun registerProduction(operation: Operation, quantity: Int) {
        val workerId = preferences.userId ?: return
        val totalPayment = quantity * operation.paymentPerUnit

        val productionData = hashMapOf(
            "workerId" to workerId,
            "operationId" to operation.id,
            "operationName" to operation.name,
            "quantity" to quantity,
            "paymentPerUnit" to operation.paymentPerUnit,
            "totalPayment" to totalPayment,
            "date" to com.google.firebase.Timestamp.now()
        )

        db.collection(Constants.COLLECTION_PRODUCTION)
            .add(productionData)
            .addOnSuccessListener {
                Toast.makeText(this, "Registrado: +S/ ${String.format("%.2f", totalPayment)}", Toast.LENGTH_SHORT).show()
                loadTotalEarned()
                operationsAdapter.notifyDataSetChanged() // Refresh today's counts
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error registrando producción: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadTotalEarned() {
        val workerId = preferences.userId ?: return

        db.collection(Constants.COLLECTION_PRODUCTION)
            .whereEqualTo("workerId", workerId)
            .get()
            .addOnSuccessListener { documents ->
                var totalEarned = 0.0
                for (document in documents) {
                    totalEarned += document.getDouble("totalPayment") ?: 0.0
                }
                binding.tvTotalEarned.text = "S/ ${String.format("%.2f", totalEarned)}"
            }
            .addOnFailureListener { exception ->
                binding.tvTotalEarned.text = "S/ 0.00"
            }
    }

    private fun logout() {
        preferences.clear()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
    }
}