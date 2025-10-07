package com.costura.pro.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.costura.pro.data.local.dao.AttendanceDao
import com.costura.pro.data.local.dao.OperationDao
import com.costura.pro.data.local.dao.ProductionDao
import com.costura.pro.data.local.dao.UserDao
import com.costura.pro.data.local.entity.AttendanceEntity
import com.costura.pro.data.local.entity.OperationEntity
import com.costura.pro.data.local.entity.ProductionEntity
import com.costura.pro.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        OperationEntity::class,
        ProductionEntity::class,
        AttendanceEntity::class  // AÑADIR ESTA LÍNEA
    ],
    version = 2,  // INCREMENTAR LA VERSIÓN
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun operationDao(): OperationDao
    abstract fun productionDao(): ProductionDao
    abstract fun attendanceDao(): AttendanceDao  // AÑADIR ESTE MÉTODO

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "costura_pro_database"
                )
                    .fallbackToDestructiveMigration()  // AÑADIR ESTO PARA LA MIGRACIÓN
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}