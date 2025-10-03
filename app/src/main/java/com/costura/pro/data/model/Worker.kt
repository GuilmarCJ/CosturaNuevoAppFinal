package com.costura.pro.data.model

import java.util.Date

data class Worker(
    val id: String = "",
    val name: String = "",
    val username: String = "",
    val password: String = "",
    val modality: WorkModality = WorkModality.PIECE_RATE,
    val createdAt: Date = Date(),
    val isActive: Boolean = true
)

enum class WorkModality {
    DAILY_RATE,  // Jornal
    PIECE_RATE   // Destajo
}