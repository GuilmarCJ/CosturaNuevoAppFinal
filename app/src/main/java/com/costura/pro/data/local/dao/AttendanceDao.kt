package com.costura.pro.data.local.dao


import androidx.room.*
import com.costura.pro.data.local.entity.AttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    @Query("SELECT * FROM attendance_records WHERE workerId = :workerId ORDER BY date DESC, entryTime DESC")
    fun getAttendanceByWorker(workerId: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance_records WHERE workerId = :workerId AND date = :date")
    suspend fun getAttendanceByWorkerAndDate(workerId: String, date: String): AttendanceEntity?

    @Query("SELECT * FROM attendance_records WHERE date = :date ORDER BY entryTime DESC")
    fun getAttendanceByDate(date: String): Flow<List<AttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity)

    @Query("UPDATE attendance_records SET exitTime = :exitTime WHERE id = :attendanceId")
    suspend fun updateExitTime(attendanceId: String, exitTime: String)

    @Query("SELECT * FROM attendance_records WHERE isSynced = 0")
    suspend fun getUnsyncedAttendance(): List<AttendanceEntity>

    @Query("UPDATE attendance_records SET isSynced = 1 WHERE id = :attendanceId")
    suspend fun markAsSynced(attendanceId: String)

    @Query("DELETE FROM attendance_records WHERE date < :timestamp")
    suspend fun deleteOldRecords(timestamp: String)
}