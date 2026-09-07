package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.MemberEntity
import com.example.data.local.entity.PartyEntity

@Composable
fun CreatePartyDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String, currency: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf("₹") }
    val currencies = listOf("₹", "$", "€", "£", "¥")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Create New Party", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Party Name (e.g. Summer BBQ)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Location (Optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Currency Symbol", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currencies.forEach { curr ->
                        FilterChip(
                            selected = selectedCurrency == curr,
                            onClick = { selectedCurrency = curr },
                            label = { Text(curr, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title.trim(), description.trim(), selectedCurrency)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Create Party")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditPartyDialog(
    party: PartyEntity,
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String, currency: String) -> Unit
) {
    var title by remember { mutableStateOf(party.title) }
    var description by remember { mutableStateOf(party.description) }
    var selectedCurrency by remember { mutableStateOf(party.currencySymbol) }
    val currencies = listOf("₹", "$", "€", "£", "¥")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Party Details", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Party Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Location (Optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Currency Symbol", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currencies.forEach { curr ->
                        FilterChip(
                            selected = selectedCurrency == curr,
                            onClick = { selectedCurrency = curr },
                            label = { Text(curr, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title.trim(), description.trim(), selectedCurrency)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddMemberDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, emailOrPhone: String, colorHex: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    val colors = listOf("#4F46E5", "#EC4899", "#10B981", "#F59E0B", "#8B5CF6", "#06B6D4", "#EF4444")
    var selectedColor by remember { mutableStateOf(colors.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add Party Member", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Member Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Phone or Note (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Avatar Accent Color", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colors.forEach { hex ->
                        val isSelected = selectedColor == hex
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .clickable { selectedColor = hex }
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), contact.trim(), selectedColor)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Add Member")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditMemberDialog(
    member: MemberEntity,
    onDismiss: () -> Unit,
    onConfirm: (name: String, emailOrPhone: String, colorHex: String) -> Unit
) {
    var name by remember { mutableStateOf(member.name) }
    var contact by remember { mutableStateOf(member.emailOrPhone) }
    val colors = listOf("#4F46E5", "#EC4899", "#10B981", "#F59E0B", "#8B5CF6", "#06B6D4", "#EF4444")
    var selectedColor by remember { mutableStateOf(member.avatarColorHex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Member", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Member Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Phone or Note (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Avatar Accent Color", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colors.forEach { hex ->
                        val isSelected = selectedColor.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .clickable { selectedColor = hex }
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), contact.trim(), selectedColor)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddExpenseDialog(
    members: List<MemberEntity>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        amount: Double,
        payerId: Long,
        payerName: String,
        category: String,
        splitMemberIds: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Food") }
    val categories = listOf("Food", "Drinks", "Venue", "Entertainment", "Supplies", "Other")

    var selectedPayer by remember { mutableStateOf(members.firstOrNull()) }
    var payerExpanded by remember { mutableStateOf(false) }

    // Split options
    var splitEquallyAll by remember { mutableStateOf(true) }
    val selectedSplitMembers = remember {
        mutableStateListOf<Long>().apply {
            addAll(members.map { it.id })
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add Party Expense", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Expense Title (e.g. Ice & Beverages)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Category", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Paid By", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                ExposedDropdownMenuBox(
                    expanded = payerExpanded,
                    onExpandedChange = { payerExpanded = !payerExpanded },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    OutlinedTextField(
                        value = selectedPayer?.name ?: "Select Payer",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = payerExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = payerExpanded,
                        onDismissRequest = { payerExpanded = false }
                    ) {
                        members.forEach { m ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        MemberAvatar(name = m.name, colorHex = m.avatarColorHex, size = 26)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(m.name)
                                    }
                                },
                                onClick = {
                                    selectedPayer = m
                                    payerExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Split Between", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { splitEquallyAll = true }
                ) {
                    RadioButton(
                        selected = splitEquallyAll,
                        onClick = { splitEquallyAll = true }
                    )
                    Text("Split equally among all members (${members.size})", fontSize = 13.sp)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { splitEquallyAll = false }
                ) {
                    RadioButton(
                        selected = !splitEquallyAll,
                        onClick = { splitEquallyAll = false }
                    )
                    Text("Select specific members only", fontSize = 13.sp)
                }

                if (!splitEquallyAll) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 4.dp)
                    ) {
                        members.forEach { m ->
                            val isChecked = selectedSplitMembers.contains(m.id)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) {
                                            if (selectedSplitMembers.size > 1) {
                                                selectedSplitMembers.remove(m.id)
                                            }
                                        } else {
                                            selectedSplitMembers.add(m.id)
                                        }
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedSplitMembers.add(m.id)
                                        else if (selectedSplitMembers.size > 1) selectedSplitMembers.remove(m.id)
                                    }
                                )
                                MemberAvatar(name = m.name, colorHex = m.avatarColorHex, size = 24)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(m.name, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val amount = amountText.toDoubleOrNull() ?: 0.0
            Button(
                onClick = {
                    val payer = selectedPayer ?: members.firstOrNull() ?: return@Button
                    val splitIds = if (splitEquallyAll) "" else selectedSplitMembers.joinToString(",")
                    onConfirm(
                        title.trim(),
                        amount,
                        payer.id,
                        payer.name,
                        selectedCategory,
                        splitIds
                    )
                },
                enabled = title.isNotBlank() && amount > 0 && selectedPayer != null
            ) {
                Text("Save Expense")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditExpenseDialog(
    expense: ExpenseEntity,
    members: List<MemberEntity>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        amount: Double,
        payerId: Long,
        payerName: String,
        category: String,
        splitMemberIds: String
    ) -> Unit
) {
    var title by remember { mutableStateOf(expense.title) }
    var amountText by remember {
        mutableStateOf(
            if (expense.amount % 1.0 == 0.0) expense.amount.toInt().toString() else expense.amount.toString()
        )
    }
    var selectedCategory by remember { mutableStateOf(expense.category) }
    val categories = listOf("Drinks", "Food", "Music & Gear", "Supplies", "Venue", "Decor", "Other")

    var selectedPayer by remember {
        mutableStateOf(members.find { it.id == expense.paidByMemberId } ?: members.firstOrNull())
    }
    var payerExpanded by remember { mutableStateOf(false) }

    val initialSplitIds = remember {
        if (expense.splitMemberIds.isBlank()) {
            members.map { it.id }.toSet()
        } else {
            expense.splitMemberIds.split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()
        }
    }
    var splitEquallyAll by remember {
        mutableStateOf(expense.splitMemberIds.isBlank() || (initialSplitIds.size == members.size && members.isNotEmpty()))
    }
    val selectedSplitMembers = remember {
        mutableStateListOf<Long>().apply {
            addAll(if (splitEquallyAll) members.map { it.id } else initialSplitIds)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Expense", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Expense Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Category", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Paid By", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                ExposedDropdownMenuBox(
                    expanded = payerExpanded,
                    onExpandedChange = { payerExpanded = !payerExpanded },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    OutlinedTextField(
                        value = selectedPayer?.name ?: "Select Payer",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = payerExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = payerExpanded,
                        onDismissRequest = { payerExpanded = false }
                    ) {
                        members.forEach { m ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        MemberAvatar(name = m.name, colorHex = m.avatarColorHex, size = 26)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(m.name)
                                    }
                                },
                                onClick = {
                                    selectedPayer = m
                                    payerExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Split Between", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { splitEquallyAll = true }
                ) {
                    RadioButton(
                        selected = splitEquallyAll,
                        onClick = { splitEquallyAll = true }
                    )
                    Text("Split equally among all members (${members.size})", fontSize = 13.sp)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { splitEquallyAll = false }
                ) {
                    RadioButton(
                        selected = !splitEquallyAll,
                        onClick = { splitEquallyAll = false }
                    )
                    Text("Select specific members only", fontSize = 13.sp)
                }

                if (!splitEquallyAll) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 4.dp)
                    ) {
                        members.forEach { m ->
                            val isChecked = selectedSplitMembers.contains(m.id)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) {
                                            if (selectedSplitMembers.size > 1) {
                                                selectedSplitMembers.remove(m.id)
                                            }
                                        } else {
                                            selectedSplitMembers.add(m.id)
                                        }
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedSplitMembers.add(m.id)
                                        else if (selectedSplitMembers.size > 1) selectedSplitMembers.remove(m.id)
                                    }
                                )
                                MemberAvatar(name = m.name, colorHex = m.avatarColorHex, size = 24)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(m.name, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val amount = amountText.toDoubleOrNull() ?: 0.0
            Button(
                onClick = {
                    val payer = selectedPayer ?: members.firstOrNull() ?: return@Button
                    val splitIds = if (splitEquallyAll) "" else selectedSplitMembers.joinToString(",")
                    onConfirm(
                        title.trim(),
                        amount,
                        payer.id,
                        payer.name,
                        selectedCategory,
                        splitIds
                    )
                },
                enabled = title.isNotBlank() && amount > 0 && selectedPayer != null
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
