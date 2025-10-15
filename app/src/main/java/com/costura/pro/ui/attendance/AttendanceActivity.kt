package com.costura.pro.ui.attendance

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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

    // Nuevo sistema de Activity Result (reemplaza startActivityForResult)
    private val qrScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val actionType = data?.getStringExtra("ACTION_TYPE") ?: "asistencia"
            val workerName = data?.getStringExtra("WORKER_NAME") ?: preferences.username ?: ""

            Toast.makeText(this, "✅ $workerName registró $actionType automáticamente", Toast.LENGTH_LONG).show()
            loadCurrentAttendance()
        }
    }

    private val attendanceRepository by lazy {
        (application as com.costura.pro.CosturaProApp).attendanceRepository
    }

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

    override fun onResume() {
        super.onResume()
        loadCurrentAttendance()
    }

    override fun onDestroy() {
        super.onDestroy()
        timeTimer?.cancel()
        activityScope.cancel()
    }

    private fun setupUI() {
        supportActionBar?.title = "Registro de Asistencia"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val workerName = preferences.username ?: "Trabajador"
        binding.tvWorkerName.text = "Hola, $workerName"
    }

    private fun setupClickListeners() {
        binding.btnRegisterEntry.setOnClickListener {
            val intent = Intent(this, QRScannerActivity::class.java)
            qrScannerLauncher.launch(intent)  // Usar nuevo sistema
        }

        binding.btnRegisterExit.setOnClickListener {
            val intent = Intent(this, QRScannerActivity::class.java)
            qrScannerLauncher.launch(intent)  // Usar nuevo sistema
        }

        binding.btnViewHistory.setOnClickListener {
            val intent = Intent(this, AttendanceHistoryActivity::class.java)
            startActivity(intent)
        }

        binding.btnRefresh.setOnClickListener {
            loadCurrentAttendance()
            Toast.makeText(this, "Actualizando...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        attendanceAdapter = RecentAttendanceAdapter(recentAttendanceList)
        binding.rvRecentAttendance.layoutManager = LinearLayoutManager(this)
        binding.rvRecentAttendance.adapter = attendanceAdapter
    }

    private fun loadCurrentAttendance() {
        val today = DateTime().toString("yyyy-MM-dd")
        val workerId = preferences.userId ?: return

        activityScope.launch {
            try {
                showLoading(true)

                val todayRecord = attendanceRepository.getAttendanceByWorkerAndDate(workerId, today)
                updateAttendanceUI(todayRecord)
                loadRecentAttendance(workerId)

            } catch (e: Exception) {
                Toast.makeText(this@AttendanceActivity, "Error cargando asistencia", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun updateAttendanceUI(todayRecord: AttendanceRecord?) {
        if (todayRecord == null) {
            binding.tvStatus.text = "No has registrado entrada hoy"
            binding.tvStatusBadge.text = "AUSENTE"
            binding.layoutEntryInfo.visibility = android.view.View.GONE
            binding.layoutExitInfo.visibility = android.view.View.GONE
            binding.btnRegisterEntry.isEnabled = true
            binding.btnRegisterExit.isEnabled = false
            binding.tvInstructions.text = "Escanea el código QR para comenzar tu jornada"
        } else {
            binding.tvStatus.text = "Entrada registrada"
            binding.tvEntryTime.text = todayRecord.entryTime
            binding.layoutEntryInfo.visibility = android.view.View.VISIBLE

            when (todayRecord.status) {
                AttendanceStatus.PRESENT -> {
                    binding.tvStatusBadge.text = "PUNTUAL"
                }
                AttendanceStatus.LATE -> {
                    binding.tvStatusBadge.text = "TARDÍO"
                }
                else -> {
                    binding.tvStatusBadge.text = "PRESENTE"
                }
            }

            if (todayRecord.exitTime != null) {
                binding.tvStatus.text = "Jornada completada"
                binding.tvExitTime.text = todayRecord.exitTime
                binding.layoutExitInfo.visibility = android.view.View.VISIBLE
                binding.btnRegisterEntry.isEnabled = false
                binding.btnRegisterExit.isEnabled = false
                binding.tvInstructions.text = "Jornada completada. ¡Hasta mañana!"
            } else {
                binding.btnRegisterEntry.isEnabled = false
                binding.btnRegisterExit.isEnabled = true
                binding.tvInstructions.text = "Escanea el código QR para registrar tu salida"
            }
        }
    }

    private fun loadRecentAttendance(workerId: String) {
        activityScope.launch {
            try {
                val recentRecords = withContext(Dispatchers.IO) {
                    attendanceRepository.getAttendanceHistory(workerId, DateTime())
                        .sortedByDescending { it.date }
                        .take(5)
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

    private fun updateEmptyState() {
        binding.tvEmptyHistory.visibility = if (recentAttendanceList.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        binding.rvRecentAttendance.visibility = if (recentAttendanceList.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
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

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        binding.btnRefresh.isEnabled = !show
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    // ELIMINAR el método onActivityResult y las constantes REQUEST_QR_SCAN
}