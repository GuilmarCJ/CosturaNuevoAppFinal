package com.costura.pro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "production_records")
data class ProductionEntity(
    @PrimaryKey
    val id: String,
    val workerId: String,
    val operationId: String,
    val operationName: String,
    val quantity: Int,
    val paymentPerUnit: Double,
    val totalPayment: Double,
    val date: Long,
    val isSynced: Boolean = false,
    val lastSync: Long = System.currentTimeMillis()
)