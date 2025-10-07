package com.costura.pro.data.repository

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

            // Verificar si ya existe un registro para hoy
            val existingRecord = attendanceDao.getAttendanceByWorkerAndDate(workerId, date)

            if (existingRecord != null) {
                // Ya registró entrada hoy
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

                // Guardar localmente
                val attendanceEntity = AttendanceEntity(
                    id = attendanceId,
                    workerId = workerId,
                    workerName = workerName,
                    date = date,
                    entryTime = time,
                    exitTime = null,
                    status = status.name,
                    createdAt = System.currentTimeMillis(),
                    isSynced = false
                )

                attendanceDao.insertAttendance(attendanceEntity)

                // Sincronizar con Firebase
                syncToFirebase(attendanceEntity)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun registerExit(workerId: String): Boolean {
        return try {
            val today = DateTime().toString("yyyy-MM-dd")
            val currentTime = DateTime().toString("HH:mm")

            val existingRecord = attendanceDao.getAttendanceByWorkerAndDate(workerId, today)

            if (existingRecord != null && existingRecord.exitTime == null) {
                // Actualizar salida
                attendanceDao.updateExitTime(existingRecord.id, currentTime)

                // Sincronizar con Firebase
                val updatedEntity = existingRecord.copy(exitTime = currentTime)
                syncToFirebase(updatedEntity)
                true
            } else {
                false
            }
        } catch (e: Exception) {
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

    suspend fun getAttendanceHistory(workerId: String, month: DateTime): List<AttendanceRecord> {
        return try {
            val startOfMonth = month.withDayOfMonth(1).toString("yyyy-MM-dd")
            val endOfMonth = month.dayOfMonth().withMaximumValue().toString("yyyy-MM-dd")

            val documents = db.collection(Constants.COLLECTION_ATTENDANCE)
                .whereEqualTo("workerId", workerId)
                .whereGreaterThanOrEqualTo("date", startOfMonth)
                .whereLessThanOrEqualTo("date", endOfMonth)
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
            }.sortedByDescending { it.date }
        } catch (e: Exception) {
            emptyList()
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