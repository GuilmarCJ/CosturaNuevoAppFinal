package com.costura.pro.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.costura.pro.R
import com.costura.pro.data.model.Operation

class OperationsAdapter(
    private val operations: List<Operation>,
    private val onOperationAction: (Operation, String) -> Unit
) : RecyclerView.Adapter<OperationsAdapter.OperationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OperationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_operation, parent, false)
        return OperationViewHolder(view)
    }

    override fun onBindViewHolder(holder: OperationViewHolder, position: Int) {
        val operation = operations[position]
        holder.bind(operation)
    }

    override fun getItemCount(): Int = operations.size

    inner class OperationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvOperationName)
        private val tvPayment: TextView = itemView.findViewById(R.id.tvPaymentPerUnit)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvOperationStatus)
        private val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
        private val btnToggleStatus: Button = itemView.findViewById(R.id.btnToggleStatus)

        fun bind(operation: Operation) {
            tvName.text = operation.name
            tvPayment.text = "Pago por unidad: S/ ${String.format("%.2f", operation.paymentPerUnit)}"

            // Set status
            if (operation.isActive) {
                tvStatus.text = "ACTIVA"
                tvStatus.setBackgroundResource(R.drawable.bg_status_active)
                btnToggleStatus.text = "DESACTIVAR"
                btnToggleStatus.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.error_red))
            } else {
                tvStatus.text = "INACTIVA"
                tvStatus.setBackgroundResource(R.drawable.bg_status_inactive)
                btnToggleStatus.text = "ACTIVAR"
                btnToggleStatus.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.success_green))
            }

            btnEdit.setOnClickListener {
                onOperationAction(operation, "edit")
            }

            btnToggleStatus.setOnClickListener {
                onOperationAction(operation, "toggle")
            }
        }
    }
}