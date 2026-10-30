package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ConversationSessionEntity
import com.example.data.model.ConversationTurnEntity
import com.example.data.model.ManagedAccountEntity
import com.example.data.model.PromptEntity
import com.example.data.model.SessionLogEntity
import kotlinx.coroutines.CoroutineScope

/** Keep schema version 5 so existing on-device records are not erased during this update. */
@Database(
    entities = [
        PromptEntity::class,
        SessionLogEntity::class,
        ConversationSessionEntity::class,
        ConversationTurnEntity::class,
        ManagedAccountEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun promptDao(): PromptDao
    abstract fun sessionLogDao(): SessionLogDao
    abstract fun conversationDao(): ConversationDao
    abstract fun accountDao(): AccountDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        @Suppress("UNUSED_PARAMETER")
        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "arena_pilot.db"
                )
                    // Never discard local history or legacy account rows silently on schema mismatch.
                    .build()
                    .also { INSTANCE = it }
            }

        // The v1.0.0 seed data included real-looking email/password literals and fake battle
        // records. Do not reintroduce these: retain existing user data but create no new samples.
        @Suppress("UNUSED_PARAMETER")
        suspend fun populateInitialAccounts(dao: AccountDao) = Unit

        @Suppress("UNUSED_PARAMETER")
        suspend fun populateInitialConversations(dao: ConversationDao) = Unit

        @Suppress("UNUSED_PARAMETER")
        suspend fun populateInitialPrompts(dao: PromptDao) = Unit
    }
}
