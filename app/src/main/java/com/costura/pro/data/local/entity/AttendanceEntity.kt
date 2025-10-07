package com.costura.pro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_records")
data class AttendanceEntity(
    @PrimaryKey
    val id: String,
    val workerId: String,
    val workerName: String,
    val date: String,
    val entryTime: String,
    val exitTime: String?,
    val status: String,
    val createdAt: Long,
    val isSynced: Boolean = false
)