package com.costura.pro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "machines")
data class MachineEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val machineNumber: String,
    val type: String,
    val description: String,
    val status: MachineStatus = MachineStatus.OPERATIONAL,
    val createdAt: Long = System.currentTimeMillis(),
    val lastMaintenance: Long? = null
)

enum class MachineStatus {
    OPERATIONAL,      // Operativa
    MAINTENANCE,      // En mantenimiento
    BROKEN           // Averiada
}