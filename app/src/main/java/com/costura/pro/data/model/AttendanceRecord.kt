package com.costura.pro.data.model

import android.util.Log
import java.util.*

data class AttendanceRecord(
    val id: String = "",
    val workerId: String = "",
    val workerName: String = "",
    val date: String = "", // Formato: YYYY-MM-DD
    val entryTime: String = "", // Formato: HH:mm
    val exitTime: String? = null,
    val status: AttendanceStatus = AttendanceStatus.PRESENT,
    val createdAt: Date = Date()
)

enum class AttendanceStatus {
    PRESENT, LATE, ABSENT, HALF_DAY
}



data class QRCodeData(
    val locationId: String = "costura_pro"
) {
    fun toJsonString(): String {
        return """{
            "locationId":"$locationId"
        }""".trimIndent()
    }

    companion object {
        fun fromJsonString(json: String): QRCodeData? {
            return try {
                val cleanJson = json.trim()
                    .replace("\\s".toRegex(), "")
                    .replace("\\n".toRegex(), "")

                Log.d("QRParser", "JSON limpio: $cleanJson")

                val locationIdMatch = "\"locationId\":\"([^\"]+)\"".toRegex().find(cleanJson)

                if (locationIdMatch == null) {
                    Log.e("QRParser", "Faltan campos en el JSON: $cleanJson")
                    return null
                }

                val locationId = locationIdMatch.groupValues[1]

                Log.d("QRParser", "Campos extraídos: locationId=$locationId")

                QRCodeData(
                    locationId = locationId
                )
            } catch (e: Exception) {
                Log.e("QRParser", "Error parseando JSON: $json", e)
                null
            }
        }
    }
}

enum class QRType {
    ENTRY, EXIT
}