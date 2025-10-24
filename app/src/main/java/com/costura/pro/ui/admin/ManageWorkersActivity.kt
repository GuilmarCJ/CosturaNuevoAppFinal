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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class ManageWorkersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageWorkersBinding
    private val db = FirebaseFirestore.getInstance()
    private val workersList = mutableListOf<Worker>()
    private lateinit var workersAdapter: WorkersAdapter

    // NUEVO: Scope para corrutinas
    private val scope = CoroutineScope(Dispatchers.Main)

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

        scope.launch {
            try {
                val documents = db.collection(Constants.COLLECTION_USERS)
                    .whereEqualTo("basicInfo.role", "WORKER")
                    .get()
                    .await()

                workersList.clear()
                for (document in documents) {
                    val userData = document.get("basicInfo") as? Map<String, Any>
                    val statsData = document.get("stats") as? Map<String, Any>

                    val worker = Worker(
                        id = document.id,
                        name = userData?.get("name") as? String ?: "",
                        username = userData?.get("username") as? String ?: "",
                        password = userData?.get("password") as? String ?: "",
                        modality = WorkModality.valueOf(userData?.get("modality") as? String ?: "PIECE_RATE"),
                        isActive = true // Por defecto activo en nueva estructura
                    )
                    workersList.add(worker)
                }
                workersAdapter.notifyDataSetChanged()
                showLoading(false)
            } catch (exception: Exception) {
                showLoading(false)
                Toast.makeText(this@ManageWorkersActivity, "Error cargando trabajadores: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
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

        scope.launch {
            try {
                // Verificar si el usuario ya existe
                val existingUsers = db.collection(Constants.COLLECTION_USERS)
                    .whereEqualTo("basicInfo.username", username)
                    .get()
                    .await()

                if (!existingUsers.isEmpty) {
                    showLoading(false)
                    Toast.makeText(this@ManageWorkersActivity, "El usuario ya existe", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Crear usuario con NUEVA ESTRUCTURA
                val userData = hashMapOf(
                    "basicInfo" to hashMapOf(
                        "name" to name,
                        "username" to username,
                        "password" to password,
                        "modality" to modality.name,
                        "role" to "WORKER"
                    ),
                    "stats" to hashMapOf(
                        "totalEarnings" to 0.0,
                        "monthlyProduction" to 0,
                        "workedDays" to 0,
                        "lastAttendanceDate" to null
                    ),
                    "timestamps" to hashMapOf(
                        "createdAt" to com.google.firebase.Timestamp.now(),
                        "lastActive" to com.google.firebase.Timestamp.now()
                    )
                )

                val documentReference = db.collection(Constants.COLLECTION_USERS)
                    .add(userData)
                    .await()

                showLoading(false)
                Toast.makeText(this@ManageWorkersActivity, "Trabajador creado exitosamente", Toast.LENGTH_SHORT).show()
                loadWorkers()
            } catch (exception: Exception) {
                showLoading(false)
                Toast.makeText(this@ManageWorkersActivity, "Error creando trabajador: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateWorker(workerId: String, name: String, username: String, password: String, modality: WorkModality) {
        showLoading(true)

        scope.launch {
            try {
                val updates = hashMapOf<String, Any>(
                    "basicInfo.name" to name,
                    "basicInfo.username" to username,
                    "basicInfo.password" to password,
                    "basicInfo.modality" to modality.name
                )

                db.collection(Constants.COLLECTION_USERS)
                    .document(workerId)
                    .update(updates)
                    .await()

                showLoading(false)
                Toast.makeText(this@ManageWorkersActivity, "Trabajador actualizado exitosamente", Toast.LENGTH_SHORT).show()
                loadWorkers()
            } catch (exception: Exception) {
                showLoading(false)
                Toast.makeText(this@ManageWorkersActivity, "Error actualizando trabajador: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleWorkerStatus(worker: Worker) {
        // En la nueva estructura, no tenemos campo isActive, podemos usar lastActive para determinar estado
        Toast.makeText(this, "En nueva estructura, los usuarios siempre están activos", Toast.LENGTH_SHORT).show()
        loadWorkers()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
    }
}