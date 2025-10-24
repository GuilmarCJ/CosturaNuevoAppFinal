package com.costura.pro.ui.worker

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.costura.pro.data.model.Operation
import com.costura.pro.databinding.ActivityWorkerDashboardBinding
import com.costura.pro.ui.attendance.AttendanceActivity
import com.costura.pro.ui.auth.LoginActivity
import com.costura.pro.utils.AppPreferences
import com.costura.pro.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class WorkerDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorkerDashboardBinding
    private lateinit var preferences: AppPreferences
    private val db = FirebaseFirestore.getInstance()
    private val operationsList = mutableListOf<Operation>()
    private lateinit var operationsAdapter: WorkerOperationsAdapter

    // NUEVO: Scope para corrutinas
    private val scope = CoroutineScope(Dispatchers.Main)

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
        binding.btnAttendance.setOnClickListener {
            val intent = Intent(this, AttendanceActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadOperations() {
        showLoading(true)

        scope.launch {
            try {
                val documents = db.collection(Constants.COLLECTION_OPERATIONS)
                    .whereEqualTo("isActive", true)
                    .get()
                    .await()

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
            } catch (exception: Exception) {
                showLoading(false)
                Toast.makeText(this@WorkerDashboardActivity, "Error cargando operaciones: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
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
        val workerName = preferences.username ?: ""

        scope.launch {
            try {
                val productionRepository = (application as com.costura.pro.CosturaProApp).productionRepository
                val success = productionRepository.registerProduction(
                    workerId = workerId,
                    operationId = operation.id,
                    operationName = operation.name,
                    paymentPerUnit = operation.paymentPerUnit,
                    quantity = quantity
                )

                if (success) {
                    Toast.makeText(this@WorkerDashboardActivity, "Registrado: +S/ ${String.format("%.2f", quantity * operation.paymentPerUnit)}", Toast.LENGTH_SHORT).show()
                    loadTotalEarned()
                    operationsAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this@WorkerDashboardActivity, "Error registrando producción", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@WorkerDashboardActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadTotalEarned() {
        val workerId = preferences.userId ?: return

        scope.launch {
            try {
                val productionRepository = (application as com.costura.pro.CosturaProApp).productionRepository
                val totalEarnings = productionRepository.getTotalEarnings(workerId) ?: 0.0
                binding.tvTotalEarned.text = "S/ ${String.format("%.2f", totalEarnings)}"
            } catch (e: Exception) {
                binding.tvTotalEarned.text = "S/ 0.00"
            }
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