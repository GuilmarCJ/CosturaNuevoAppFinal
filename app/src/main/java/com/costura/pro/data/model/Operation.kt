package com.costura.pro.data.model

import java.util.Date

data class Operation(
    val id: String = "",
    val name: String = "",
    val paymentPerUnit: Double = 0.0,
    val createdAt: Date = Date(),
    val isActive: Boolean = true
)

data class ProductionRecord(
    val id: String = "",
    val workerId: String = "",
    val operationId: String = "",
    val quantity: Int = 0,
    val totalPayment: Double = 0.0,
    val date: Date = Date()
)