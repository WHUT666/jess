package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ManagedAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Query("SELECT * FROM managed_accounts ORDER BY id ASC")
    fun getAllAccountsFlow(): Flow<List<ManagedAccountEntity>>

    @Query("SELECT * FROM managed_accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: Long): ManagedAccountEntity?

    @Query("SELECT * FROM managed_accounts WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveAccount(): ManagedAccountEntity?

    @Query("SELECT * FROM managed_accounts WHERE isActive = 1 LIMIT 1")
    fun getActiveAccountFlow(): Flow<ManagedAccountEntity?>

    @Query("SELECT COUNT(*) FROM managed_accounts")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: ManagedAccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<ManagedAccountEntity>)

    @Update
    suspend fun update(account: ManagedAccountEntity)

    @Delete
    suspend fun delete(account: ManagedAccountEntity)

    @Query("SELECT * FROM managed_accounts ORDER BY id ASC")
    suspend fun getAllAccountsList(): List<ManagedAccountEntity>

    @Query("UPDATE managed_accounts SET status = :status, lastCheckedAt = :checkedAt, failureReason = :reason WHERE id = :id")
    suspend fun updateAccountStatus(id: Long, status: String, checkedAt: Long, reason: String)

    @Query("UPDATE managed_accounts SET status = :status, lastCheckedAt = :checkedAt, failureReason = :reason, isBannedOnArena = :isBanned, cookies = CASE WHEN :cookies != '' THEN :cookies ELSE cookies END WHERE id = :id")
    suspend fun updateAccountCheckResult(id: Long, status: String, checkedAt: Long, reason: String, isBanned: Boolean, cookies: String)

    @Query("UPDATE managed_accounts SET isActive = CASE WHEN id = :selectedId THEN 1 ELSE 0 END")
    suspend fun setActiveAccount(selectedId: Long)
}
