package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.PartyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PartyDao {
    @Query("SELECT * FROM parties WHERE userId = :userId ORDER BY createdAt DESC")
    fun getPartiesByUser(userId: String): Flow<List<PartyEntity>>

    @Query("SELECT * FROM parties ORDER BY createdAt DESC")
    fun getAllParties(): Flow<List<PartyEntity>>

    @Query("SELECT * FROM parties ORDER BY createdAt DESC")
    suspend fun getAllPartiesSync(): List<PartyEntity>

    @Query("SELECT * FROM parties WHERE id = :id")
    fun getPartyById(id: Long): Flow<PartyEntity?>

    @Query("SELECT * FROM parties WHERE id = :id")
    suspend fun getPartyByIdSync(id: Long): PartyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParty(party: PartyEntity): Long

    @Update
    suspend fun updateParty(party: PartyEntity)

    @Delete
    suspend fun deleteParty(party: PartyEntity)

    @Query("DELETE FROM parties WHERE id = :id")
    suspend fun deletePartyById(id: Long)

    @Query("UPDATE parties SET lastSyncedAt = :timestamp WHERE id = :id")
    suspend fun updateSyncTimestamp(id: Long, timestamp: Long)

    @Query("UPDATE parties SET currencySymbol = '₹' WHERE currencySymbol = '$' OR currencySymbol = ''")
    suspend fun updateDollarToRupee()

    @Query("DELETE FROM parties WHERE userId LIKE '%google%' OR title = 'Summer Rooftop Party'")
    suspend fun cleanLegacyParties()

    @Query("DELETE FROM parties")
    suspend fun deleteAllParties()
}
