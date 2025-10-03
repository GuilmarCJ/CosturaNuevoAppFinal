package com.costura.pro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

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
    val lastSync: Long = System.currentTimeMillis()
)