package com.costura.pro.data.local.dao

import androidx.room.*
import com.costura.pro.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE isActive = 1")
    fun getActiveUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE username = :username AND password = :password AND isActive = 1")
    suspend fun getUserByCredentials(username: String, password: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllUsers(users: List<UserEntity>)

    @Query("UPDATE users SET isActive = :isActive WHERE id = :userId")
    suspend fun updateUserStatus(userId: String, isActive: Boolean)

    // NUEVOS MÉTODOS para estadísticas
    @Query("UPDATE users SET totalEarnings = :earnings, monthlyProduction = :production, lastAttendanceDate = :attendanceDate WHERE id = :userId")
    suspend fun updateUserStats(userId: String, earnings: Double, production: Int, attendanceDate: String?)

    @Query("SELECT totalEarnings FROM users WHERE id = :userId")
    suspend fun getUserEarnings(userId: String): Double?

    @Query("DELETE FROM users")
    suspend fun clearAll()
}