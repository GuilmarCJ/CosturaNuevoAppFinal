package com.costura.pro.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.costura.pro.R
import com.costura.pro.data.local.entity.MachineHistoryEntity
import com.costura.pro.data.local.entity.HistoryType
import java.text.SimpleDateFormat
import java.util.*

class MachineHistoryAdapter(
    private var history: List<MachineHistoryEntity>
) : RecyclerView.Adapter<MachineHistoryAdapter.HistoryViewHolder>() {

    fun updateData(newHistory: List<MachineHistoryEntity>) {
        this.history = newHistory
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_machine_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val historyItem = history[position]
        holder.bind(historyItem)
    }

    override fun getItemCount(): Int = history.size

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMachineName: TextView = itemView.findViewById(R.id.tvMachineName)
        private val tvMachineNumber: TextView = itemView.findViewById(R.id.tvMachineNumber)
        private val tvEventType: TextView = itemView.findViewById(R.id.tvEventType)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvProblemDescription: TextView = itemView.findViewById(R.id.tvProblemDescription)
        private val tvSolution: TextView = itemView.findViewById(R.id.tvSolution)
        private val containerSolution: View = itemView.findViewById(R.id.containerSolution)

        fun bind(historyItem: MachineHistoryEntity) {
            tvMachineName.text = historyItem.machineName
            tvMachineNumber.text = "Nº: ${historyItem.machineNumber}"
            tvProblemDescription.text = historyItem.description

            // Formatear fecha
            val dateFormat = SimpleDateFormat("dd MMM yyyy - HH:mm", Locale.getDefault())
            tvDate.text = dateFormat.format(Date(historyItem.date))

            // Configurar según el tipo de evento
            when (historyItem.type) {
                HistoryType.PROBLEM_REPORTED -> {
                    tvEventType.text = "PROBLEMA"
                    tvEventType.setBackgroundResource(R.drawable.bg_status_late)
                    containerSolution.visibility = View.GONE
                }
                HistoryType.PROBLEM_SOLVED -> {
                    tvEventType.text = "SOLUCIONADO"
                    tvEventType.setBackgroundResource(R.drawable.bg_status_active)
                    containerSolution.visibility = View.VISIBLE
                    tvSolution.text = historyItem.solution ?: "Solución no especificada"
                }
            }
        }
    }
}