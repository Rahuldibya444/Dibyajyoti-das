package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.MemberEntity
import com.example.data.local.entity.PartyEntity
import com.example.data.model.MemberSummary
import com.example.data.model.PartyFullSummary
import com.example.data.model.SettlementTransfer
import com.example.ui.components.AddExpenseDialog
import com.example.ui.components.AddMemberDialog
import com.example.ui.components.EditExpenseDialog
import com.example.ui.components.EditMemberDialog
import com.example.ui.components.EditPartyDialog
import com.example.ui.components.MemberAvatar
import com.example.ui.components.getCategoryIcon
import com.example.ui.theme.DebtRed
import com.example.ui.theme.DeepPurpleBadge
import com.example.ui.theme.GainGreen
import com.example.ui.theme.SubtitleGray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyDetailScreen(
    summary: PartyFullSummary?,
    onBack: () -> Unit,
    onAddMember: (name: String, contact: String, color: String) -> Unit,
    onDeleteMember: (Long) -> Unit,
    onAddExpense: (
        title: String,
        amount: Double,
        payerId: Long,
        payerName: String,
        category: String,
        splitMemberIds: String
    ) -> Unit,
    onDeleteExpense: (Long) -> Unit,
    onUpdateParty: (PartyEntity) -> Unit = {},
    onUpdateMember: (MemberEntity) -> Unit = {},
    onUpdateExpense: (ExpenseEntity) -> Unit = {},
    onToggleSettled: (Boolean) -> Unit,
    onExportPdf: () -> Unit,
    onSendAlert: (debtorName: String, creditorName: String, amount: String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (summary == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading party details...")
        }
        return
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Expenses", "Members", "Settlement")

    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showEditPartyDialog by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<MemberEntity?>(null) }
    var editingExpense by remember { mutableStateOf<ExpenseEntity?>(null) }

    val party = summary.party
    val currency = party.currencySymbol
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "EVENT TRACKER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            party.title,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showEditPartyDialog = true },
                        modifier = Modifier.testTag("edit_party_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Party Details",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    // PDF Export Action
                    IconButton(
                        onClick = onExportPdf,
                        modifier = Modifier.testTag("export_pdf_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export PDF Report",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Clean Minimalism Hero Total Expenses Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Top row: Total Expenses & Members pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                "Total Expenses",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "$currency${String.format(Locale.US, "%.2f", summary.totalExpense)}",
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = DeepPurpleBadge
                            ) {
                                Text(
                                    text = "${summary.members.size} Members",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }

                            FilterChip(
                                selected = party.isSettled,
                                onClick = { onToggleSettled(!party.isSettled) },
                                label = {
                                    Text(if (party.isSettled) "✓ Settled" else "Settle", fontSize = 11.sp)
                                }
                            )
                        }
                    }

                    // Divider with subtle opacity
                    HorizontalDivider(
                        color = SubtitleGray.copy(alpha = 0.3f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 14.dp)
                    )

                    // Bottom row: Split Per Person & Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Split Per Person",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "$currency${String.format(Locale.US, "%.2f", summary.averagePerPerson)}",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onExportPdf,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PDF", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = {
                                    if (summary.members.isEmpty()) {
                                        showAddMemberDialog = true
                                    } else {
                                        showAddExpenseDialog = true
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("+ Add Expense", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Tabs Row
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = when (index) {
                                    0 -> "Expenses (${summary.expenses.size})"
                                    1 -> "Members (${summary.members.size})"
                                    else -> "Settlements (${summary.settlements.size})"
                                },
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            // Tab Content
            when (selectedTabIndex) {
                0 -> ExpensesTabContent(
                    summary = summary,
                    currency = currency,
                    onAddExpenseClick = {
                        if (summary.members.isEmpty()) {
                            showAddMemberDialog = true
                        } else {
                            showAddExpenseDialog = true
                        }
                    },
                    onEditExpense = { editingExpense = it },
                    onDeleteExpense = onDeleteExpense
                )
                1 -> MembersTabContent(
                    summary = summary,
                    currency = currency,
                    onAddMemberClick = { showAddMemberDialog = true },
                    onEditMember = { editingMember = it },
                    onDeleteMember = onDeleteMember
                )
                2 -> SettlementsTabContent(
                    summary = summary,
                    currency = currency,
                    onSendAlert = onSendAlert
                )
            }
        }

        // Dialogs
        if (showAddMemberDialog) {
            AddMemberDialog(
                onDismiss = { showAddMemberDialog = false },
                onConfirm = { name, contact, color ->
                    onAddMember(name, contact, color)
                    showAddMemberDialog = false
                }
            )
        }

        if (showAddExpenseDialog && summary.members.isNotEmpty()) {
            AddExpenseDialog(
                members = summary.members,
                currencySymbol = currency,
                onDismiss = { showAddExpenseDialog = false },
                onConfirm = { title, amt, payerId, payerName, cat, splitIds ->
                    onAddExpense(title, amt, payerId, payerName, cat, splitIds)
                    showAddExpenseDialog = false
                }
            )
        }

        if (showEditPartyDialog) {
            EditPartyDialog(
                party = party,
                onDismiss = { showEditPartyDialog = false },
                onConfirm = { title, desc, curr ->
                    onUpdateParty(party.copy(title = title, description = desc, currencySymbol = curr))
                    showEditPartyDialog = false
                }
            )
        }

        editingMember?.let { memberToEdit ->
            EditMemberDialog(
                member = memberToEdit,
                onDismiss = { editingMember = null },
                onConfirm = { name, contact, color ->
                    onUpdateMember(memberToEdit.copy(name = name, emailOrPhone = contact, avatarColorHex = color))
                    editingMember = null
                }
            )
        }

        editingExpense?.let { expenseToEdit ->
            EditExpenseDialog(
                expense = expenseToEdit,
                members = summary.members,
                currencySymbol = currency,
                onDismiss = { editingExpense = null },
                onConfirm = { title, amt, payerId, payerName, cat, splitIds ->
                    onUpdateExpense(
                        expenseToEdit.copy(
                            title = title,
                            amount = amt,
                            paidByMemberId = payerId,
                            payerName = payerName,
                            category = cat,
                            splitMemberIds = splitIds
                        )
                    )
                    editingExpense = null
                }
            )
        }
    }
}

@Composable
fun ExpensesTabContent(
    summary: PartyFullSummary,
    currency: String,
    onAddExpenseClick: () -> Unit,
    onEditExpense: (ExpenseEntity) -> Unit,
    onDeleteExpense: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "ITEMIZED EXPENSES",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onAddExpenseClick,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("add_expense_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Expense", fontSize = 12.sp)
                }
            }
        }

        if (summary.expenses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No Expenses Recorded", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            "Add receipts, food, drinks, venue rentals, and split them equally or individually.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(summary.expenses, key = { it.id }) { expense ->
                ExpenseItemCard(
                    expense = expense,
                    currency = currency,
                    totalMembers = summary.members.size,
                    onEdit = { onEditExpense(expense) },
                    onDelete = { onDeleteExpense(expense.id) }
                )
            }
        }
    }
}

@Composable
fun ExpenseItemCard(
    expense: ExpenseEntity,
    currency: String,
    totalMembers: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(expense.category),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = expense.title,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Paid by ${expense.payerName} • ${expense.category}",
                        fontSize = 12.sp,
                        color = SubtitleGray
                    )
                    val splitNote = if (expense.splitMemberIds.isBlank()) {
                        "Split among all ($totalMembers)"
                    } else {
                        val count = expense.splitMemberIds.split(",").size
                        "Split among $count members"
                    }
                    Text(
                        text = splitNote,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$currency${String.format(Locale.US, "%.2f", expense.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Expense",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Expense",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MembersTabContent(
    summary: PartyFullSummary,
    currency: String,
    onAddMemberClick: () -> Unit,
    onEditMember: (MemberEntity) -> Unit,
    onDeleteMember: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "PARTY MEMBERS & BALANCES",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onAddMemberClick,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("add_member_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Member", fontSize = 12.sp)
                }
            }
        }

        if (summary.members.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No Members Added Yet", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            "Add party members to track who paid and calculate who owes whom.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(summary.memberSummaries, key = { it.member.id }) { mSummary ->
                MemberSummaryCard(
                    summary = mSummary,
                    currency = currency,
                    onEdit = { onEditMember(mSummary.member) },
                    onDelete = { onDeleteMember(mSummary.member.id) }
                )
            }

            // Clean Minimalism Dashed Add Member Button
            item {
                Surface(
                    onClick = onAddMemberClick,
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .testTag("add_new_member_dashed_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "+",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Light
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add New Member",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MemberSummaryCard(
    summary: MemberSummary,
    currency: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    MemberAvatar(
                        name = summary.member.name,
                        colorHex = summary.member.avatarColorHex,
                        size = 40
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            summary.member.name,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Paid: $currency${String.format(Locale.US, "%.2f", summary.totalPaid)}",
                            fontSize = 12.sp,
                            color = SubtitleGray
                        )
                    }
                }

                // Clean Minimalism Net Balance
                val net = summary.netBalance
                Column(horizontalAlignment = Alignment.End) {
                    when {
                        net > 0.01 -> Text(
                            text = "+$currency${String.format(Locale.US, "%.2f", net)}",
                            color = GainGreen,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                        net < -0.01 -> Text(
                            text = "-$currency${String.format(Locale.US, "%.2f", -net)}",
                            color = DebtRed,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                        else -> Text(
                            text = "Settled",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }

                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.size(24.dp).padding(top = 2.dp)) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Member",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp).padding(top = 2.dp)) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Member",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettlementsTabContent(
    summary: PartyFullSummary,
    currency: String,
    onSendAlert: (debtorName: String, creditorName: String, amount: String) -> Unit
) {
    val settlements = summary.settlements

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text(
                    "OPTIMIZED SETTLEMENT TRANSFERS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Minimal transactions calculated to balance all party debts directly.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (settlements.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD1FAE5).copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF065F46),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "All Settled Up!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF065F46)
                        )
                        Text(
                            "No member owes money. Everyone's balance is clean and balanced.",
                            fontSize = 12.sp,
                            color = Color(0xFF065F46),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(settlements) { transfer ->
                SettlementTransferCard(
                    transfer = transfer,
                    currency = currency,
                    partyTitle = summary.party.title,
                    onSendAlert = {
                        val amountFormatted = "$currency${String.format(Locale.US, "%.2f", transfer.amount)}"
                        onSendAlert(transfer.fromMemberName, transfer.toMemberName, amountFormatted)
                    }
                )
            }
        }
    }
}

@Composable
fun SettlementTransferCard(
    transfer: SettlementTransfer,
    currency: String,
    partyTitle: String,
    onSendAlert: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Debtor pays Creditor visual
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    MemberAvatar(name = transfer.fromMemberName, colorHex = "#EFB8C8", size = 36)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            transfer.fromMemberName,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text("pays", fontSize = 11.sp, color = SubtitleGray)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("→", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    MemberAvatar(name = transfer.toMemberName, colorHex = "#D0BCFF", size = 36)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        transfer.toMemberName,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "$currency${String.format(Locale.US, "%.2f", transfer.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action: Send Push Notification / Reminder
            OutlinedButton(
                onClick = onSendAlert,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Notify ${transfer.fromMemberName} (Settlement Alert)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun KpiPill(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 10.5.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
