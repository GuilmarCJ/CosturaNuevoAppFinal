package com.costura.pro.data.repository


import com.costura.pro.data.local.dao.ProductionDao
import com.costura.pro.data.local.entity.ProductionEntity
import com.costura.pro.data.model.FirebaseProduction
import com.costura.pro.utils.Constants
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import org.joda.time.DateTime
import java.util.*

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
            val currentTime = System.currentTimeMillis()
            val date = Date(currentTime)
            val yearMonth = DateTime().toString(Constants.YEAR_MONTH_FORMAT)
            val productionId = UUID.randomUUID().toString()

            // Guardar en Firebase con NUEVA ESTRUCTURA
            val success = saveProductionToFirebase(
                productionId = productionId,
                workerId = workerId,
                operationId = operationId,
                operationName = operationName,
                quantity = quantity,
                paymentPerUnit = paymentPerUnit,
                totalPayment = totalPayment,
                date = date,
                yearMonth = yearMonth
            )

            if (success) {
                // Actualizar estadísticas del usuario
                updateUserProductionStats(workerId, totalPayment, quantity)

                // Guardar localmente
                val productionEntity = ProductionEntity(
                    id = productionId,
                    workerId = workerId,
                    operationId = operationId,
                    operationName = operationName,
                    quantity = quantity,
                    paymentPerUnit = paymentPerUnit,
                    totalPayment = totalPayment,
                    date = currentTime,
                    isSynced = true,
                    yearMonth = yearMonth
                )

                productionDao.insertProduction(productionEntity)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun saveProductionToFirebase(
        productionId: String,
        workerId: String,
        operationId: String,
        operationName: String,
        quantity: Int,
        paymentPerUnit: Double,
        totalPayment: Double,
        date: Date,
        yearMonth: String
    ): Boolean {
        return try {
            val firebaseProduction = FirebaseProduction(
                id = productionId,
                operationId = operationId,
                operationName = operationName,
                quantity = quantity,
                paymentPerUnit = paymentPerUnit,
                totalPayment = totalPayment,
                date = date,
                yearMonth = yearMonth
            )

            // NUEVA ESTRUCTURA: Guardar en subcolección del usuario
            db.collection(Constants.COLLECTION_USERS)
                .document(workerId)
                .collection(Constants.SUBCOLLECTION_PRODUCTION)
                .document(productionId)
                .set(firebaseProduction)
                .await()

            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun updateUserProductionStats(workerId: String, earnings: Double, production: Int) {
        try {
            val updates = hashMapOf<String, Any>(
                "stats.totalEarnings" to com.google.firebase.firestore.FieldValue.increment(earnings),
                "stats.monthlyProduction" to com.google.firebase.firestore.FieldValue.increment(production.toDouble()),
                "timestamps.lastActive" to Timestamp.now()
            )

            db.collection(Constants.COLLECTION_USERS)
                .document(workerId)
                .update(updates)
                .await()
        } catch (e: Exception) {
            // Handle error
        }
    }

    fun getProductionByWorker(workerId: String): Flow<List<ProductionEntity>> {
        return productionDao.getProductionByWorker(workerId)
    }

    suspend fun getTotalEarnings(workerId: String): Double? {
        return try {
            // Consultar directamente a Firebase para datos actualizados
            val documents = db.collection(Constants.COLLECTION_USERS)
                .document(workerId)
                .collection(Constants.SUBCOLLECTION_PRODUCTION)
                .get()
                .await()

            var totalEarnings = 0.0
            for (document in documents) {
                val production = document.toObject(FirebaseProduction::class.java)
                totalEarnings += production.totalPayment
            }

            totalEarnings
        } catch (e: Exception) {
            // Fallback a base local
            productionDao.getTotalEarnings(workerId)
        }
    }

    suspend fun getTodayProduction(workerId: String): List<ProductionEntity> {
        return try {
            val startOfDay = DateTime()
                .withTimeAtStartOfDay()
                .millis

            val documents = db.collection(Constants.COLLECTION_USERS)
                .document(workerId)
                .collection(Constants.SUBCOLLECTION_PRODUCTION)
                .whereGreaterThanOrEqualTo("date", Timestamp(Date(startOfDay)))
                .get()
                .await()

            documents.map { document ->
                val firebaseProduction = document.toObject(FirebaseProduction::class.java)
                ProductionEntity(
                    id = firebaseProduction.id,
                    workerId = workerId,
                    operationId = firebaseProduction.operationId,
                    operationName = firebaseProduction.operationName,
                    quantity = firebaseProduction.quantity,
                    paymentPerUnit = firebaseProduction.paymentPerUnit,
                    totalPayment = firebaseProduction.totalPayment,
                    date = firebaseProduction.date.time,
                    isSynced = true,
                    yearMonth = firebaseProduction.yearMonth
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun syncUnsyncedProduction() {
        try {
            val unsyncedRecords = productionDao.getUnsyncedProduction()

            for (record in unsyncedRecords) {
                val firebaseProduction = FirebaseProduction(
                    id = record.id,
                    operationId = record.operationId,
                    operationName = record.operationName,
                    quantity = record.quantity,
                    paymentPerUnit = record.paymentPerUnit,
                    totalPayment = record.totalPayment,
                    date = Date(record.date),
                    yearMonth = record.yearMonth
                )

                db.collection(Constants.COLLECTION_USERS)
                    .document(record.workerId)
                    .collection(Constants.SUBCOLLECTION_PRODUCTION)
                    .document(record.id)
                    .set(firebaseProduction)
                    .await()

                productionDao.markAsSynced(record.id)
            }
        } catch (e: Exception) {
            // Handle sync error
        }
    }
}