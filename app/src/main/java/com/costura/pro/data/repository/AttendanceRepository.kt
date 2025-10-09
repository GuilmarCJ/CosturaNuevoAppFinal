package com.costura.pro.data.repository

import android.util.Log
import com.costura.pro.data.local.dao.AttendanceDao
import com.costura.pro.data.local.entity.AttendanceEntity
import com.costura.pro.data.model.AttendanceRecord
import com.costura.pro.data.model.AttendanceStatus
import com.costura.pro.data.model.QRCodeData
import com.costura.pro.data.model.QRType
import com.costura.pro.utils.Constants
import com.costura.pro.utils.QRManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.joda.time.DateTime
import org.joda.time.LocalTime
import java.util.*

class AttendanceRepository(
    private val attendanceDao: AttendanceDao
) {

    private val db = FirebaseFirestore.getInstance()

    fun getAttendanceByWorker(workerId: String): Flow<List<AttendanceRecord>> {
        return attendanceDao.getAttendanceByWorker(workerId).map { entities ->
            entities.map { entity ->
                AttendanceRecord(
                    id = entity.id,
                    workerId = entity.workerId,
                    workerName = entity.workerName,
                    date = entity.date,
                    entryTime = entity.entryTime,
                    exitTime = entity.exitTime,
                    status = AttendanceStatus.valueOf(entity.status),
                    createdAt = Date(entity.createdAt)
                )
            }
        }
    }

    suspend fun registerEntry(workerId: String, workerName: String): Boolean {
        return try {
            val now = DateTime()
            val date = now.toString("yyyy-MM-dd")
            val time = now.toString("HH:mm")

            Log.d("AttendanceRepository", "🔄 Registrando entrada para $workerName ($workerId) a las $time")

            // Verificar si ya existe un registro para hoy
            val existingRecord = getAttendanceByWorkerAndDate(workerId, date)

            if (existingRecord != null) {
                Log.w("AttendanceRepository", "⚠️ Ya existe registro de entrada para hoy")
                false
            } else {
                // Determinar status (presente o tarde)
                val workStart = LocalTime.parse(Constants.WORK_START_TIME)
                val entryTime = LocalTime.parse(time)
                val status = if (entryTime.isAfter(workStart.plusMinutes(Constants.LATE_THRESHOLD_MINUTES))) {
                    AttendanceStatus.LATE
                } else {
                    AttendanceStatus.PRESENT
                }

                val attendanceId = UUID.randomUUID().toString()

                Log.d("AttendanceRepository", "📝 Creando registro con ID: $attendanceId, Status: $status")

                // Guardar en Firebase directamente
                val success = saveAttendanceToFirebase(
                    attendanceId = attendanceId,
                    workerId = workerId,
                    workerName = workerName,
                    date = date,
                    entryTime = time,
                    exitTime = null,
                    status = status
                )

                if (success) {
                    Log.d("AttendanceRepository", "✅ Entrada guardada exitosamente en Firebase")

                    // También guardar localmente para cache
                    val attendanceEntity = AttendanceEntity(
                        id = attendanceId,
                        workerId = workerId,
                        workerName = workerName,
                        date = date,
                        entryTime = time,
                        exitTime = null,
                        status = status.name,
                        createdAt = System.currentTimeMillis(),
                        isSynced = true // Ya está sincronizado
                    )
                    attendanceDao.insertAttendance(attendanceEntity)
                } else {
                    Log.e("AttendanceRepository", "❌ Error guardando en Firebase")
                }

                success
            }
        } catch (e: Exception) {
            Log.e("AttendanceRepository", "❌ Excepción en registerEntry", e)
            false
        }
    }


    suspend fun registerExit(workerId: String): Boolean {
        return try {
            val today = DateTime().toString("yyyy-MM-dd")
            val currentTime = DateTime().toString("HH:mm")

            Log.d("AttendanceRepository", "🔄 Registrando salida para $workerId a las $currentTime")

            val existingRecord = getAttendanceByWorkerAndDate(workerId, today)

            if (existingRecord != null && existingRecord.exitTime == null) {
                Log.d("AttendanceRepository", "📝 Actualizando salida para registro existente")

                // Actualizar en Firebase
                val success = updateExitTimeInFirebase(existingRecord.id, currentTime)

                if (success) {
                    // Actualizar localmente
                    attendanceDao.updateExitTime(existingRecord.id, currentTime)
                    Log.d("AttendanceRepository", "✅ Salida registrada exitosamente")
                } else {
                    Log.e("AttendanceRepository", "❌ Error actualizando salida en Firebase")
                }

                success
            } else {
                Log.w("AttendanceRepository", "⚠️ No se encontró registro de entrada para hoy o ya tiene salida")
                false
            }
        } catch (e: Exception) {
            Log.e("AttendanceRepository", "❌ Excepción en registerExit", e)
            false
        }
    }


    private suspend fun saveAttendanceToFirebase(
        attendanceId: String,
        workerId: String,
        workerName: String,
        date: String,
        entryTime: String,
        exitTime: String?,
        status: AttendanceStatus
    ): Boolean {
        return try {
            val attendanceData = hashMapOf(
                "workerId" to workerId,
                "workerName" to workerName,
                "date" to date,
                "entryTime" to entryTime,
                "exitTime" to exitTime,
                "status" to status.name,
                "createdAt" to com.google.firebase.Timestamp.now(),
                "updatedAt" to com.google.firebase.Timestamp.now()
            )

            db.collection(Constants.COLLECTION_ATTENDANCE)
                .document(attendanceId)
                .set(attendanceData)
                .await()

            Log.d("AttendanceRepository", "🔥 Registro guardado en Firebase: $attendanceId")
            true
        } catch (e: Exception) {
            Log.e("AttendanceRepository", "❌ Error guardando en Firebase: ${e.message}")
            false
        }
    }

    private suspend fun updateExitTimeInFirebase(attendanceId: String, exitTime: String): Boolean {
        return try {
            val updates = hashMapOf<String, Any>(
                "exitTime" to exitTime,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )

            db.collection(Constants.COLLECTION_ATTENDANCE)
                .document(attendanceId)
                .update(updates)
                .await()

            Log.d("AttendanceRepository", "🔥 Salida actualizada en Firebase: $attendanceId -> $exitTime")
            true
        } catch (e: Exception) {
            Log.e("AttendanceRepository", "❌ Error actualizando salida en Firebase: ${e.message}")
            false
        }
    }

    private suspend fun syncToFirebase(attendance: AttendanceEntity) {
        try {
            val attendanceData = hashMapOf(
                "workerId" to attendance.workerId,
                "workerName" to attendance.workerName,
                "date" to attendance.date,
                "entryTime" to attendance.entryTime,
                "exitTime" to attendance.exitTime,
                "status" to attendance.status,
                "createdAt" to com.google.firebase.Timestamp.now()
            )

            db.collection(Constants.COLLECTION_ATTENDANCE)
                .document(attendance.id)
                .set(attendanceData)
                .await()

            attendanceDao.markAsSynced(attendance.id)
        } catch (e: Exception) {
            // Error en sincronización, se intentará más tarde
        }
    }

    suspend fun syncUnsyncedAttendance() {
        try {
            val unsyncedRecords = attendanceDao.getUnsyncedAttendance()

            for (record in unsyncedRecords) {
                syncToFirebase(record)
            }
        } catch (e: Exception) {
            // Handle sync error
        }
    }

    fun generateQRCodeData(type: QRType): String {
        val qrData = QRManager.generatePermanentQR(type)
        return qrData.toJsonString()
    }

    fun parseQRCodeData(qrContent: String): com.costura.pro.data.model.QRCodeData? {
        return com.costura.pro.data.model.QRCodeData.fromJsonString(qrContent)
    }

    private fun handleQRRegistration(qrData: QRCodeData): Boolean {
        return if (QRManager.isQRValid(qrData)) {
            // Marcar QR como usado
            QRManager.markQRAsUsed(qrData)
            true
        } else {
            false
        }
    }

    // Añadir estos métodos al AttendanceRepository existente

    // Añadir este método al AttendanceRepository
    suspend fun getAttendanceHistory(workerId: String, endDate: DateTime): List<AttendanceRecord> {
        return try {
            val startDate = endDate.minusDays(30) // Últimos 30 días
            val startDateStr = startDate.toString("yyyy-MM-dd")
            val endDateStr = endDate.toString("yyyy-MM-dd")

            val documents = db.collection(Constants.COLLECTION_ATTENDANCE)
                .whereEqualTo("workerId", workerId)
                .whereGreaterThanOrEqualTo("date", startDateStr)
                .whereLessThanOrEqualTo("date", endDateStr)
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            documents.map { document ->
                AttendanceRecord(
                    id = document.id,
                    workerId = document.getString("workerId") ?: "",
                    workerName = document.getString("workerName") ?: "",
                    date = document.getString("date") ?: "",
                    entryTime = document.getString("entryTime") ?: "--:--",
                    exitTime = document.getString("exitTime"),
                    status = AttendanceStatus.valueOf(document.getString("status") ?: "ABSENT"),
                    createdAt = (document.get("createdAt") as? com.google.firebase.Timestamp)?.toDate() ?: Date()
                )
            }
        } catch (e: Exception) {
            emptyList() // Si hay error, retornar lista vacía
        }
    }

    // En AttendanceRepository.kt - Añadir este método
    suspend fun getAttendanceByWorkerAndDate(workerId: String, date: String): AttendanceRecord? {
        return try {
            val documents = db.collection(Constants.COLLECTION_ATTENDANCE)
                .whereEqualTo("workerId", workerId)
                .whereEqualTo("date", date)
                .get()
                .await()

            if (documents.isEmpty) {
                null
            } else {
                val document = documents.documents[0]
                AttendanceRecord(
                    id = document.id,
                    workerId = document.getString("workerId") ?: "",
                    workerName = document.getString("workerName") ?: "",
                    date = document.getString("date") ?: "",
                    entryTime = document.getString("entryTime") ?: "--:--",
                    exitTime = document.getString("exitTime"),
                    status = AttendanceStatus.valueOf(document.getString("status") ?: "ABSENT"),
                    createdAt = (document.get("createdAt") as? com.google.firebase.Timestamp)?.toDate() ?: Date()
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getAttendanceStatistics(workerId: String, month: DateTime): AttendanceStatistics {
        val records = getAttendanceHistory(workerId, month)

        val workedDays = records.count { it.entryTime != "--:--" }
        val onTimeDays = records.count { it.status == AttendanceStatus.PRESENT }
        val lateDays = records.count { it.status == AttendanceStatus.LATE }
        val absentDays = records.count { it.status == AttendanceStatus.ABSENT }

        return AttendanceStatistics(
            workedDays = workedDays,
            onTimeDays = onTimeDays,
            lateDays = lateDays,
            absentDays = absentDays
        )
    }

    data class AttendanceStatistics(
        val workedDays: Int,
        val onTimeDays: Int,
        val lateDays: Int,
        val absentDays: Int
    )
}