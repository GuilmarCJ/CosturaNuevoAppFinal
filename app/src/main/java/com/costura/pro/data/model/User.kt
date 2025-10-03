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