package com.costura.pro.data.repository

import com.costura.pro.data.local.dao.MachineDao
import com.costura.pro.data.local.entity.MachineEntity
import com.costura.pro.data.local.entity.MachineStatus
import kotlinx.coroutines.flow.Flow
import java.util.*

class MachineRepository(
    private val machineDao: MachineDao
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

    suspend fun deleteMachine(machineId: String): Boolean {
        return try {
            val machine = machineDao.getMachineById(machineId)
            machine?.let {
                machineDao.deleteMachine(it)
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun reportProblem(machineId: String, problemDescription: String): Boolean {
        return try {
            val machine = machineDao.getMachineById(machineId)
            machine?.let {
                val updatedMachine = it.copy(
                    status = MachineStatus.MAINTENANCE
                )
                machineDao.updateMachine(updatedMachine)
                // Aquí podrías guardar el problema en otra tabla si quieres historial
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
                val updatedMachine = it.copy(
                    status = MachineStatus.OPERATIONAL,
                    lastMaintenance = System.currentTimeMillis()
                )
                machineDao.updateMachine(updatedMachine)
                // Aquí podrías guardar la solución en otra tabla si quieres historial
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
}