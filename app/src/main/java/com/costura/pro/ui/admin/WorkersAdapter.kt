package com.costura.pro.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.costura.pro.R
import com.costura.pro.data.model.Worker
import com.costura.pro.data.model.WorkModality

class WorkersAdapter(
    private val workers: List<Worker>,
    private val onWorkerAction: (Worker, String) -> Unit
) : RecyclerView.Adapter<WorkersAdapter.WorkerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_worker, parent, false)
        return WorkerViewHolder(view)
    }

    override fun onBindViewHolder(holder: WorkerViewHolder, position: Int) {
        val worker = workers[position]
        holder.bind(worker)
    }

    override fun getItemCount(): Int = workers.size

    inner class WorkerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvWorkerName)
        private val tvUsername: TextView = itemView.findViewById(R.id.tvWorkerUsername)
        private val tvModality: TextView = itemView.findViewById(R.id.tvWorkerModality)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvWorkerStatus)
        private val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
        private val btnToggleStatus: Button = itemView.findViewById(R.id.btnToggleStatus)

        fun bind(worker: Worker) {
            tvName.text = worker.name
            tvUsername.text = "Usuario: ${worker.username}"
            tvModality.text = "Modalidad: ${
                when (worker.modality) {
                    WorkModality.DAILY_RATE -> "Jornal"
                    WorkModality.PIECE_RATE -> "Destajo"
                }
            }"

            // Set status
            if (worker.isActive) {
                tvStatus.text = "ACTIVO"
                tvStatus.setBackgroundResource(R.drawable.bg_status_active)
                btnToggleStatus.text = "DESACTIVAR"
                btnToggleStatus.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.error_red))
            } else {
                tvStatus.text = "INACTIVO"
                tvStatus.setBackgroundResource(R.drawable.bg_status_inactive)
                btnToggleStatus.text = "ACTIVAR"
                btnToggleStatus.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.success_green))
            }

            btnEdit.setOnClickListener {
                onWorkerAction(worker, "edit")
            }

            btnToggleStatus.setOnClickListener {
                onWorkerAction(worker, "toggle")
            }
        }
    }
}