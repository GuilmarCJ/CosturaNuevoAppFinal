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

class RecentAttendanceAdapter(
    private val attendanceList: List<AttendanceRecord>
) : RecyclerView.Adapter<RecentAttendanceAdapter.AttendanceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_attendance, parent, false)
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

        fun bind(record: AttendanceRecord) {
            // Formatear fecha
            val date = DateTime.parse(record.date)
            val dateFormat = DateTimeFormat.forPattern("d 'de' MMM")
            val dayFormat = DateTimeFormat.forPattern("EEEE")

            tvDate.text = dateFormat.print(date).replaceFirstChar { it.uppercase() }
            tvDay.text = dayFormat.print(date).replaceFirstChar { it.uppercase() }

            // Mostrar tiempos
            tvEntryTime.text = "Entrada: ${record.entryTime}"
            tvExitTime.text = if (record.exitTime != null) {
                "Salida: ${record.exitTime}"
            } else {
                "Salida: --:--"
            }

            // Configurar status
            when (record.status) {
                AttendanceStatus.PRESENT -> {
                    tvStatus.text = "PUNTUAL"
                    tvStatus.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.success_green))
                }
                AttendanceStatus.LATE -> {
                    tvStatus.text = "TARDÍO"
                    tvStatus.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.warning_orange))
                }
                AttendanceStatus.ABSENT -> {
                    tvStatus.text = "AUSENTE"
                    tvStatus.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.error_red))
                }
                AttendanceStatus.HALF_DAY -> {
                    tvStatus.text = "MEDIO DÍA"
                    tvStatus.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.warning_orange))
                }
            }
        }
    }
}