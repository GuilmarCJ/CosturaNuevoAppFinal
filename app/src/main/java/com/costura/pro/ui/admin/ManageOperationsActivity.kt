package com.costura.pro.ui.admin

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.costura.pro.data.model.Operation
import com.costura.pro.databinding.ActivityManageOperationsBinding
import com.costura.pro.databinding.DialogAddOperationBinding
import com.costura.pro.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore

class ManageOperationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageOperationsBinding
    private val db = FirebaseFirestore.getInstance()
    private val operationsList = mutableListOf<Operation>()
    private lateinit var operationsAdapter: OperationsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageOperationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupClickListeners()
        loadOperations()
    }

    private fun setupRecyclerView() {
        operationsAdapter = OperationsAdapter(operationsList) { operation, action ->
            when (action) {
                "edit" -> showEditOperationDialog(operation)
                "toggle" -> toggleOperationStatus(operation)
            }
        }
        binding.rvOperations.apply {
            layoutManager = LinearLayoutManager(this@ManageOperationsActivity)
            adapter = operationsAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnAddOperation.setOnClickListener {
            showAddOperationDialog()
        }

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun loadOperations() {
        showLoading(true)
        db.collection(Constants.COLLECTION_OPERATIONS)
            .get()
            .addOnSuccessListener { documents ->
                operationsList.clear()
                for (document in documents) {
                    val operation = Operation(
                        id = document.id,
                        name = document.getString("name") ?: "",
                        paymentPerUnit = document.getDouble("paymentPerUnit") ?: 0.0,
                        isActive = document.getBoolean("isActive") ?: true
                    )
                    operationsList.add(operation)
                }
                operationsAdapter.notifyDataSetChanged()
                showLoading(false)
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Toast.makeText(this, "Error cargando operaciones: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAddOperationDialog() {
        val dialogBinding = DialogAddOperationBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setTitle("NUEVA OPERACIÓN")
            .setView(dialogBinding.root)
            .setPositiveButton("CREAR OPERACIÓN", null)
            .setNegativeButton("CANCELAR", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val name = dialogBinding.etOperationName.text.toString().trim()
                val paymentText = dialogBinding.etPaymentPerUnit.text.toString().trim()

                if (validateOperationInputs(name, paymentText)) {
                    val paymentPerUnit = paymentText.toDouble()
                    createOperation(name, paymentPerUnit)
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun showEditOperationDialog(operation: Operation) {
        val dialogBinding = DialogAddOperationBinding.inflate(layoutInflater)
        dialogBinding.etOperationName.setText(operation.name)
        dialogBinding.etPaymentPerUnit.setText(operation.paymentPerUnit.toString())

        val dialog = AlertDialog.Builder(this)
            .setTitle("EDITAR OPERACIÓN")
            .setView(dialogBinding.root)
            .setPositiveButton("ACTUALIZAR", null)
            .setNegativeButton("CANCELAR", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val name = dialogBinding.etOperationName.text.toString().trim()
                val paymentText = dialogBinding.etPaymentPerUnit.text.toString().trim()

                if (validateOperationInputs(name, paymentText)) {
                    val paymentPerUnit = paymentText.toDouble()
                    updateOperation(operation.id, name, paymentPerUnit)
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun validateOperationInputs(name: String, paymentText: String): Boolean {
        if (name.isEmpty()) {
            Toast.makeText(this, "Ingresa el nombre de la operación", Toast.LENGTH_SHORT).show()
            return false
        }
        if (paymentText.isEmpty()) {
            Toast.makeText(this, "Ingresa el pago por unidad", Toast.LENGTH_SHORT).show()
            return false
        }
        try {
            val payment = paymentText.toDouble()
            if (payment <= 0) {
                Toast.makeText(this, "El pago debe ser mayor a 0", Toast.LENGTH_SHORT).show()
                return false
            }
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Ingresa un valor válido para el pago", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun createOperation(name: String, paymentPerUnit: Double) {
        showLoading(true)

        val operationData = hashMapOf(
            "name" to name,
            "paymentPerUnit" to paymentPerUnit,
            "isActive" to true,
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        db.collection(Constants.COLLECTION_OPERATIONS)
            .add(operationData)
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, "Operación creada exitosamente", Toast.LENGTH_SHORT).show()
                loadOperations()
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Toast.makeText(this, "Error creando operación: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateOperation(operationId: String, name: String, paymentPerUnit: Double) {
        showLoading(true)

        val updates = hashMapOf<String, Any>(
            "name" to name,
            "paymentPerUnit" to paymentPerUnit
        )

        db.collection(Constants.COLLECTION_OPERATIONS)
            .document(operationId)
            .update(updates)
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, "Operación actualizada exitosamente", Toast.LENGTH_SHORT).show()
                loadOperations()
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Toast.makeText(this, "Error actualizando operación: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun toggleOperationStatus(operation: Operation) {
        val newStatus = !operation.isActive
        val action = if (newStatus) "activada" else "desactivada"

        db.collection(Constants.COLLECTION_OPERATIONS)
            .document(operation.id)
            .update("isActive", newStatus)
            .addOnSuccessListener {
                Toast.makeText(this, "Operación $action exitosamente", Toast.LENGTH_SHORT).show()
                loadOperations()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error actualizando estado: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
    }
}