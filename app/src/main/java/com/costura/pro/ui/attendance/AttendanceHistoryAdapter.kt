package com.costura.pro.ui.attendance

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.costura.pro.R
import com.costura.pro.data.model.AttendanceRecord
import com.costura.pro.data.model.AttendanceStatus
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

class AttendanceHistoryAdapter(
    private val attendanceList: List<AttendanceRecord>
) : RecyclerView.Adapter<AttendanceHistoryAdapter.AttendanceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance_history, parent, false)
        return AttendanceViewHolder(view)
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        val record = attendanceList[position]
        holder.bind(record)
    }

    override fun getItemCount(): Int = attendanceList.size

    class AttendanceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvDay: TextView = itemView.findViewById(R.id.tvDay)
        private val tvEntryTime: TextView = itemView.findViewById(R.id.tvEntryTime)
        private val tvExitTime: TextView = itemView.findViewById(R.id.tvExitTime)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvHours: TextView = itemView.findViewById(R.id.tvHours)

        fun bind(record: AttendanceRecord) {
            // Formatear fecha
            val date = DateTime.parse(record.date)
            val dateFormat = DateTimeFormat.forPattern("d 'de' MMM")
            val dayFormat = DateTimeFormat.forPattern("EEEE")

            tvDate.text = dateFormat.print(date)
            tvDay.text = dayFormat.print(date).replaceFirstChar { it.uppercase() }

            // Mostrar tiempos
            tvEntryTime.text = if (record.entryTime != "--:--") record.entryTime else "--:--"
            tvExitTime.text = record.exitTime ?: "--:--"

            // Calcular horas trabajadas
            val hoursWorked = calculateHoursWorked(record.entryTime, record.exitTime)
            tvHours.text = hoursWorked

            // Configurar status
            when (record.status) {
                AttendanceStatus.PRESENT -> {
                    tvStatus.text = "PUNTUAL"
                    tvStatus.setBackgroundResource(R.drawable.bg_status_present)
                }
                AttendanceStatus.LATE -> {
                    tvStatus.text = "TARDÍO"
                    tvStatus.setBackgroundResource(R.drawable.bg_status_late)
                }
                AttendanceStatus.ABSENT -> {
                    tvStatus.text = "AUSENTE"
                    tvStatus.setBackgroundResource(R.drawable.bg_status_absent)
                }
                AttendanceStatus.HALF_DAY -> {
                    tvStatus.text = "MEDIO DÍA"
                    tvStatus.setBackgroundResource(R.drawable.bg_status_half_day)
                }
            }

            // Cambiar colores si está ausente
            if (record.status == AttendanceStatus.ABSENT) {
                tvEntryTime.setTextColor(ContextCompat.getColor(itemView.context, R.color.gray))
                tvExitTime.setTextColor(ContextCompat.getColor(itemView.context, R.color.gray))
                tvHours.setTextColor(ContextCompat.getColor(itemView.context, R.color.gray))
            } else {
                tvEntryTime.setTextColor(ContextCompat.getColor(itemView.context, R.color.black))
                tvExitTime.setTextColor(ContextCompat.getColor(itemView.context, R.color.black))
                tvHours.setTextColor(ContextCompat.getColor(itemView.context, R.color.primary_color))
            }
        }

        private fun calculateHoursWorked(entryTime: String, exitTime: String?): String {
            if (exitTime == null || entryTime == "--:--") return "--:--"

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
    }
}