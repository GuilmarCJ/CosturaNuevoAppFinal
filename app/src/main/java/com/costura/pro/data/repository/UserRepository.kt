package com.costura.pro.data.repository

import com.costura.pro.data.local.dao.UserDao
import com.costura.pro.data.local.entity.UserEntity
import com.costura.pro.data.model.User
import com.costura.pro.data.model.UserRole
import com.costura.pro.data.remote.FirebaseManager
import com.costura.pro.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
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

        // If not found locally, check Firebase
        return try {
            val documents = db.collection(Constants.COLLECTION_USERS)
                .whereEqualTo("username", username)
                .whereEqualTo("password", password)
                .whereEqualTo("isActive", true)
                .get()
                .await()

            if (documents.isEmpty) null
            else {
                val document = documents.documents[0]
                val user = User(
                    id = document.id,
                    username = document.getString("username") ?: "",
                    password = document.getString("password") ?: "",
                    role = UserRole.valueOf(document.getString("role") ?: "WORKER")
                )

                // Cache user locally
                userDao.insertUser(mapToUserEntity(user, document))
                user
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun syncUsersFromFirebase() {
        try {
            val documents = db.collection(Constants.COLLECTION_USERS)
                .get()
                .await()

            val userEntities = documents.map { document ->
                UserEntity(
                    id = document.id,
                    username = document.getString("username") ?: "",
                    password = document.getString("password") ?: "",
                    role = document.getString("role") ?: "WORKER",
                    name = document.getString("name") ?: "",
                    modality = document.getString("modality") ?: "PIECE_RATE",
                    isActive = document.getBoolean("isActive") ?: true,
                    createdAt = (document.get("createdAt") as? com.google.firebase.Timestamp)?.seconds ?: System.currentTimeMillis()
                )
            }

            userDao.insertAllUsers(userEntities)
        } catch (e: Exception) {
            // Handle error
        }
    }

    private fun mapToUser(entity: UserEntity): User {
        return User(
            id = entity.id,
            username = entity.username,
            password = entity.password,
            role = UserRole.valueOf(entity.role)
        )
    }

    private fun mapToUserEntity(user: User, document: com.google.firebase.firestore.DocumentSnapshot): UserEntity {
        return UserEntity(
            id = document.id,
            username = user.username,
            password = user.password,
            role = user.role.name,
            name = document.getString("name") ?: "",
            modality = document.getString("modality") ?: "PIECE_RATE",
            isActive = document.getBoolean("isActive") ?: true,
            createdAt = (document.get("createdAt") as? com.google.firebase.Timestamp)?.seconds ?: System.currentTimeMillis()
        )
    }
}