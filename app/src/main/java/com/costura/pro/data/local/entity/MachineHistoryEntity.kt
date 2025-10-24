package com.costura.pro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "machine_history")
data class MachineHistoryEntity(
    @PrimaryKey
    val id: String,
    val machineId: String,
    val machineName: String,
    val machineNumber: String,
    val type: HistoryType,
    val description: String,
    val date: Long = System.currentTimeMillis(),
    val solvedBy: String? = null,
    val solution: String? = null
)

enum class HistoryType {
    PROBLEM_REPORTED,  // Problema reportado
    PROBLEM_SOLVED     // Problema solucionado
}