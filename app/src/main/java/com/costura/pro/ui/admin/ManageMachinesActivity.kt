package com.costura.pro.ui.admin

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.costura.pro.data.local.entity.MachineEntity
import com.costura.pro.data.local.entity.MachineStatus
import com.costura.pro.databinding.ActivityManageMachinesBinding
import com.costura.pro.databinding.DialogAddMachineBinding
import com.costura.pro.databinding.DialogMaintenanceBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class ManageMachinesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageMachinesBinding
    private val scope = CoroutineScope(Dispatchers.Main)
    private val machinesList = mutableListOf<MachineEntity>()
    private lateinit var machinesAdapter: MachinesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageMachinesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        loadMachines()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        machinesAdapter = MachinesAdapter(machinesList) { machine, action ->
            when (action) {
                "edit" -> showEditMachineDialog(machine)
                "delete" -> showDeleteConfirmation(machine)
                "maintenance" -> showMaintenanceDialog(machine)
                "history" -> showMachineHistory(machine)  // NUEVO
            }
        }
        binding.rvMachines.apply {
            layoutManager = LinearLayoutManager(this@ManageMachinesActivity)
            adapter = machinesAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnAddMachine.setOnClickListener {
            showAddMachineDialog()
        }
    }

    private fun showMachineHistory(machine: MachineEntity) {
        val intent = Intent(this, MachineHistoryActivity::class.java)
        // Puedes pasar el machineId si quieres filtrar por máquina específica
        // intent.putExtra("MACHINE_ID", machine.id)
        startActivity(intent)
    }

    private fun loadMachines() {
        showLoading(true)

        scope.launch {
            try {
                val machineRepository = (application as com.costura.pro.CosturaProApp).machineRepository
                val machinesFlow = withContext(Dispatchers.IO) {
                    machineRepository.getAllMachines()
                }

                machinesFlow.collect { machines ->
                    machinesList.clear()
                    machinesList.addAll(machines)
                    machinesAdapter.notifyDataSetChanged()
                    showLoading(false)
                    updateEmptyState()
                }
            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(this@ManageMachinesActivity, "Error cargando máquinas", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun showAddMachineDialog() {
        val dialogBinding = DialogAddMachineBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setTitle("AGREGAR MÁQUINA")
            .setView(dialogBinding.root)
            .setPositiveButton("CREAR MÁQUINA", null)
            .setNegativeButton("CANCELAR", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val name = dialogBinding.etMachineName.text.toString().trim()
                val number = dialogBinding.etMachineNumber.text.toString().trim()
                val type = dialogBinding.etMachineType.text.toString().trim()
                val description = dialogBinding.etMachineDescription.text.toString().trim()

                if (validateMachineInputs(name, number, type)) {
                    createMachine(name, number, type, description)
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun showEditMachineDialog(machine: MachineEntity) {
        val dialogBinding = DialogAddMachineBinding.inflate(layoutInflater)
        dialogBinding.etMachineName.setText(machine.name)
        dialogBinding.etMachineNumber.setText(machine.machineNumber)
        dialogBinding.etMachineType.setText(machine.type)
        dialogBinding.etMachineDescription.setText(machine.description)

        val dialog = AlertDialog.Builder(this)
            .setTitle("EDITAR MÁQUINA")
            .setView(dialogBinding.root)
            .setPositiveButton("ACTUALIZAR", null)
            .setNegativeButton("CANCELAR", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val name = dialogBinding.etMachineName.text.toString().trim()
                val number = dialogBinding.etMachineNumber.text.toString().trim()
                val type = dialogBinding.etMachineType.text.toString().trim()
                val description = dialogBinding.etMachineDescription.text.toString().trim()

                if (validateMachineInputs(name, number, type)) {
                    updateMachine(machine.id, name, number, type, description)
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun showMaintenanceDialog(machine: MachineEntity) {
        val dialogBinding = DialogMaintenanceBinding.inflate(layoutInflater)

        val dialogTitle = if (machine.status == MachineStatus.OPERATIONAL) {
            "REPORTAR PROBLEMA"
        } else {
            "REGISTRAR REPARACIÓN"
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(dialogTitle)
            .setView(dialogBinding.root)
            .setPositiveButton("GUARDAR", null)
            .setNegativeButton("CANCELAR", null)
            .create()

        if (machine.status == MachineStatus.OPERATIONAL) {
            // Reportar problema
            dialogBinding.tvDialogTitle.text = "Describa el problema de la máquina:"
            dialogBinding.etMaintenanceText.hint = "Descripción del problema..."
        } else {
            // Registrar reparación
            dialogBinding.tvDialogTitle.text = "Describa la solución aplicada:"
            dialogBinding.etMaintenanceText.hint = "Solución aplicada..."
        }

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val text = dialogBinding.etMaintenanceText.text.toString().trim()

                if (text.isEmpty()) {
                    Toast.makeText(this, "Debe ingresar una descripción", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (machine.status == MachineStatus.OPERATIONAL) {
                    reportProblem(machine.id, text)
                } else {
                    markAsRepaired(machine.id, text)
                }
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showDeleteConfirmation(machine: MachineEntity) {
        AlertDialog.Builder(this)
            .setTitle("ELIMINAR MÁQUINA")
            .setMessage("¿Está seguro de eliminar la máquina ${machine.name}?")
            .setPositiveButton("ELIMINAR") { _, _ ->
                deleteMachine(machine.id)
            }
            .setNegativeButton("CANCELAR", null)
            .show()
    }

    private fun validateMachineInputs(name: String, number: String, type: String): Boolean {
        if (name.isEmpty()) {
            Toast.makeText(this, "Ingrese el nombre de la máquina", Toast.LENGTH_SHORT).show()
            return false
        }
        if (number.isEmpty()) {
            Toast.makeText(this, "Ingrese el número de máquina", Toast.LENGTH_SHORT).show()
            return false
        }
        if (type.isEmpty()) {
            Toast.makeText(this, "Ingrese el tipo de máquina", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun createMachine(name: String, number: String, type: String, description: String) {
        showLoading(true)

        scope.launch {
            try {
                val machineRepository = (application as com.costura.pro.CosturaProApp).machineRepository
                val success = machineRepository.createMachine(name, number, type, description)

                if (success) {
                    Toast.makeText(this@ManageMachinesActivity, "Máquina creada exitosamente", Toast.LENGTH_SHORT).show()
                    loadMachines()
                } else {
                    Toast.makeText(this@ManageMachinesActivity, "Error creando máquina", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                }
            } catch (e: Exception) {
                Toast.makeText(this@ManageMachinesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
        }
    }

    private fun updateMachine(machineId: String, name: String, number: String, type: String, description: String) {
        showLoading(true)

        scope.launch {
            try {
                val machineRepository = (application as com.costura.pro.CosturaProApp).machineRepository
                val existingMachine = machineRepository.getMachineById(machineId)

                if (existingMachine != null) {
                    val updatedMachine = existingMachine.copy(
                        name = name,
                        machineNumber = number,
                        type = type,
                        description = description
                    )
                    val success = machineRepository.updateMachine(updatedMachine)

                    if (success) {
                        Toast.makeText(this@ManageMachinesActivity, "Máquina actualizada", Toast.LENGTH_SHORT).show()
                        loadMachines()
                    } else {
                        Toast.makeText(this@ManageMachinesActivity, "Error actualizando máquina", Toast.LENGTH_SHORT).show()
                        showLoading(false)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@ManageMachinesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
        }
    }

    private fun deleteMachine(machineId: String) {
        showLoading(true)

        scope.launch {
            try {
                val machineRepository = (application as com.costura.pro.CosturaProApp).machineRepository
                val success = machineRepository.deleteMachine(machineId)

                if (success) {
                    Toast.makeText(this@ManageMachinesActivity, "Máquina eliminada", Toast.LENGTH_SHORT).show()
                    loadMachines()
                } else {
                    Toast.makeText(this@ManageMachinesActivity, "Error eliminando máquina", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                }
            } catch (e: Exception) {
                Toast.makeText(this@ManageMachinesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
        }
    }

    private fun reportProblem(machineId: String, problem: String) {
        showLoading(true)

        scope.launch {
            try {
                val machineRepository = (application as com.costura.pro.CosturaProApp).machineRepository
                val success = machineRepository.reportProblem(machineId, problem)

                if (success) {
                    Toast.makeText(this@ManageMachinesActivity, "Problema reportado", Toast.LENGTH_SHORT).show()
                    loadMachines()
                } else {
                    Toast.makeText(this@ManageMachinesActivity, "Error reportando problema", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                }
            } catch (e: Exception) {
                Toast.makeText(this@ManageMachinesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
        }
    }

    private fun markAsRepaired(machineId: String, solution: String) {
        showLoading(true)

        scope.launch {
            try {
                val machineRepository = (application as com.costura.pro.CosturaProApp).machineRepository
                val success = machineRepository.markAsRepaired(machineId, solution)

                if (success) {
                    Toast.makeText(this@ManageMachinesActivity, "Máquina marcada como reparada", Toast.LENGTH_SHORT).show()
                    loadMachines()
                } else {
                    Toast.makeText(this@ManageMachinesActivity, "Error actualizando estado", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                }
            } catch (e: Exception) {
                Toast.makeText(this@ManageMachinesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun updateEmptyState() {
        binding.tvEmptyMachines.visibility = if (machinesList.isEmpty()) View.VISIBLE else View.GONE
        binding.rvMachines.visibility = if (machinesList.isEmpty()) View.GONE else View.VISIBLE
    }
}