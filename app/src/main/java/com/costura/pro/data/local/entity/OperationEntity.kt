package com.costura.pro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "operations")
data class OperationEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val paymentPerUnit: Double,
    val isActive: Boolean,
    val createdAt: Long,
    val lastSync: Long = System.currentTimeMillis()
)