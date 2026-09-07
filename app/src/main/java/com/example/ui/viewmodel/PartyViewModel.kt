package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.MemberEntity
import com.example.data.local.entity.PartyEntity
import com.example.data.model.PartyFullSummary
import com.example.data.model.UserAccount
import com.example.data.repository.PartyRepository
import com.example.util.NotificationHelper
import com.example.util.PdfReportExporter
import com.example.util.SettlementCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DarkModeSetting {
    SYSTEM,
    LIGHT,
    DARK
}

class PartyViewModel(private val repository: PartyRepository) : ViewModel() {

    val userAccount: StateFlow<UserAccount> = repository.currentAccount
    val isOfflineMode: StateFlow<Boolean> = repository.isOfflineMode

    private val _darkModeSetting = MutableStateFlow(DarkModeSetting.SYSTEM)
    val darkModeSetting: StateFlow<DarkModeSetting> = _darkModeSetting.asStateFlow()

    val parties: StateFlow<List<PartyEntity>> = repository.getParties()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedPartyId = MutableStateFlow<Long?>(null)
    val selectedPartyId: StateFlow<Long?> = _selectedPartyId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentPartySummary: StateFlow<PartyFullSummary?> = _selectedPartyId
        .flatMapLatest { partyId ->
            if (partyId == null) {
                flowOf(null)
            } else {
                combine(
                    repository.getPartyById(partyId),
                    repository.getMembersByParty(partyId),
                    repository.getExpensesByParty(partyId)
                ) { party, members, expenses ->
                    if (party == null) null
                    else SettlementCalculator.calculateSummary(party, members, expenses)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun setDarkMode(setting: DarkModeSetting) {
        _darkModeSetting.value = setting
    }

    fun selectParty(partyId: Long?) {
        _selectedPartyId.value = partyId
    }

    fun createParty(title: String, description: String, currencySymbol: String, dateMillis: Long) {
        viewModelScope.launch {
            val id = repository.createParty(title, description, currencySymbol, dateMillis)
            _selectedPartyId.value = id
            _userMessage.value = "Created party \"$title\""
        }
    }

    fun deleteParty(partyId: Long) {
        viewModelScope.launch {
            repository.deleteParty(partyId)
            if (_selectedPartyId.value == partyId) {
                _selectedPartyId.value = null
            }
            _userMessage.value = "Party deleted"
        }
    }

    fun addMember(partyId: Long, name: String, emailOrPhone: String, colorHex: String) {
        viewModelScope.launch {
            repository.addMember(partyId, name, emailOrPhone, colorHex)
            _userMessage.value = "Added member $name"
        }
    }

    fun deleteMember(memberId: Long) {
        viewModelScope.launch {
            repository.deleteMember(memberId)
            _userMessage.value = "Member removed"
        }
    }

    fun addExpense(
        partyId: Long,
        title: String,
        amount: Double,
        paidByMemberId: Long,
        payerName: String,
        category: String,
        splitMemberIds: String
    ) {
        viewModelScope.launch {
            repository.addExpense(
                partyId = partyId,
                title = title,
                amount = amount,
                paidByMemberId = paidByMemberId,
                payerName = payerName,
                category = category,
                splitMemberIds = splitMemberIds
            )
            _userMessage.value = "Added expense \"$title\""
        }
    }

    fun deleteExpense(expenseId: Long) {
        viewModelScope.launch {
            repository.deleteExpense(expenseId)
            _userMessage.value = "Expense deleted"
        }
    }

    fun updateParty(party: PartyEntity) {
        viewModelScope.launch {
            repository.updateParty(party)
            _userMessage.value = "Party \"${party.title}\" updated"
        }
    }

    fun updateMember(member: MemberEntity) {
        viewModelScope.launch {
            repository.updateMember(member)
            _userMessage.value = "Member \"${member.name}\" updated"
        }
    }

    fun updateExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.updateExpense(expense)
            _userMessage.value = "Expense \"${expense.title}\" updated"
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _selectedPartyId.value = null
            _userMessage.value = "All party data wiped clean"
        }
    }

    fun togglePartySettled(partyId: Long, isSettled: Boolean) {
        viewModelScope.launch {
            repository.toggleSettledStatus(partyId, isSettled)
            _userMessage.value = if (isSettled) "Party marked as Settled!" else "Party marked as Active"
        }
    }

    fun exportPdf(context: Context) {
        val summary = currentPartySummary.value
        if (summary == null) {
            _userMessage.value = "No party summary available to export"
            return
        }
        val file = PdfReportExporter.generateAndSharePdf(context, summary)
        if (file != null) {
            _userMessage.value = "PDF Summary generated: ${file.name}"
        } else {
            _userMessage.value = "Failed to generate PDF report"
        }
    }

    fun sendSettlementNotification(
        context: Context,
        partyTitle: String,
        debtorName: String,
        creditorName: String,
        amount: String
    ) {
        NotificationHelper.showSettlementReminder(
            context = context,
            partyTitle = partyTitle,
            debtorName = debtorName,
            creditorName = creditorName,
            amount = amount
        )
        _userMessage.value = "Settlement reminder sent for $debtorName"
    }

    fun toggleOfflineMode(enabled: Boolean, context: Context) {
        repository.setOfflineMode(enabled)
        val msg = if (enabled) "Offline Mode enabled. Calculations stored on device." else "Offline Mode disabled."
        _userMessage.value = msg
        NotificationHelper.showSyncNotification(context, msg)
    }

    fun refreshData(context: Context) {
        viewModelScope.launch {
            repository.triggerSync()
            _userMessage.value = "Local database verified and refreshed"
            NotificationHelper.showSyncNotification(context, "Local party database refreshed")
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val repository = PartyRepository(context)
            return PartyViewModel(repository) as T
        }
    }
}
