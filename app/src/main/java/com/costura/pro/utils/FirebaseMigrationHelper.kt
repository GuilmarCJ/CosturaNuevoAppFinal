package com.costura.pro.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.joda.time.DateTime
import java.util.*

object FirebaseMigrationHelper {
    private const val TAG = "FirebaseMigration"
    private val db = FirebaseFirestore.getInstance()

    // Método para migrar datos existentes a la nueva estructura
    suspend fun migrateExistingData() {
        Log.d(TAG, "🔄 Iniciando migración de datos a nueva estructura...")

        try {
            // Migrar usuarios
            migrateUsers()

            // Migrar asistencia
            migrateAttendance()

            // Migrar producción
            migrateProduction()

            Log.d(TAG, "✅ Migración completada exitosamente")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en migración: ${e.message}", e)
        }
    }

    private suspend fun migrateUsers() {
        Log.d(TAG, "Migrando usuarios...")

        try {
            val oldUsers = db.collection("old_users_backup").get().await()

            for (document in oldUsers) {
                val oldUser = document.data
                val userId = document.id

                val newUserData = hashMapOf(
                    "basicInfo" to hashMapOf(
                        "name" to (oldUser["name"] ?: ""),
                        "username" to (oldUser["username"] ?: ""),
                        "password" to (oldUser["password"] ?: ""),
                        "role" to (oldUser["role"] ?: "WORKER"),
                        "modality" to (oldUser["modality"] ?: "PIECE_RATE")
                    ),
                    "stats" to hashMapOf(
                        "totalEarnings" to 0.0,
                        "monthlyProduction" to 0,
                        "workedDays" to 0,
                        "lastAttendanceDate" to null
                    ),
                    "timestamps" to hashMapOf(
                        "createdAt" to (oldUser["createdAt"] ?: Timestamp.now()),
                        "lastActive" to Timestamp.now()
                    )
                )

                // Guardar en nueva estructura
                db.collection(Constants.COLLECTION_USERS)
                    .document(userId)
                    .set(newUserData)
                    .await()

                Log.d(TAG, "Usuario migrado: ${oldUser["name"]}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error migrando usuarios: ${e.message}")
        }
    }

    private suspend fun migrateAttendance() {
        Log.d(TAG, "Migrando asistencia...")

        try {
            val oldAttendance = db.collection("old_attendance_backup").get().await()

            for (document in oldAttendance) {
                val oldRecord = document.data
                val workerId = oldRecord["workerId"] as? String ?: continue
                val date = oldRecord["date"] as? String ?: continue
                val yearMonth = DateUtils.formatToYearMonth(date)

                val newAttendanceData = hashMapOf(
                    "id" to document.id,
                    "date" to date,
                    "entryTime" to (oldRecord["entryTime"] ?: ""),
                    "exitTime" to (oldRecord["exitTime"]),
                    "status" to (oldRecord["status"] ?: "PRESENT"),
                    "yearMonth" to yearMonth
                )

                // Guardar en subcolección del usuario
                db.collection(Constants.COLLECTION_USERS)
                    .document(workerId)
                    .collection(Constants.SUBCOLLECTION_ATTENDANCE)
                    .document(document.id)
                    .set(newAttendanceData)
                    .await()
            }

            Log.d(TAG, "Asistencia migrada: ${oldAttendance.size()} registros")
        } catch (e: Exception) {
            Log.e(TAG, "Error migrando asistencia: ${e.message}")
        }
    }

    private suspend fun migrateProduction() {
        Log.d(TAG, "Migrando producción...")

        try {
            val oldProduction = db.collection("old_production_backup").get().await()

            for (document in oldProduction) {
                val oldRecord = document.data
                val workerId = oldRecord["workerId"] as? String ?: continue
                val date = oldRecord["date"] as? Timestamp ?: continue
                val yearMonth = DateTime(date.toDate()).toString(Constants.YEAR_MONTH_FORMAT)

                val newProductionData = hashMapOf(
                    "id" to document.id,
                    "operationId" to (oldRecord["operationId"] ?: ""),
                    "operationName" to (oldRecord["operationName"] ?: ""),
                    "quantity" to (oldRecord["quantity"] ?: 0),
                    "paymentPerUnit" to (oldRecord["paymentPerUnit"] ?: 0.0),
                    "totalPayment" to (oldRecord["totalPayment"] ?: 0.0),
                    "date" to date,
                    "yearMonth" to yearMonth
                )

                // Guardar en subcolección del usuario
                db.collection(Constants.COLLECTION_USERS)
                    .document(workerId)
                    .collection(Constants.SUBCOLLECTION_PRODUCTION)
                    .document(document.id)
                    .set(newProductionData)
                    .await()
            }

            Log.d(TAG, "Producción migrada: ${oldProduction.size()} registros")
        } catch (e: Exception) {
            Log.e(TAG, "Error migrando producción: ${e.message}")
        }
    }

    // Método para crear backups antes de migrar
    suspend fun createBackups() {
        Log.d(TAG, "🔄 Creando backups de datos existentes...")

        // Backup de usuarios
        createCollectionBackup("users", "old_users_backup")

        // Backup de asistencia
        createCollectionBackup("attendance", "old_attendance_backup")

        // Backup de producción
        createCollectionBackup("production", "old_production_backup")

        Log.d(TAG, "✅ Backups creados exitosamente")
    }

    private suspend fun createCollectionBackup(sourceCollection: String, backupCollection: String) {
        try {
            val documents = db.collection(sourceCollection).get().await()

            for (document in documents) {
                db.collection(backupCollection)
                    .document(document.id)
                    .set(document.data)
                    .await()
            }

            Log.d(TAG, "Backup creado: $sourceCollection -> $backupCollection (${documents.size()} documentos)")
        } catch (e: Exception) {
            Log.e(TAG, "Error creando backup de $sourceCollection: ${e.message}")
        }
    }
}