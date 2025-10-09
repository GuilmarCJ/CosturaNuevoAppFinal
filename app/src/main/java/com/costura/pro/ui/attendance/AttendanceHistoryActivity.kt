package com.costura.pro.ui.attendance

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.costura.pro.data.model.AttendanceRecord
import com.costura.pro.data.model.AttendanceStatus
import com.costura.pro.databinding.ActivityAttendanceHistoryBinding
import com.costura.pro.utils.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.util.*

class AttendanceHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAttendanceHistoryBinding
    private lateinit var preferences: AppPreferences
    private lateinit var attendanceAdapter: AttendanceHistoryAdapter
    private val attendanceList = mutableListOf<AttendanceRecord>()

    private var selectedMonth: DateTime = DateTime()
    private var selectedStatus: String = "ALL"

    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAttendanceHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferences = AppPreferences(this)
        setupUI()
        setupClickListeners()
        setupRecyclerView()
        loadAttendanceHistory()
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
        updateMonthDisplay()
    }

    private fun setupClickListeners() {
        // Seleccionar mes
        binding.btnSelectMonth.setOnClickListener {
            showMonthPicker()
        }

        // Seleccionar estado
        binding.btnSelectStatus.setOnClickListener {
            showStatusFilterDialog()
        }
    }

    private fun setupRecyclerView() {
        attendanceAdapter = AttendanceHistoryAdapter(attendanceList)
        binding.rvAttendanceHistory.apply {
            layoutManager = LinearLayoutManager(this@AttendanceHistoryActivity)
            adapter = attendanceAdapter
        }
    }

    private fun loadAttendanceHistory() {
        showLoading(true)

        val workerId = preferences.userId ?: return

        // Usar coroutines en lugar de Thread
        activityScope.launch {
            try {
                val realData = withContext(Dispatchers.IO) {
                    getRealAttendanceHistory(workerId, selectedMonth)
                }

                showLoading(false)
                attendanceList.clear()
                attendanceList.addAll(realData)
                attendanceAdapter.notifyDataSetChanged()
                updateStatistics()
                updateEmptyState()
            } catch (e: Exception) {
                showLoading(false)
                // En caso de error, mostrar lista vacía
                attendanceList.clear()
                attendanceAdapter.notifyDataSetChanged()
                updateEmptyState()
                Toast.makeText(this@AttendanceHistoryActivity, "No hay registros de asistencia", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun getRealAttendanceHistory(workerId: String, month: DateTime): List<AttendanceRecord> {
        return try {
            val repository = (application as com.costura.pro.CosturaProApp).attendanceRepository
            repository.getAttendanceHistory(workerId, month)
        } catch (e: Exception) {
            emptyList()
        }
    }



    private fun showMonthPicker() {
        val currentYear = selectedMonth.year
            val currentMonth = selectedMonth.monthOfYear - 1 // Calendar usa 0-11

        val datePicker = DatePickerDialog(
            this,
            { _, year, month, _ ->
                selectedMonth = DateTime().withYear(year).withMonthOfYear(month + 1).withDayOfMonth(1)
                updateMonthDisplay()
                loadAttendanceHistory()
            },
            currentYear,
            currentMonth,
            1
        )

        datePicker.datePicker.findViewById<android.view.View>(
            resources.getIdentifier("day", "id", "android")
        )?.visibility = android.view.View.GONE

        datePicker.show()
    }

    private fun showStatusFilterDialog() {
        val statuses = arrayOf("Todos", "Puntual", "Tardío", "Ausente")
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, statuses)

        android.app.AlertDialog.Builder(this)
            .setTitle("Filtrar por estado")
            .setAdapter(adapter) { _, which ->
                selectedStatus = when (which) {
                    0 -> "ALL"
                    1 -> "PRESENT"
                    2 -> "LATE"
                    3 -> "ABSENT"
                    else -> "ALL"
                }
                binding.tvSelectedStatus.text = statuses[which]
                loadAttendanceHistory()
            }
            .show()
    }

    private fun updateMonthDisplay() {
        val monthFormat = DateTimeFormat.forPattern("MMMM yyyy")
        binding.tvSelectedMonth.text = monthFormat.print(selectedMonth).replaceFirstChar { it.uppercase() }
    }

    private fun updateStatistics() {
        val workedDays = attendanceList.count { it.entryTime != "--:--" }
        val onTimeDays = attendanceList.count { it.status == AttendanceStatus.PRESENT }
        val lateDays = attendanceList.count { it.status == AttendanceStatus.LATE }

        binding.tvDaysWorked.text = workedDays.toString()
        binding.tvOnTimeDays.text = onTimeDays.toString()
        binding.tvLateDays.text = lateDays.toString()
    }

    private fun updateEmptyState() {
        if (attendanceList.isEmpty()) {
            binding.tvEmptyHistory.visibility = android.view.View.VISIBLE
            binding.rvAttendanceHistory.visibility = android.view.View.GONE
        } else {
            binding.tvEmptyHistory.visibility = android.view.View.GONE
            binding.rvAttendanceHistory.visibility = android.view.View.VISIBLE
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
    }
}