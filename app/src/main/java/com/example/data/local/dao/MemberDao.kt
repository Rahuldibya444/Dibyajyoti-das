package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.MemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {
    @Query("SELECT * FROM members WHERE partyId = :partyId ORDER BY name ASC")
    fun getMembersByParty(partyId: Long): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE partyId = :partyId ORDER BY name ASC")
    suspend fun getMembersByPartySync(partyId: Long): List<MemberEntity>

    @Query("SELECT * FROM members WHERE id = :id")
    suspend fun getMemberById(id: Long): MemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: MemberEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<MemberEntity>): List<Long>

    @Update
    suspend fun updateMember(member: MemberEntity)

    @Delete
    suspend fun deleteMember(member: MemberEntity)

    @Query("DELETE FROM members WHERE id = :id")
    suspend fun deleteMemberById(id: Long)

    @Query("DELETE FROM members WHERE emailOrPhone LIKE '%gmail%' OR emailOrPhone LIKE '%srisri%' OR name = 'Dibyajyoti'")
    suspend fun cleanLegacyMembers()

    @Query("DELETE FROM members")
    suspend fun deleteAllMembers()
}
