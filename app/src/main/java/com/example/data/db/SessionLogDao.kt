package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SessionLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionLogDao {
    @Query("SELECT * FROM session_logs ORDER BY id DESC")
    fun getAllLogsFlow(): Flow<List<SessionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: SessionLogEntity): Long

    @Query("DELETE FROM session_logs")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM session_logs")
    suspend fun count(): Int
}
