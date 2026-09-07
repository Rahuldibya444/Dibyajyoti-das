package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parties")
data class PartyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "local_user",
    val title: String,
    val description: String = "",
    val currencySymbol: String = "₹",
    val dateMillis: Long = System.currentTimeMillis(),
    val isSettled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSyncedAt: Long = System.currentTimeMillis()
)
