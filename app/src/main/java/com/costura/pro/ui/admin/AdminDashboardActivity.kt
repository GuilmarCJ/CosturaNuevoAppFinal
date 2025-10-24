package com.costura.pro.ui.admin

import androidx.core.content.ContextCompat
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.costura.pro.databinding.ActivityAdminDashboardBinding
import com.costura.pro.ui.attendance.QRGeneratorActivity
import com.costura.pro.ui.auth.LoginActivity
import com.costura.pro.utils.AppPreferences
import com.costura.pro.utils.Constants
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.text.SimpleDateFormat
import java.util.*
import com.costura.pro.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.tasks.await
import java.util.*

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var preferences: AppPreferences
    private val db = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(Dispatchers.Main)

    companion object {
        private const val TAG = "AdminDashboard"
    }

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

        binding.btnManageMachines.setOnClickListener {
            val intent = Intent(this, ManageMachinesActivity::class.java)
            startActivity(intent)
        }
        binding.btnExcel.setOnClickListener {
            val intent = Intent(this, ExcelActivity::class.java)
            startActivity(intent)
        }

        binding.btnNotes.setOnClickListener {
            val intent = Intent(this, NotesActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadDashboardData() {
        showWorkersLoading(true)

        scope.launch {
            try {
                // Cargar trabajadores y sus datos
                loadWorkersWithDailyProgress()

            } catch (e: Exception) {
                Log.e(TAG, "Error cargando dashboard: ${e.message}", e)
                showWorkersLoading(false)
                Toast.makeText(this@AdminDashboardActivity, "Error cargando datos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun loadWorkersWithDailyProgress() {
        try {
            // Obtener todos los trabajadores
            val workersSnapshot = db.collection(Constants.COLLECTION_USERS)
                .whereEqualTo("basicInfo.role", "WORKER")
                .get()
                .await()

            val activeWorkers = workersSnapshot.size()
            binding.tvActiveWorkers.text = "$activeWorkers activos"

            if (activeWorkers == 0) {
                showNoWorkersMessage(true)
                showWorkersLoading(false)
                return
            }

            showNoWorkersMessage(false)
            binding.containerWorkers.removeAllViews()

            val today = DateTime().toString(Constants.DATE_FORMAT)
            val startOfDay = DateTime().withTimeAtStartOfDay().millis

            for (workerDoc in workersSnapshot) {
                val workerId = workerDoc.id
                val basicInfo = workerDoc.get("basicInfo") as? Map<String, Any>
                val workerName = basicInfo?.get("name") as? String ?: "Sin nombre"
                val workerModality = basicInfo?.get("modality") as? String ?: "PIECE_RATE"

                // Obtener asistencia de hoy
                val attendanceToday = getTodayAttendance(workerId, today)

                // Obtener producción de hoy
                val todayProduction = getTodayProduction(workerId, startOfDay)

                // Calcular ganancias de hoy
                val dailyEarnings = calculateDailyEarnings(todayProduction)

                // Mostrar trabajador en la UI
                displayWorkerProgress(
                    workerName,
                    workerModality,
                    attendanceToday,
                    todayProduction,
                    dailyEarnings
                )
            }

            showWorkersLoading(false)

        } catch (e: Exception) {
            Log.e(TAG, "Error cargando trabajadores: ${e.message}", e)
            showWorkersLoading(false)
            showNoWorkersMessage(true)
        }
    }

    private suspend fun getTodayAttendance(workerId: String, today: String): Map<String, Any>? {
        return try {
            val attendanceSnapshot = db.collection(Constants.COLLECTION_USERS)
                .document(workerId)
                .collection(Constants.SUBCOLLECTION_ATTENDANCE)
                .whereEqualTo("date", today)
                .get()
                .await()

            if (!attendanceSnapshot.isEmpty) {
                val attendanceDoc = attendanceSnapshot.documents[0]
                attendanceDoc.data
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo asistencia: ${e.message}")
            null
        }
    }

    private suspend fun getTodayProduction(workerId: String, startOfDay: Long): List<Map<String, Any>> {
        return try {
            val productionSnapshot = db.collection(Constants.COLLECTION_USERS)
                .document(workerId)
                .collection(Constants.SUBCOLLECTION_PRODUCTION)
                .whereGreaterThanOrEqualTo("date", Timestamp(Date(startOfDay)))
                .get()
                .await()

            productionSnapshot.documents.map { it.data ?: emptyMap() }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo producción: ${e.message}")
            emptyList()
        }
    }

    private fun calculateDailyEarnings(production: List<Map<String, Any>>): Double {
        var totalEarnings = 0.0
        for (prod in production) {
            totalEarnings += (prod["totalPayment"] as? Double) ?: 0.0
        }
        return totalEarnings
    }

    private fun displayWorkerProgress(
        workerName: String,
        modality: String,
        attendance: Map<String, Any>?,
        production: List<Map<String, Any>>,
        dailyEarnings: Double
    ) {
        try {
            val inflater = LayoutInflater.from(this)
            val workerView = inflater.inflate(R.layout.item_worker_daily_progress, null)

            // Configurar datos del trabajador
            workerView.findViewById<TextView>(R.id.tvWorkerName).text = workerName
            workerView.findViewById<TextView>(R.id.tvWorkerModality).text =
                if (modality == "DAILY_RATE") "Jornal" else "Destajo"

            // Configurar asistencia
            setupAttendanceInfo(workerView, attendance)

            // Configurar producción y ganancias
            setupProductionInfo(workerView, production, dailyEarnings)

            // Agregar a la lista
            binding.containerWorkers.addView(workerView)

        } catch (e: Exception) {
            Log.e(TAG, "Error mostrando trabajador $workerName: ${e.message}")
        }
    }

    private fun setupAttendanceInfo(workerView: View, attendance: Map<String, Any>?) {
        val tvAttendanceStatus = workerView.findViewById<TextView>(R.id.tvAttendanceStatus)
        val tvEntryTime = workerView.findViewById<TextView>(R.id.tvEntryTime)
        val tvExitTime = workerView.findViewById<TextView>(R.id.tvExitTime)
        val tvHoursWorked = workerView.findViewById<TextView>(R.id.tvHoursWorked)

        if (attendance != null) {
            val status = attendance["status"] as? String ?: "ABSENT"
            val entryTime = attendance["entryTime"] as? String ?: "--:--"
            val exitTime = attendance["exitTime"] as? String ?: "--:--"

            tvAttendanceStatus.text = when (status) {
                "PRESENT" -> "PUNTUAL"
                "LATE" -> "TARDÍO"
                else -> "AUSENTE"
            }

            // Cambiar color según estado
            when (status) {
                "PRESENT" -> tvAttendanceStatus.setBackgroundResource(R.drawable.bg_status_present)
                "LATE" -> tvAttendanceStatus.setBackgroundResource(R.drawable.bg_status_late)
                else -> tvAttendanceStatus.setBackgroundResource(R.drawable.bg_status_absent)
            }

            tvEntryTime.text = entryTime
            tvExitTime.text = exitTime

            // Calcular horas trabajadas
            if (exitTime != "--:--" && entryTime != "--:--") {
                val hours = calculateHoursWorked(entryTime, exitTime)
                tvHoursWorked.text = hours
            } else {
                tvHoursWorked.text = "--:--"
            }
        } else {
            tvAttendanceStatus.text = "AUSENTE"
            tvAttendanceStatus.setBackgroundResource(R.drawable.bg_status_absent)
            tvEntryTime.text = "--:--"
            tvExitTime.text = "--:--"
            tvHoursWorked.text = "--:--"
        }
    }

    private fun setupProductionInfo(workerView: View, production: List<Map<String, Any>>, dailyEarnings: Double) {
        val totalProduction = production.sumOf { (it["quantity"] as? Int) ?: 0 }
        val tvDailyProduction = workerView.findViewById<TextView>(R.id.tvDailyProduction)
        val tvDailyEarnings = workerView.findViewById<TextView>(R.id.tvDailyEarnings)

        tvDailyEarnings.text = "S/ ${String.format("%.2f", dailyEarnings)}"

        // Buscar el contenedor de producción de forma segura
        val productionContainer = workerView.findViewById<View>(R.id.containerProduction)
        if (productionContainer != null && totalProduction > 0) {
            productionContainer.visibility = View.VISIBLE
            tvDailyProduction.text = "$totalProduction unidades"
        } else {
            productionContainer?.visibility = View.GONE
        }
    }

    private fun calculateHoursWorked(entryTime: String, exitTime: String): String {
        return try {
            val entry = DateTime.parse("2000-01-01T${entryTime}:00")
            val exit = DateTime.parse("2000-01-01T${exitTime}:00")
            val duration = org.joda.time.Duration(entry, exit)
            val hours = duration.standardHours
            val minutes = duration.standardMinutes % 60
            String.format("%02d:%02d", hours, minutes)
        } catch (e: Exception) {
            "--:--"
        }
    }

    private fun showWorkersLoading(show: Boolean) {
        binding.progressWorkers.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showNoWorkersMessage(show: Boolean) {
        binding.tvNoWorkers.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun logout() {
        preferences.clear()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}