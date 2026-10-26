package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PromptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PromptDao {
    @Query("SELECT * FROM prompts ORDER BY orderIndex ASC, id ASC")
    fun getAllPromptsFlow(): Flow<List<PromptEntity>>

    @Query("SELECT * FROM prompts WHERE isEnabled = 1 ORDER BY orderIndex ASC, id ASC")
    suspend fun getEnabledPrompts(): List<PromptEntity>

    @Query("SELECT * FROM prompts WHERE id = :id")
    suspend fun getPromptById(id: Long): PromptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(prompt: PromptEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(prompts: List<PromptEntity>)

    @Update
    suspend fun update(prompt: PromptEntity)

    @Delete
    suspend fun delete(prompt: PromptEntity)

    @Query("UPDATE prompts SET timesUsed = timesUsed + 1 WHERE id = :id")
    suspend fun incrementTimesUsed(id: Long)

    @Query("SELECT COUNT(*) FROM prompts")
    suspend fun count(): Int

    @Query("DELETE FROM prompts")
    suspend fun clearAll()
}
