package com.costura.pro

import android.app.Application
import com.costura.pro.data.local.database.AppDatabase
import com.costura.pro.data.repository.AttendanceRepository
import com.costura.pro.data.repository.OperationRepository
import com.costura.pro.data.repository.ProductionRepository
import com.costura.pro.data.repository.UserRepository
import com.costura.pro.utils.SecurityUtils
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class CosturaProApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val userRepository: UserRepository by lazy { UserRepository(database.userDao()) }
    val operationRepository: OperationRepository by lazy { OperationRepository(database.operationDao()) }
    val productionRepository: ProductionRepository by lazy { ProductionRepository(database.productionDao()) }
    val attendanceRepository: AttendanceRepository by lazy { AttendanceRepository(database.attendanceDao()) }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // INICIALIZAR FIREBASE PRIMERO
        initializeFirebase()

        // Initialize security checks
        initializeSecurity()

    }

    private fun initializeFirebase() {
        try {
            // Inicializar Firebase
            FirebaseApp.initializeApp(this)

            // Configurar Firestore para desarrollo (opcional)
            val db = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true) // Habilitar cache offline
                .build()
            db.firestoreSettings = settings

        } catch (e: Exception) {
            // Firebase ya está inicializado o hay un error
            e.printStackTrace()
        }
    }

    private fun initializeSecurity() {
        // Validate app signature (optional but recommended for security)
        if (!SecurityUtils.validateAppSignature(this)) {
            // Handle invalid signature (app might be tampered)
            // You might want to restrict functionality or show warning
        }
    }

    companion object {
        lateinit var instance: CosturaProApp
            private set
    }
}