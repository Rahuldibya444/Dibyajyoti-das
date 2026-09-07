package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.MemberEntity
import com.example.data.local.entity.PartyEntity
import com.example.data.model.SyncStatus
import com.example.data.model.UserAccount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PartyRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val partyDao = db.partyDao()
    private val memberDao = db.memberDao()
    private val expenseDao = db.expenseDao()

    private val _currentAccount = MutableStateFlow(
        UserAccount(
            id = "local_device",
            email = "",
            displayName = "Organizer",
            isSignedInWithGoogle = false,
            syncStatus = SyncStatus.OFFLINE_READY,
            lastSyncMillis = System.currentTimeMillis()
        )
    )
    val currentAccount: StateFlow<UserAccount> = _currentAccount.asStateFlow()

    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    init {
        // Automatically migrate currency to Rupee and purge legacy test data
        CoroutineScope(Dispatchers.IO).launch {
            partyDao.updateDollarToRupee()
            partyDao.cleanLegacyParties()
            memberDao.cleanLegacyMembers()
            expenseDao.cleanLegacyExpenses()
        }
    }

    fun getParties(): Flow<List<PartyEntity>> = partyDao.getAllParties()

    fun getPartyById(id: Long): Flow<PartyEntity?> = partyDao.getPartyById(id)

    suspend fun getPartyByIdSync(id: Long): PartyEntity? = withContext(Dispatchers.IO) {
        partyDao.getPartyByIdSync(id)
    }

    fun getMembersByParty(partyId: Long): Flow<List<MemberEntity>> =
        memberDao.getMembersByParty(partyId)

    suspend fun getMembersByPartySync(partyId: Long): List<MemberEntity> =
        withContext(Dispatchers.IO) {
            memberDao.getMembersByPartySync(partyId)
        }

    fun getExpensesByParty(partyId: Long): Flow<List<ExpenseEntity>> =
        expenseDao.getExpensesByParty(partyId)

    suspend fun getExpensesByPartySync(partyId: Long): List<ExpenseEntity> =
        withContext(Dispatchers.IO) {
            expenseDao.getExpensesByPartySync(partyId)
        }

    suspend fun createParty(
        title: String,
        description: String,
        currencySymbol: String,
        dateMillis: Long
    ): Long = withContext(Dispatchers.IO) {
        val party = PartyEntity(
            userId = "local",
            title = title,
            description = description,
            currencySymbol = currencySymbol,
            dateMillis = dateMillis,
            createdAt = System.currentTimeMillis(),
            lastSyncedAt = System.currentTimeMillis()
        )
        partyDao.insertParty(party)
    }

    suspend fun updateParty(party: PartyEntity) = withContext(Dispatchers.IO) {
        partyDao.updateParty(party.copy(lastSyncedAt = System.currentTimeMillis()))
    }

    suspend fun deleteParty(partyId: Long) = withContext(Dispatchers.IO) {
        partyDao.deletePartyById(partyId)
    }

    suspend fun addMember(partyId: Long, name: String, emailOrPhone: String = "", colorHex: String = "#6366F1"): Long =
        withContext(Dispatchers.IO) {
            val member = MemberEntity(
                partyId = partyId,
                name = name,
                emailOrPhone = emailOrPhone,
                avatarColorHex = colorHex
            )
            memberDao.insertMember(member)
        }

    suspend fun updateMember(member: MemberEntity) = withContext(Dispatchers.IO) {
        memberDao.updateMember(member)
    }

    suspend fun deleteMember(memberId: Long) = withContext(Dispatchers.IO) {
        memberDao.deleteMemberById(memberId)
    }

    suspend fun addExpense(
        partyId: Long,
        title: String,
        amount: Double,
        paidByMemberId: Long,
        payerName: String,
        category: String,
        splitMemberIds: String = ""
    ): Long = withContext(Dispatchers.IO) {
        val expense = ExpenseEntity(
            partyId = partyId,
            title = title,
            amount = amount,
            paidByMemberId = paidByMemberId,
            payerName = payerName,
            category = category,
            splitMemberIds = splitMemberIds,
            dateMillis = System.currentTimeMillis()
        )
        expenseDao.insertExpense(expense)
    }

    suspend fun updateExpense(expense: ExpenseEntity) = withContext(Dispatchers.IO) {
        expenseDao.updateExpense(expense)
    }

    suspend fun deleteExpense(expenseId: Long) = withContext(Dispatchers.IO) {
        expenseDao.deleteExpenseById(expenseId)
    }

    suspend fun toggleSettledStatus(partyId: Long, isSettled: Boolean) = withContext(Dispatchers.IO) {
        val party = partyDao.getPartyByIdSync(partyId) ?: return@withContext
        partyDao.updateParty(party.copy(isSettled = isSettled, lastSyncedAt = System.currentTimeMillis()))
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        expenseDao.deleteAllExpenses()
        memberDao.deleteAllMembers()
        partyDao.deleteAllParties()
    }

    fun setOfflineMode(enabled: Boolean) {
        _isOfflineMode.value = enabled
        _currentAccount.value = _currentAccount.value.copy(
            syncStatus = if (enabled) SyncStatus.OFFLINE_READY else SyncStatus.SYNCED
        )
    }

    suspend fun triggerSync(): Boolean = withContext(Dispatchers.IO) {
        _currentAccount.value = _currentAccount.value.copy(syncStatus = SyncStatus.SYNCING)
        kotlinx.coroutines.delay(400)
        _currentAccount.value = _currentAccount.value.copy(
            syncStatus = SyncStatus.OFFLINE_READY,
            lastSyncMillis = System.currentTimeMillis()
        )
        true
    }
}
