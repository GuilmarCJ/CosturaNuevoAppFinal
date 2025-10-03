package com.costura.pro.data.repository

import com.costura.pro.data.local.dao.OperationDao
import com.costura.pro.data.local.entity.OperationEntity
import com.costura.pro.data.model.Operation
import com.costura.pro.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class OperationRepository(
    private val operationDao: OperationDao
) {
    private val db = FirebaseFirestore.getInstance()

    fun getActiveOperations(): Flow<List<Operation>> {
        return operationDao.getActiveOperations().map { entities ->
            entities.map { entity ->
                Operation(
                    id = entity.id,
                    name = entity.name,
                    paymentPerUnit = entity.paymentPerUnit,
                    isActive = entity.isActive
                )
            }
        }
    }

    suspend fun syncOperationsFromFirebase() {
        try {
            val documents = db.collection(Constants.COLLECTION_OPERATIONS)
                .get()
                .await()

            val operationEntities = documents.map { document ->
                OperationEntity(
                    id = document.id,
                    name = document.getString("name") ?: "",
                    paymentPerUnit = document.getDouble("paymentPerUnit") ?: 0.0,
                    isActive = document.getBoolean("isActive") ?: true,
                    createdAt = (document.get("createdAt") as? com.google.firebase.Timestamp)?.seconds ?: System.currentTimeMillis()
                )
            }

            operationDao.insertAllOperations(operationEntities)
        } catch (e: Exception) {
            // Handle error
        }
    }

    suspend fun createOperation(name: String, paymentPerUnit: Double): Boolean {
        return try {
            val operationData = hashMapOf(
                "name" to name,
                "paymentPerUnit" to paymentPerUnit,
                "isActive" to true,
                "createdAt" to com.google.firebase.Timestamp.now()
            )

            val documentReference = db.collection(Constants.COLLECTION_OPERATIONS)
                .add(operationData)
                .await()

            // Sync the new operation to local database
            syncOperationsFromFirebase()
            true
        } catch (e: Exception) {
            false
        }
    }
}