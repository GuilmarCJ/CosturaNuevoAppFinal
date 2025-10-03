package com.costura.pro.ui.admin

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.costura.pro.data.model.Worker
import com.costura.pro.data.model.WorkModality
import com.costura.pro.databinding.ActivityManageWorkersBinding
import com.costura.pro.databinding.DialogAddWorkerBinding
import com.costura.pro.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore

class ManageWorkersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageWorkersBinding
    private val db = FirebaseFirestore.getInstance()
    private val workersList = mutableListOf<Worker>()
    private lateinit var workersAdapter: WorkersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageWorkersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupClickListeners()
        loadWorkers()
    }

    private fun setupRecyclerView() {
        workersAdapter = WorkersAdapter(workersList) { worker, action ->
            when (action) {
                "edit" -> showEditWorkerDialog(worker)
                "toggle" -> toggleWorkerStatus(worker)
            }
        }
        binding.rvWorkers.apply {
            layoutManager = LinearLayoutManager(this@ManageWorkersActivity)
            adapter = workersAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnAddWorker.setOnClickListener {
            showAddWorkerDialog()
        }

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun loadWorkers() {
        showLoading(true)
        db.collection(Constants.COLLECTION_USERS)
            .whereEqualTo("role", "WORKER")
            .get()
            .addOnSuccessListener { documents ->
                workersList.clear()
                for (document in documents) {
                    val worker = Worker(
                        id = document.id,
                        name = document.getString("name") ?: "",
                        username = document.getString("username") ?: "",
                        password = document.getString("password") ?: "",
                        modality = WorkModality.valueOf(document.getString("modality") ?: "PIECE_RATE"),
                        isActive = document.getBoolean("isActive") ?: true
                    )
                    workersList.add(worker)
                }
                workersAdapter.notifyDataSetChanged()
                showLoading(false)
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Toast.makeText(this, "Error cargando trabajadores: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAddWorkerDialog() {
        val dialogBinding = DialogAddWorkerBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setTitle("AGREGAR TRABAJADOR")
            .setView(dialogBinding.root)
            .setPositiveButton("CREAR USUARIO", null)
            .setNegativeButton("CANCELAR", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val name = dialogBinding.etName.text.toString().trim()
                val username = dialogBinding.etUsername.text.toString().trim()
                val password = dialogBinding.etPassword.text.toString().trim()
                val modality = if (dialogBinding.rbDailyRate.isChecked) {
                    WorkModality.DAILY_RATE
                } else {
                    WorkModality.PIECE_RATE
                }

                if (validateWorkerInputs(name, username, password)) {
                    createWorker(name, username, password, modality)
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun showEditWorkerDialog(worker: Worker) {
        val dialogBinding = DialogAddWorkerBinding.inflate(layoutInflater)
        dialogBinding.etName.setText(worker.name)
        dialogBinding.etUsername.setText(worker.username)
        dialogBinding.etPassword.setText(worker.password)

        when (worker.modality) {
            WorkModality.DAILY_RATE -> dialogBinding.rbDailyRate.isChecked = true
            WorkModality.PIECE_RATE -> dialogBinding.rbPieceRate.isChecked = true
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("EDITAR TRABAJADOR")
            .setView(dialogBinding.root)
            .setPositiveButton("ACTUALIZAR", null)
            .setNegativeButton("CANCELAR", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val name = dialogBinding.etName.text.toString().trim()
                val username = dialogBinding.etUsername.text.toString().trim()
                val password = dialogBinding.etPassword.text.toString().trim()
                val modality = if (dialogBinding.rbDailyRate.isChecked) {
                    WorkModality.DAILY_RATE
                } else {
                    WorkModality.PIECE_RATE
                }

                if (validateWorkerInputs(name, username, password)) {
                    updateWorker(worker.id, name, username, password, modality)
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun validateWorkerInputs(name: String, username: String, password: String): Boolean {
        if (name.isEmpty()) {
            Toast.makeText(this, "Ingresa el nombre del trabajador", Toast.LENGTH_SHORT).show()
            return false
        }
        if (username.isEmpty()) {
            Toast.makeText(this, "Ingresa el usuario", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.isEmpty()) {
            Toast.makeText(this, "Ingresa la contraseña", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun createWorker(name: String, username: String, password: String, modality: WorkModality) {
        showLoading(true)

        // Check if username already exists
        db.collection(Constants.COLLECTION_USERS)
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    val workerData = hashMapOf(
                        "name" to name,
                        "username" to username,
                        "password" to password,
                        "modality" to modality.name,
                        "role" to "WORKER",
                        "isActive" to true,
                        "createdAt" to com.google.firebase.Timestamp.now()
                    )

                    db.collection(Constants.COLLECTION_USERS)
                        .add(workerData)
                        .addOnSuccessListener {
                            showLoading(false)
                            Toast.makeText(this, "Trabajador creado exitosamente", Toast.LENGTH_SHORT).show()
                            loadWorkers()
                        }
                        .addOnFailureListener { exception ->
                            showLoading(false)
                            Toast.makeText(this, "Error creando trabajador: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    showLoading(false)
                    Toast.makeText(this, "El usuario ya existe", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Toast.makeText(this, "Error verificando usuario: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateWorker(workerId: String, name: String, username: String, password: String, modality: WorkModality) {
        showLoading(true)

        val updates = hashMapOf<String, Any>(
            "name" to name,
            "username" to username,
            "password" to password,
            "modality" to modality.name
        )

        db.collection(Constants.COLLECTION_USERS)
            .document(workerId)
            .update(updates)
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, "Trabajador actualizado exitosamente", Toast.LENGTH_SHORT).show()
                loadWorkers()
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Toast.makeText(this, "Error actualizando trabajador: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun toggleWorkerStatus(worker: Worker) {
        val newStatus = !worker.isActive
        val action = if (newStatus) "activado" else "desactivado"

        db.collection(Constants.COLLECTION_USERS)
            .document(worker.id)
            .update("isActive", newStatus)
            .addOnSuccessListener {
                Toast.makeText(this, "Trabajador $action exitosamente", Toast.LENGTH_SHORT).show()
                loadWorkers()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error actualizando estado: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
    }
}