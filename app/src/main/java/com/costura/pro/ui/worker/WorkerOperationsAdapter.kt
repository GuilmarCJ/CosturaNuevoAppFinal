package com.costura.pro.ui.worker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.costura.pro.R
import com.costura.pro.data.model.Operation
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class WorkerOperationsAdapter(
    private val operations: List<Operation>,
    private val workerId: String,
    private val onProductionRegistered: (Operation, Int) -> Unit
) : RecyclerView.Adapter<WorkerOperationsAdapter.OperationViewHolder>() {

    private val db = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OperationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_worker_operation, parent, false)
        return OperationViewHolder(view)
    }

    override fun onBindViewHolder(holder: OperationViewHolder, position: Int) {
        val operation = operations[position]
        holder.bind(operation)
    }

    override fun getItemCount(): Int = operations.size

    inner class OperationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvOperationName)
        private val tvPayment: TextView = itemView.findViewById(R.id.tvPayment)
        private val tvTodayProduction: TextView = itemView.findViewById(R.id.tvTodayProduction)
        private val tvCustomQuantity: TextView = itemView.findViewById(R.id.tvCustomQuantity)

        private val btnAddSmall: Button = itemView.findViewById(R.id.btnAddSmall)
        private val btnAddMedium: Button = itemView.findViewById(R.id.btnAddMedium)
        private val btnAddLarge: Button = itemView.findViewById(R.id.btnAddLarge)
        private val btnCustomMinus: Button = itemView.findViewById(R.id.btnCustomMinus)
        private val btnCustomPlus: Button = itemView.findViewById(R.id.btnCustomPlus)
        private val btnAddCustom: Button = itemView.findViewById(R.id.btnAddCustom)

        private var customQuantity = 0

        fun bind(operation: Operation) {
            tvName.text = operation.name
            tvPayment.text = "S/ ${String.format("%.2f", operation.paymentPerUnit)} c/u"

            loadTodayProduction(operation.id)

            // Quick action buttons
            btnAddSmall.setOnClickListener {
                onProductionRegistered(operation, 10)
            }

            btnAddMedium.setOnClickListener {
                onProductionRegistered(operation, 25)
            }

            btnAddLarge.setOnClickListener {
                onProductionRegistered(operation, 50)
            }

            // Custom quantity controls
            btnCustomMinus.setOnClickListener {
                if (customQuantity > 0) {
                    customQuantity--
                    updateCustomQuantityDisplay()
                }
            }

            btnCustomPlus.setOnClickListener {
                customQuantity++
                updateCustomQuantityDisplay()
            }

            btnAddCustom.setOnClickListener {
                if (customQuantity > 0) {
                    onProductionRegistered(operation, customQuantity)
                    customQuantity = 0
                    updateCustomQuantityDisplay()
                } else {
                    Toast.makeText(itemView.context, "Selecciona una cantidad", Toast.LENGTH_SHORT).show()
                }
            }
        }

        private fun updateCustomQuantityDisplay() {
            tvCustomQuantity.text = customQuantity.toString()
        }

        private fun loadTodayProduction(operationId: String) {
            scope.launch {
                try {
                    val startOfDay = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time

                    val documents = db.collection("users")
                        .document(workerId)
                        .collection("production")
                        .whereEqualTo("operationId", operationId)
                        .whereGreaterThanOrEqualTo("date", Timestamp(startOfDay))
                        .get()
                        .await()

                    var todayTotal = 0
                    for (document in documents) {
                        val quantity = document.getLong("quantity")?.toInt() ?: 0
                        todayTotal += quantity
                    }
                    tvTodayProduction.text = "Hoy: $todayTotal unidades"
                } catch (e: Exception) {
                    tvTodayProduction.text = "Hoy: 0 unidades"
                }
            }
        }
    }
}