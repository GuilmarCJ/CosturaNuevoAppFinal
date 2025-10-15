package com.costura.pro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val username: String,
    val password: String,
    val role: String,
    val name: String,
    val modality: String,
    val isActive: Boolean,
    val createdAt: Long,
    val lastSync: Long = System.currentTimeMillis(),
    // Nuevos campos para estadísticas
    val totalEarnings: Double = 0.0,
    val lastAttendanceDate: String? = null,
    val monthlyProduction: Int = 0
)