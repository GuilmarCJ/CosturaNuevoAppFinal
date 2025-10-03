package com.costura.pro.data.repository

import com.costura.pro.data.local.dao.ProductionDao
import com.costura.pro.data.local.entity.ProductionEntity
import com.costura.pro.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class ProductionRepository(
    private val productionDao: ProductionDao
) {
    private val db = FirebaseFirestore.getInstance()

    suspend fun registerProduction(
        workerId: String,
        operationId: String,
        operationName: String,
        paymentPerUnit: Double,
        quantity: Int
    ): Boolean {
        return try {
            val totalPayment = quantity * paymentPerUnit

            // Guardar en Firebase
            val productionData = hashMapOf(
                "workerId" to workerId,
                "operationId" to operationId,
                "operationName" to operationName,
                "quantity" to quantity,
                "paymentPerUnit" to paymentPerUnit,
                "totalPayment" to totalPayment,
                "date" to com.google.firebase.Timestamp.now()
            )

            val documentReference = db.collection(Constants.COLLECTION_PRODUCTION)
                .add(productionData)
                .await()

            // Guardar localmente
            val productionEntity = ProductionEntity(
                id = documentReference.id,
                workerId = workerId,
                operationId = operationId,
                operationName = operationName,
                quantity = quantity,
                paymentPerUnit = paymentPerUnit,
                totalPayment = totalPayment,
                date = System.currentTimeMillis(),
                isSynced = true
            )

            productionDao.insertProduction(productionEntity)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getProductionByWorker(workerId: String): Flow<List<ProductionEntity>> {
        return productionDao.getProductionByWorker(workerId)
    }



    suspend fun getTotalEarnings(workerId: String): Double? {
        return productionDao.getTotalEarnings(workerId)
    }

    suspend fun syncUnsyncedProduction() {
        try {
            val unsyncedRecords = productionDao.getUnsyncedProduction()

            for (record in unsyncedRecords) {
                val productionData = hashMapOf(
                    "workerId" to record.workerId,
                    "operationId" to record.operationId,
                    "operationName" to record.operationName,
                    "quantity" to record.quantity,
                    "paymentPerUnit" to record.paymentPerUnit,
                    "totalPayment" to record.totalPayment,
                    "date" to com.google.firebase.Timestamp(record.date / 1000, 0)
                )

                val documentReference = db.collection(Constants.COLLECTION_PRODUCTION)
                    .add(productionData)
                    .await()

                productionDao.markAsSynced(record.id)
            }
        } catch (e: Exception) {
            // Handle sync error
        }
    }
}