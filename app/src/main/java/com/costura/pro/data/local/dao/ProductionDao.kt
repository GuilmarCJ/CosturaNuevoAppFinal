package com.costura.pro.data.local.dao

import androidx.room.*
import com.costura.pro.data.local.entity.ProductionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductionDao {

    @Query("SELECT * FROM production_records WHERE workerId = :workerId")
    fun getProductionByWorker(workerId: String): Flow<List<ProductionEntity>>

    @Query("SELECT * FROM production_records WHERE workerId = :workerId AND date >= :startOfDay")
    fun getTodayProduction(workerId: String, startOfDay: Long): Flow<List<ProductionEntity>>

    // NUEVO: Consulta por mes y año
    @Query("SELECT * FROM production_records WHERE workerId = :workerId AND yearMonth = :yearMonth")
    fun getProductionByWorkerAndMonth(workerId: String, yearMonth: String): Flow<List<ProductionEntity>>

    @Query("SELECT SUM(totalPayment) FROM production_records WHERE workerId = :workerId")
    suspend fun getTotalEarnings(workerId: String): Double?

    // NUEVO: Ganancias por mes
    @Query("SELECT SUM(totalPayment) FROM production_records WHERE workerId = :workerId AND yearMonth = :yearMonth")
    suspend fun getMonthlyEarnings(workerId: String, yearMonth: String): Double?

    @Query("SELECT SUM(quantity) FROM production_records WHERE workerId = :workerId AND operationId = :operationId AND date >= :startOfDay")
    suspend fun getTodayQuantity(workerId: String, operationId: String, startOfDay: Long): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduction(production: ProductionEntity)

    @Query("SELECT * FROM production_records WHERE isSynced = 0")
    suspend fun getUnsyncedProduction(): List<ProductionEntity>

    @Query("UPDATE production_records SET isSynced = 1 WHERE id = :productionId")
    suspend fun markAsSynced(productionId: String)

    @Query("DELETE FROM production_records WHERE date < :timestamp")
    suspend fun deleteOldRecords(timestamp: Long)
}