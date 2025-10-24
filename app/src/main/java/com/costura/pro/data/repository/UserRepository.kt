package com.costura.pro.data.repository

import com.costura.pro.data.local.dao.UserDao
import com.costura.pro.data.local.entity.UserEntity
import com.costura.pro.data.model.User
import com.costura.pro.data.model.UserRole
import com.costura.pro.data.remote.FirebaseManager
import com.costura.pro.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.*
import com.costura.pro.data.model.FirebaseUser
import com.costura.pro.data.model.UserBasicInfo
import com.costura.pro.data.model.UserStats
import com.costura.pro.data.model.UserTimestamps
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.*


class UserRepository(
    private val userDao: UserDao
) {
    private val db = FirebaseFirestore.getInstance()

    suspend fun authenticateUser(username: String, password: String): User? {
        // Check local database first
        val localUser = userDao.getUserByCredentials(username, password)
        if (localUser != null) {
            return mapToUser(localUser)
        }

        // If not found locally, check Firebase con NUEVA ESTRUCTURA
        return try {
            val documents = db.collection(Constants.COLLECTION_USERS)
                .whereEqualTo("basicInfo.username", username)
                .whereEqualTo("basicInfo.password", password)
                .get()
                .await()

            if (documents.isEmpty) null
            else {
                val document = documents.documents[0]
                val basicInfoMap = document.get("basicInfo") as? Map<String, Any>
                val statsMap = document.get("stats") as? Map<String, Any>
                val timestampsMap = document.get("timestamps") as? Map<String, Any>

                if (basicInfoMap == null) {
                    return null
                }

                val user = User(
                    id = document.id,
                    username = basicInfoMap["username"] as? String ?: "",
                    password = basicInfoMap["password"] as? String ?: "",
                    role = UserRole.valueOf(basicInfoMap["role"] as? String ?: "WORKER")
                )

                // Cache user locally con nueva estructura
                val userEntity = createUserEntityFromFirebase(
                    documentId = document.id,
                    basicInfo = basicInfoMap,
                    stats = statsMap,
                    timestamps = timestampsMap
                )
                userDao.insertUser(userEntity)

                user
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun createUserEntityFromFirebase(
        documentId: String,
        basicInfo: Map<String, Any>,
        stats: Map<String, Any>?,
        timestamps: Map<String, Any>?
    ): UserEntity {
        return UserEntity(
            id = documentId,
            username = basicInfo["username"] as? String ?: "",
            password = basicInfo["password"] as? String ?: "",
            role = basicInfo["role"] as? String ?: "WORKER",
            name = basicInfo["name"] as? String ?: "",
            modality = basicInfo["modality"] as? String ?: "PIECE_RATE",
            isActive = true,
            createdAt = (timestamps?.get("createdAt") as? Timestamp)?.seconds ?: System.currentTimeMillis(),
            totalEarnings = (stats?.get("totalEarnings") as? Double) ?: 0.0,
            lastAttendanceDate = stats?.get("lastAttendanceDate") as? String,
            monthlyProduction = (stats?.get("monthlyProduction") as? Int) ?: 0
        )
    }

    suspend fun syncUsersFromFirebase() {
        try {
            val documents = db.collection(Constants.COLLECTION_USERS)
                .get()
                .await()

            val userEntities = documents.map { document ->
                val basicInfoMap = document.get("basicInfo") as? Map<String, Any> ?: emptyMap()
                val statsMap = document.get("stats") as? Map<String, Any> ?: emptyMap()
                val timestampsMap = document.get("timestamps") as? Map<String, Any> ?: emptyMap()

                createUserEntityFromFirebase(
                    documentId = document.id,
                    basicInfo = basicInfoMap,
                    stats = statsMap,
                    timestamps = timestampsMap
                )
            }

            userDao.insertAllUsers(userEntities)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // NUEVO: Crear usuario en estructura híbrida
    suspend fun createUserInFirebase(
        name: String,
        username: String,
        password: String,
        modality: String
    ): String? {
        return try {
            val userId = UUID.randomUUID().toString()

            val userData = hashMapOf(
                "basicInfo" to hashMapOf(
                    "name" to name,
                    "username" to username,
                    "password" to password,
                    "modality" to modality,
                    "role" to "WORKER"
                ),
                "stats" to hashMapOf(
                    "totalEarnings" to 0.0,
                    "monthlyProduction" to 0,
                    "workedDays" to 0,
                    "lastAttendanceDate" to null
                ),
                "timestamps" to hashMapOf(
                    "createdAt" to Timestamp.now(),
                    "lastActive" to Timestamp.now()
                )
            )

            db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .set(userData)
                .await()

            userId
        } catch (e: Exception) {
            null
        }
    }

    // NUEVO: Actualizar estadísticas de usuario
    suspend fun updateUserStats(userId: String, newEarnings: Double = 0.0, newProduction: Int = 0) {
        try {
            val updates = hashMapOf<String, Any>(
                "stats.totalEarnings" to com.google.firebase.firestore.FieldValue.increment(newEarnings),
                "stats.monthlyProduction" to com.google.firebase.firestore.FieldValue.increment(newProduction.toDouble()),
                "stats.workedDays" to com.google.firebase.firestore.FieldValue.increment(1),
                "timestamps.lastActive" to Timestamp.now(),
                "stats.lastAttendanceDate" to getCurrentDateString()
            )

            db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .update(updates)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getCurrentDateString(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return String.format("%04d-%02d-%02d", year, month, day)
    }

    private fun mapToUser(entity: UserEntity): User {
        return User(
            id = entity.id,
            username = entity.username,
            password = entity.password,
            role = UserRole.valueOf(entity.role)
        )
    }
}