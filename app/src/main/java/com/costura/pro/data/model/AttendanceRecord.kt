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
    val type: QRType,
    val locationId: String = "costura_pro",
    val uniqueId: String = "", // ID único para cada QR
    val isPermanent: Boolean = true // Indica si el QR es permanente
) {
    fun toJsonString(): String {
        return """{
            "type":"${type.name}",
            "locationId":"$locationId",
            "uniqueId":"$uniqueId",
            "isPermanent":$isPermanent
        }""".trimIndent()
    }

    // En QRCodeData.kt - MEJORAR EL MÉTODO fromJsonString
    companion object {
        fun fromJsonString(json: String): QRCodeData? {
            return try {
                // Limpiar y normalizar el JSON
                val cleanJson = json.trim()
                    .replace("\\s".toRegex(), "") // Eliminar espacios en blanco
                    .replace("\\n".toRegex(), "") // Eliminar saltos de línea

                Log.d("QRParser", "JSON limpio: $cleanJson")

                // Extraer campos de manera más robusta
                val typeMatch = "\"type\":\"([^\"]+)\"".toRegex().find(cleanJson)
                val locationIdMatch = "\"locationId\":\"([^\"]+)\"".toRegex().find(cleanJson)
                val uniqueIdMatch = "\"uniqueId\":\"([^\"]+)\"".toRegex().find(cleanJson)
                val isPermanentMatch = "\"isPermanent\":(true|false)".toRegex().find(cleanJson)

                if (typeMatch == null || locationIdMatch == null || uniqueIdMatch == null || isPermanentMatch == null) {
                    Log.e("QRParser", "Faltan campos en el JSON: $cleanJson")
                    return null
                }

                val type = typeMatch.groupValues[1]
                val locationId = locationIdMatch.groupValues[1]
                val uniqueId = uniqueIdMatch.groupValues[1]
                val isPermanent = isPermanentMatch.groupValues[1].toBoolean()

                Log.d("QRParser", "Campos extraídos: type=$type, locationId=$locationId, uniqueId=$uniqueId, isPermanent=$isPermanent")

                QRCodeData(
                    type = QRType.valueOf(type),
                    locationId = locationId,
                    uniqueId = uniqueId,
                    isPermanent = isPermanent
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

