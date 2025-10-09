package com.costura.pro.ui.attendance

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.costura.pro.R
import com.costura.pro.data.model.AttendanceRecord
import com.costura.pro.data.model.AttendanceStatus
import com.costura.pro.databinding.ActivityAttendanceBinding
import com.costura.pro.utils.AppPreferences
import kotlinx.coroutines.*
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.util.*
import kotlin.concurrent.fixedRateTimer

class AttendanceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAttendanceBinding
    private lateinit var preferences: AppPreferences
    private lateinit var attendanceAdapter: RecentAttendanceAdapter
    private val recentAttendanceList = mutableListOf<AttendanceRecord>()
    private var timeTimer: Timer? = null

    // Obtener el repository desde la aplicación
    private val attendanceRepository by lazy {
        (application as com.costura.pro.CosturaProApp).attendanceRepository
    }

    // Coroutine scope para esta actividad
    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAttendanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferences = AppPreferences(this)
        setupUI()
        setupClickListeners()
        setupRecyclerView()
        loadCurrentAttendance()
        startTimeUpdate()
    }

    override fun onDestroy() {
        super.onDestroy()
        timeTimer?.cancel()
        activityScope.cancel()
    }

    private fun setupUI() {
        supportActionBar?.title = "Registro de Asistencia"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupClickListeners() {
        binding.btnRegisterEntry.setOnClickListener {
            val intent = Intent(this, QRScannerActivity::class.java).apply {
                putExtra("SCAN_TYPE", "ENTRY")
            }
            startActivityForResult(intent, REQUEST_QR_SCAN_ENTRY)
        }

        binding.btnRegisterExit.setOnClickListener {
            val intent = Intent(this, QRScannerActivity::class.java).apply {
                putExtra("SCAN_TYPE", "EXIT")
            }
            startActivityForResult(intent, REQUEST_QR_SCAN_EXIT)
        }

        binding.btnViewHistory.setOnClickListener {
            val intent = Intent(this, AttendanceHistoryActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        attendanceAdapter = RecentAttendanceAdapter(recentAttendanceList)
        binding.rvRecentAttendance.apply {
            layoutManager = LinearLayoutManager(this@AttendanceActivity)
            adapter = attendanceAdapter
        }
    }

    private fun loadCurrentAttendance() {
        val today = DateTime().toString("yyyy-MM-dd")
        val workerId = preferences.userId ?: return

        activityScope.launch {
            try {
                // Cargar registro de hoy
                val todayRecord = attendanceRepository.getAttendanceByWorkerAndDate(workerId, today)
                updateAttendanceUI(todayRecord)

                // Cargar historial reciente
                loadRecentAttendance(workerId)
            } catch (e: Exception) {
                Toast.makeText(this@AttendanceActivity, "Error cargando asistencia", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateAttendanceUI(todayRecord: AttendanceRecord?) {
        if (todayRecord == null) {
            // No ha registrado entrada hoy
            binding.tvStatus.text = "No has registrado entrada hoy"
            binding.tvStatusBadge.text = "AUSENTE"
            binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_absent)
            binding.layoutEntryInfo.visibility = android.view.View.GONE
            binding.layoutExitInfo.visibility = android.view.View.GONE
            binding.btnRegisterEntry.isEnabled = true
            binding.btnRegisterExit.isEnabled = false
        } else {
            // Ya registró entrada
            binding.tvStatus.text = "Entrada registrada"
            binding.tvEntryTime.text = todayRecord.entryTime
            binding.layoutEntryInfo.visibility = android.view.View.VISIBLE

            when (todayRecord.status) {
                AttendanceStatus.PRESENT -> {
                    binding.tvStatusBadge.text = "PUNTUAL"
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_active)
                }
                AttendanceStatus.LATE -> {
                    binding.tvStatusBadge.text = "TARDÍO"
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_late)
                }
                else -> {
                    binding.tvStatusBadge.text = "PRESENTE"
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_active)
                }
            }

            if (todayRecord.exitTime != null) {
                // Ya registró salida
                binding.tvStatus.text = "Jornada completada"
                binding.tvExitTime.text = todayRecord.exitTime
                binding.layoutExitInfo.visibility = android.view.View.VISIBLE
                binding.btnRegisterEntry.isEnabled = false
                binding.btnRegisterExit.isEnabled = false
            } else {
                // Solo entrada registrada
                binding.btnRegisterEntry.isEnabled = false
                binding.btnRegisterExit.isEnabled = true
            }
        }
    }

    private fun loadRecentAttendance(workerId: String) {
        activityScope.launch {
            try {
                val recentRecords = withContext(Dispatchers.IO) {
                    getRealRecentAttendance(workerId)
                }

                recentAttendanceList.clear()
                recentAttendanceList.addAll(recentRecords)
                attendanceAdapter.notifyDataSetChanged()
                updateEmptyState()
            } catch (e: Exception) {
                recentAttendanceList.clear()
                attendanceAdapter.notifyDataSetChanged()
                updateEmptyState()
            }
        }
    }

    private suspend fun getRealRecentAttendance(workerId: String): List<AttendanceRecord> {
        return try {
            val repository = (application as com.costura.pro.CosturaProApp).attendanceRepository
            val recentRecords = repository.getAttendanceHistory(workerId, DateTime())

            recentRecords
                .sortedByDescending { it.date }
                .take(5)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun updateEmptyState() {
        if (recentAttendanceList.isEmpty()) {
            binding.tvEmptyHistory.visibility = android.view.View.VISIBLE
            binding.rvRecentAttendance.visibility = android.view.View.GONE
        } else {
            binding.tvEmptyHistory.visibility = android.view.View.GONE
            binding.rvRecentAttendance.visibility = android.view.View.VISIBLE
        }
    }

    private fun startTimeUpdate() {
        updateTimeDisplay()
        timeTimer = fixedRateTimer("timeUpdater", false, 0, 1000) {
            Handler(Looper.getMainLooper()).post {
                updateTimeDisplay()
            }
        }
    }

    private fun updateTimeDisplay() {
        val now = DateTime()
        val dateFormat = DateTimeFormat.forPattern("EEEE, d 'de' MMMM")
        val timeFormat = DateTimeFormat.forPattern("HH:mm:ss")
        binding.tvCurrentDate.text = dateFormat.print(now).replaceFirstChar { it.uppercase() }
        binding.tvCurrentTime.text = timeFormat.print(now)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_QR_SCAN_ENTRY, REQUEST_QR_SCAN_EXIT -> {
                    loadCurrentAttendance() // Recargar datos después de registrar
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    companion object {
        const val REQUEST_QR_SCAN_ENTRY = 1001
        const val REQUEST_QR_SCAN_EXIT = 1002
    }
}