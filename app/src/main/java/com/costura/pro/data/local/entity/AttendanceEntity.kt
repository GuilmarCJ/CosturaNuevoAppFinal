package com.costura.pro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "attendance_records")
data class AttendanceEntity(
    @PrimaryKey
    val id: String,
    val workerId: String,
    val workerName: String,
    val date: String, // Formato: YYYY-MM-DD
    val entryTime: String,
    val exitTime: String?,
    val status: String,
    val createdAt: Long,
    val isSynced: Boolean = false,
    // Nuevo campo para organización mensual
    val yearMonth: String // Formato: YYYY-MM para agrupamiento
)