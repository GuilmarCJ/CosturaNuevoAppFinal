package com.costura.pro.data.local.dao

import androidx.room.*
import com.costura.pro.data.local.entity.MachineHistoryEntity
import com.costura.pro.data.local.entity.HistoryType
import kotlinx.coroutines.flow.Flow

@Dao
interface MachineHistoryDao {

    @Query("SELECT * FROM machine_history ORDER BY date DESC")
    fun getAllHistory(): Flow<List<MachineHistoryEntity>>

    @Query("SELECT * FROM machine_history WHERE machineId = :machineId ORDER BY date DESC")
    fun getHistoryByMachine(machineId: String): Flow<List<MachineHistoryEntity>>

    @Query("SELECT * FROM machine_history WHERE type = :type ORDER BY date DESC")
    fun getHistoryByType(type: HistoryType): Flow<List<MachineHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: MachineHistoryEntity)

    @Query("DELETE FROM machine_history WHERE machineId = :machineId")
    suspend fun deleteHistoryByMachine(machineId: String)

    @Query("DELETE FROM machine_history")
    suspend fun clearAll()
}