package com.costura.pro.data.local.dao

import androidx.room.*
import com.costura.pro.data.local.entity.MachineEntity
import com.costura.pro.data.local.entity.MachineStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MachineDao {

    @Query("SELECT * FROM machines ORDER BY name ASC")
    fun getAllMachines(): Flow<List<MachineEntity>>

    @Query("SELECT * FROM machines WHERE status = :status ORDER BY name ASC")
    fun getMachinesByStatus(status: MachineStatus): Flow<List<MachineEntity>>

    @Query("SELECT * FROM machines WHERE id = :machineId")
    suspend fun getMachineById(machineId: String): MachineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMachine(machine: MachineEntity)

    @Update
    suspend fun updateMachine(machine: MachineEntity)

    @Delete
    suspend fun deleteMachine(machine: MachineEntity)

    @Query("DELETE FROM machines")
    suspend fun clearAll()
}