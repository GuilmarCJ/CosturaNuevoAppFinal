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

        // Simular carga de datos
        Thread {
            try {
                // Simular delay de red
                Thread.sleep(1500)

                val sampleData = generateSampleData()

                runOnUiThread {
                    showLoading(false)
                    attendanceList.clear()
                    attendanceList.addAll(sampleData)
                    attendanceAdapter.notifyDataSetChanged()
                    updateStatistics()
                    updateEmptyState()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showLoading(false)
                    Toast.makeText(this, "Error cargando historial", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun generateSampleData(): List<AttendanceRecord> {
        val records = mutableListOf<AttendanceRecord>()
        val now = DateTime()

        for (i in 0 until 30) {
            val date = now.minusDays(i)
            if (date.dayOfWeek in 1..5) { // Solo días laborables (lunes a viernes)
                val status = when ((0..100).random()) {
                    in 0..80 -> AttendanceStatus.PRESENT
                    in 81..95 -> AttendanceStatus.LATE
                    else -> AttendanceStatus.ABSENT
                }

                val entryTime = when (status) {
                    AttendanceStatus.PRESENT -> "08:0${(0..5).random()}"
                    AttendanceStatus.LATE -> "08:${(15..45).random()}"
                    else -> null
                }

                val exitTime = if (entryTime != null) "17:${(0..59).random()}" else null

                records.add(
                    AttendanceRecord(
                        date = date.toString("yyyy-MM-dd"),
                        entryTime = entryTime ?: "--:--",
                        exitTime = exitTime,
                        status = status
                    )
                )
            }
        }

        return records
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