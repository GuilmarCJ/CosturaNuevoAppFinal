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
import com.costura.pro.data.model.FirebaseAttendance
import com.google.firebase.Timestamp



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
            val date = now.toString(Constants.DATE_FORMAT)
            val time = now.toString(Constants.TIME_FORMAT)
            val yearMonth = now.toString(Constants.YEAR_MONTH_FORMAT)

            Log.d("AttendanceRepository", "🔄 Registrando ENTRADA para $workerName ($workerId) a las $time")

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

                // Guardar en Firebase con NUEVA ESTRUCTURA
                val success = saveAttendanceToFirebase(
                    attendanceId = attendanceId,
                    workerId = workerId,
                    workerName = workerName,
                    date = date,
                    entryTime = time,
                    exitTime = null,
                    status = status,
                    yearMonth = yearMonth
                )

                if (success) {
                    Log.d("AttendanceRepository", "✅ Entrada guardada exitosamente en Firebase")

                    // Actualizar estadísticas del usuario
                    updateUserAttendanceStats(workerId, date)

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
                        isSynced = true,
                        yearMonth = yearMonth
                    )
                    attendanceDao.insertAttendance(attendanceEntity)
                    true
                } else {
                    Log.e("AttendanceRepository", "❌ Error guardando en Firebase")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("AttendanceRepository", "❌ Excepción en registerEntry", e)
            false
        }
    }

    suspend fun registerExit(workerId: String): Boolean {
        return try {
            val today = DateTime().toString(Constants.DATE_FORMAT)
            val currentTime = DateTime().toString(Constants.TIME_FORMAT)

            Log.d("AttendanceRepository", "🔄 Registrando SALIDA para $workerId a las $currentTime")

            val existingRecord = getAttendanceByWorkerAndDate(workerId, today)

            if (existingRecord != null && existingRecord.exitTime == null) {
                Log.d("AttendanceRepository", "📝 Actualizando salida para registro existente")

                // Actualizar en Firebase con NUEVA ESTRUCTURA
                val success = updateExitTimeInFirebase(workerId, existingRecord.id, currentTime)

                if (success) {
                    // Actualizar localmente
                    attendanceDao.updateExitTime(existingRecord.id, currentTime)
                    Log.d("AttendanceRepository", "✅ Salida registrada exitosamente")
                    true
                } else {
                    Log.e("AttendanceRepository", "❌ Error actualizando salida en Firebase")
                    false
                }
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
        status: AttendanceStatus,
        yearMonth: String
    ): Boolean {
        return try {
            val firebaseAttendance = FirebaseAttendance(
                id = attendanceId,
                date = date,
                entryTime = entryTime,
                exitTime = exitTime,
                status = status.name,
                yearMonth = yearMonth
            )

            // NUEVA ESTRUCTURA: Guardar en subcolección del usuario
            db.collection(Constants.COLLECTION_USERS)
                .document(workerId)
                .collection(Constants.SUBCOLLECTION_ATTENDANCE)
                .document(attendanceId)
                .set(firebaseAttendance)
                .await()

            Log.d("AttendanceRepository", "🔥 Registro guardado en subcolección: $attendanceId")
            true
        } catch (e: Exception) {
            Log.e("AttendanceRepository", "❌ Error guardando en Firebase: ${e.message}")
            false
        }
    }

    private suspend fun updateExitTimeInFirebase(workerId: String, attendanceId: String, exitTime: String): Boolean {
        return try {
            val updates = hashMapOf<String, Any>(
                "exitTime" to exitTime
            )

            // NUEVA ESTRUCTURA: Actualizar en subcolección del usuario
            db.collection(Constants.COLLECTION_USERS)
                .document(workerId)
                .collection(Constants.SUBCOLLECTION_ATTENDANCE)
                .document(attendanceId)
                .update(updates)
                .await()

            Log.d("AttendanceRepository", "🔥 Salida actualizada en subcolección: $attendanceId -> $exitTime")
            true
        } catch (e: Exception) {
            Log.e("AttendanceRepository", "❌ Error actualizando salida en Firebase: ${e.message}")
            false
        }
    }

    private suspend fun updateUserAttendanceStats(workerId: String, date: String) {
        try {
            val updates = hashMapOf<String, Any>(
                "stats.lastAttendanceDate" to date,
                "stats.workedDays" to com.google.firebase.firestore.FieldValue.increment(1),
                "timestamps.lastActive" to Timestamp.now()
            )

            db.collection(Constants.COLLECTION_USERS)
                .document(workerId)
                .update(updates)
                .await()
        } catch (e: Exception) {
            Log.e("AttendanceRepository", "Error actualizando stats de usuario: ${e.message}")
        }
    }

    // Métodos existentes actualizados para nueva estructura
    suspend fun getAttendanceByWorkerAndDate(workerId: String, date: String): AttendanceRecord? {
        return try {
            val documents = db.collection(Constants.COLLECTION_USERS)
                .document(workerId)
                .collection(Constants.SUBCOLLECTION_ATTENDANCE)
                .whereEqualTo("date", date)
                .get()
                .await()

            if (documents.isEmpty) {
                null
            } else {
                val document = documents.documents[0]
                val firebaseAttendance = document.toObject(FirebaseAttendance::class.java)

                firebaseAttendance?.let {
                    AttendanceRecord(
                        id = it.id,
                        workerId = workerId,
                        workerName = "", // Necesitaríamos obtener el nombre del usuario
                        date = it.date,
                        entryTime = it.entryTime,
                        exitTime = it.exitTime,
                        status = AttendanceStatus.valueOf(it.status),
                        createdAt = Date() // Podríamos agregar timestamp a FirebaseAttendance
                    )
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getAttendanceHistory(workerId: String, endDate: DateTime): List<AttendanceRecord> {
        return try {
            val startDate = endDate.minusDays(30)
            val startDateStr = startDate.toString(Constants.DATE_FORMAT)
            val endDateStr = endDate.toString(Constants.DATE_FORMAT)

            val documents = db.collection(Constants.COLLECTION_USERS)
                .document(workerId)
                .collection(Constants.SUBCOLLECTION_ATTENDANCE)
                .whereGreaterThanOrEqualTo("date", startDateStr)
                .whereLessThanOrEqualTo("date", endDateStr)
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            documents.map { document ->
                val firebaseAttendance = document.toObject(FirebaseAttendance::class.java)
                AttendanceRecord(
                    id = firebaseAttendance.id,
                    workerId = workerId,
                    workerName = "", // Necesitaríamos obtener el nombre
                    date = firebaseAttendance.date,
                    entryTime = firebaseAttendance.entryTime,
                    exitTime = firebaseAttendance.exitTime,
                    status = AttendanceStatus.valueOf(firebaseAttendance.status),
                    createdAt = Date()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Mantener métodos de QR
    fun generateQRCodeData(type: QRType): String {
        val qrData = QRManager.generatePermanentQR(type)
        return qrData.toJsonString()
    }

    fun parseQRCodeData(qrContent: String): QRCodeData? {
        return QRCodeData.fromJsonString(qrContent)
    }

    data class AttendanceStatistics(
        val workedDays: Int,
        val onTimeDays: Int,
        val lateDays: Int,
        val absentDays: Int
    )

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

    // Sincronización actualizada
    private suspend fun syncToFirebase(attendance: AttendanceEntity) {
        try {
            val firebaseAttendance = FirebaseAttendance(
                id = attendance.id,
                date = attendance.date,
                entryTime = attendance.entryTime,
                exitTime = attendance.exitTime,
                status = attendance.status,
                yearMonth = attendance.yearMonth
            )

            db.collection(Constants.COLLECTION_USERS)
                .document(attendance.workerId)
                .collection(Constants.SUBCOLLECTION_ATTENDANCE)
                .document(attendance.id)
                .set(firebaseAttendance)
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
}