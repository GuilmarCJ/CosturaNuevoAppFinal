package com.costura.pro.data.local.dao

import androidx.room.*
import com.costura.pro.data.local.entity.OperationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OperationDao {

    @Query("SELECT * FROM operations WHERE isActive = 1")
    fun getActiveOperations(): Flow<List<OperationEntity>>

    @Query("SELECT * FROM operations")
    fun getAllOperations(): Flow<List<OperationEntity>>

    @Query("SELECT * FROM operations WHERE id = :operationId")
    suspend fun getOperationById(operationId: String): OperationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(operation: OperationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllOperations(operations: List<OperationEntity>)

    @Query("UPDATE operations SET isActive = :isActive WHERE id = :operationId")
    suspend fun updateOperationStatus(operationId: String, isActive: Boolean)

    @Query("DELETE FROM operations")
    suspend fun clearAll()
}