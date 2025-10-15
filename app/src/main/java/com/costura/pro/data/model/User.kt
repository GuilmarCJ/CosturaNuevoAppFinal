package com.costura.pro.data.model

import java.util.Date

data class User(
    val id: String = "",
    val username: String = "",
    val password: String = "",
    val role: UserRole = UserRole.WORKER,
    val createdAt: Date = Date(),
    val isActive: Boolean = true
)

enum class UserRole {
    ADMIN, WORKER
}

//Usuario
data class FirebaseUser(
    val id: String = "",
    val basicInfo: UserBasicInfo = UserBasicInfo(),
    val stats: UserStats = UserStats(),
    val timestamps: UserTimestamps = UserTimestamps()
)

data class UserBasicInfo(
    val name: String = "",
    val username: String = "",
    val password: String = "",
    val role: String = "WORKER",
    val modality: String = "PIECE_RATE"
)

data class UserStats(
    val totalEarnings: Double = 0.0,
    val lastAttendanceDate: String? = null,
    val monthlyProduction: Int = 0,
    val workedDays: Int = 0
)

data class UserTimestamps(
    val createdAt: Date = Date(),
    val lastActive: Date = Date(),
    val lastSync: Date = Date()
)

// Modelo para asistencia en subcolección
data class FirebaseAttendance(
    val id: String = "",
    val date: String = "",
    val entryTime: String = "",
    val exitTime: String? = null,
    val status: String = "PRESENT",
    val yearMonth: String = "" // Para agrupamiento
)

// Modelo para producción en subcolección
data class FirebaseProduction(
    val id: String = "",
    val operationId: String = "",
    val operationName: String = "",
    val quantity: Int = 0,
    val paymentPerUnit: Double = 0.0,
    val totalPayment: Double = 0.0,
    val date: Date = Date(),
    val yearMonth: String = "" // Para agrupamiento
)