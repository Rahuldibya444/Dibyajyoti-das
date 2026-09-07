package com.example.data.model

import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.MemberEntity
import com.example.data.local.entity.PartyEntity

enum class SyncStatus {
    SYNCED,
    SYNCING,
    OFFLINE_READY,
    PENDING_UPLOAD
}

data class UserAccount(
    val id: String = "local_device",
    val email: String = "",
    val displayName: String = "Organizer",
    val photoUrl: String? = null,
    val isSignedInWithGoogle: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.OFFLINE_READY,
    val lastSyncMillis: Long = System.currentTimeMillis()
)

data class MemberSummary(
    val member: MemberEntity,
    val totalPaid: Double,
    val totalShare: Double,
    val netBalance: Double // > 0 means to receive; < 0 means owes
) {
    val isOwed: Boolean get() = netBalance > 0.009
    val owes: Boolean get() = netBalance < -0.009
    val isSettled: Boolean get() = !isOwed && !owes
}

data class SettlementTransfer(
    val fromMemberId: Long,
    val fromMemberName: String,
    val toMemberId: Long,
    val toMemberName: String,
    val amount: Double,
    var isSettled: Boolean = false
)

data class PartyFullSummary(
    val party: PartyEntity,
    val members: List<MemberEntity>,
    val expenses: List<ExpenseEntity>,
    val memberSummaries: List<MemberSummary>,
    val settlements: List<SettlementTransfer>,
    val totalExpense: Double,
    val averagePerPerson: Double
)

data class BillPdfConfig(
    val upiId: String = "",
    val payeeName: String = "",
    val note: String = "",
    val customAmount: Double? = null,
    val targetMemberId: Long? = null,
    val targetMemberName: String? = null
)
