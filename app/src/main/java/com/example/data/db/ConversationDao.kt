package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.ConversationSessionEntity
import com.example.data.model.ConversationTurnEntity
import com.example.data.model.SessionWithTurns
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllSessionsFlow(): Flow<List<ConversationSessionEntity>>

    @Transaction
    @Query("SELECT * FROM conversations WHERE id = :sessionId")
    fun getSessionWithTurnsFlow(sessionId: Long): Flow<SessionWithTurns?>

    @Transaction
    @Query("SELECT * FROM conversations WHERE id = :sessionId")
    suspend fun getSessionWithTurns(sessionId: Long): SessionWithTurns?

    @Query("SELECT * FROM conversations WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): ConversationSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ConversationSessionEntity): Long

    @Update
    suspend fun updateSession(session: ConversationSessionEntity)

    @Query("DELETE FROM conversations WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTurn(turn: ConversationTurnEntity): Long

    @Query("SELECT * FROM conversation_turns WHERE sessionId = :sessionId ORDER BY turnIndex ASC")
    suspend fun getTurnsForSession(sessionId: Long): List<ConversationTurnEntity>

    @Query("UPDATE conversations SET winnerVote = :vote, updatedAt = :timestamp WHERE id = :sessionId")
    suspend fun updateWinnerVote(sessionId: Long, vote: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE conversations SET title = :newTitle, updatedAt = :timestamp WHERE id = :sessionId")
    suspend fun updateSessionTitle(sessionId: Long, newTitle: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM conversation_turns WHERE sessionId = :sessionId")
    suspend fun deleteTurnsForSession(sessionId: Long)

    @Query("SELECT COUNT(*) FROM conversations")
    suspend fun countSessions(): Int
}
