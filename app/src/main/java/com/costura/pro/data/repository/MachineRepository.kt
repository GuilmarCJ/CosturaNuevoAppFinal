package com.costura.pro.data.repository

import com.costura.pro.data.local.dao.MachineDao
import com.costura.pro.data.local.dao.MachineHistoryDao
import com.costura.pro.data.local.entity.HistoryType
import com.costura.pro.data.local.entity.MachineEntity
import com.costura.pro.data.local.entity.MachineHistoryEntity
import com.costura.pro.data.local.entity.MachineStatus
import kotlinx.coroutines.flow.Flow
import java.util.*

class MachineRepository(
    private val machineDao: MachineDao,
    private val machineHistoryDao: MachineHistoryDao
) {

    fun getAllMachines(): Flow<List<MachineEntity>> {
        return machineDao.getAllMachines()
    }

    fun getMachinesByStatus(status: MachineStatus): Flow<List<MachineEntity>> {
        return machineDao.getMachinesByStatus(status)
    }

    suspend fun getMachineById(machineId: String): MachineEntity? {
        return machineDao.getMachineById(machineId)
    }

    suspend fun createMachine(
        name: String,
        machineNumber: String,
        type: String,
        description: String
    ): Boolean {
        return try {
            val machineId = UUID.randomUUID().toString()
            val machine = MachineEntity(
                id = machineId,
                name = name,
                machineNumber = machineNumber,
                type = type,
                description = description,
                status = MachineStatus.OPERATIONAL
            )
            machineDao.insertMachine(machine)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateMachine(machine: MachineEntity): Boolean {
        return try {
            machineDao.updateMachine(machine)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getAllHistory(): Flow<List<MachineHistoryEntity>> {
        return machineHistoryDao.getAllHistory()
    }

    fun getHistoryByMachine(machineId: String): Flow<List<MachineHistoryEntity>> {
        return machineHistoryDao.getHistoryByMachine(machineId)
    }

    suspend fun reportProblem(machineId: String, problemDescription: String): Boolean {
        return try {
            val machine = machineDao.getMachineById(machineId)
            machine?.let {
                // Actualizar estado de la máquina
                val updatedMachine = it.copy(
                    status = MachineStatus.MAINTENANCE
                )
                machineDao.updateMachine(updatedMachine)

                // Guardar en historial
                val historyId = UUID.randomUUID().toString()
                val historyEntry = MachineHistoryEntity(
                    id = historyId,
                    machineId = machineId,
                    machineName = it.name,
                    machineNumber = it.machineNumber,
                    type = HistoryType.PROBLEM_REPORTED,
                    description = problemDescription
                )
                machineHistoryDao.insertHistory(historyEntry)
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun markAsRepaired(machineId: String, solution: String): Boolean {
        return try {
            val machine = machineDao.getMachineById(machineId)
            machine?.let {
                // Actualizar estado de la máquina
                val updatedMachine = it.copy(
                    status = MachineStatus.OPERATIONAL,
                    lastMaintenance = System.currentTimeMillis()
                )
                machineDao.updateMachine(updatedMachine)

                // Guardar en historial
                val historyId = UUID.randomUUID().toString()
                val historyEntry = MachineHistoryEntity(
                    id = historyId,
                    machineId = machineId,
                    machineName = it.name,
                    machineNumber = it.machineNumber,
                    type = HistoryType.PROBLEM_SOLVED,
                    description = "Problema solucionado",
                    solution = solution
                )
                machineHistoryDao.insertHistory(historyEntry)
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteMachine(machineId: String): Boolean {
        return try {
            val machine = machineDao.getMachineById(machineId)
            machine?.let {
                // Eliminar historial de la máquina primero
                machineHistoryDao.deleteHistoryByMachine(machineId)
                // Luego eliminar la máquina
                machineDao.deleteMachine(it)
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

}