package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BillPdfConfig
import com.example.data.model.PartyFullSummary
import com.example.ui.theme.DeepPurpleBadge
import com.example.ui.theme.GainGreen
import com.example.ui.theme.SubtitleGray
import com.example.util.QrCodeHelper
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBillDialog(
    summary: PartyFullSummary,
    initialTargetMemberName: String? = null,
    onDismiss: () -> Unit,
    onGenerateBillPdf: (BillPdfConfig) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("bill_preferences", Context.MODE_PRIVATE) }

    val savedUpi = remember { prefs.getString("last_upi_id", "") ?: "" }
    val savedPayee = remember { prefs.getString("last_payee_name", "") ?: "" }

    var upiId by remember { mutableStateOf(savedUpi) }
    var payeeName by remember {
        mutableStateOf(
            if (savedPayee.isNotBlank()) savedPayee
            else summary.members.firstOrNull()?.name ?: "Party Organizer"
        )
    }

    var isIndividualBill by remember {
        mutableStateOf(initialTargetMemberName != null)
    }

    var selectedMemberName by remember {
        mutableStateOf(
            initialTargetMemberName
                ?: summary.memberSummaries.firstOrNull { it.netBalance < -0.01 }?.member?.name
                ?: summary.members.firstOrNull()?.name
                ?: ""
        )
    }

    val currency = summary.party.currencySymbol

    // Calculate default amount based on selection
    val selectedMemberSummary = summary.memberSummaries.find { it.member.name == selectedMemberName }
    val initialAmount = if (isIndividualBill && selectedMemberSummary != null) {
        if (selectedMemberSummary.netBalance < -0.01) -selectedMemberSummary.netBalance
        else selectedMemberSummary.totalShare
    } else {
        summary.totalExpense
    }

    var amountText by remember {
        mutableStateOf(String.format(Locale.US, "%.2f", initialAmount))
    }

    // Auto-update amount when member or bill mode toggles
    LaunchedEffect(isIndividualBill, selectedMemberName) {
        val target = summary.memberSummaries.find { it.member.name == selectedMemberName }
        val newAmt = if (isIndividualBill && target != null) {
            if (target.netBalance < -0.01) -target.netBalance else target.totalShare
        } else {
            summary.totalExpense
        }
        amountText = String.format(Locale.US, "%.2f", newAmt)
    }

    var paymentNote by remember {
        mutableStateOf(
            if (isIndividualBill) "Party settlement for ${summary.party.title}"
            else "Full party bill: ${summary.party.title}"
        )
    }

    var memberDropdownExpanded by remember { mutableStateOf(false) }

    // Live QR preview
    val parsedAmount = amountText.toDoubleOrNull()
    var liveQrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(upiId, payeeName, parsedAmount, paymentNote) {
        if (upiId.isNotBlank()) {
            val uri = QrCodeHelper.buildUpiUri(
                upiId = upiId.trim(),
                payeeName = payeeName.trim(),
                amount = parsedAmount,
                note = paymentNote.trim()
            )
            liveQrBitmap = QrCodeHelper.generateQrBitmap(uri, 240)
        } else {
            liveQrBitmap = null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DeepPurpleBadge,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Create Bill & PDF",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "With UPI ID and bottom QR Code for payment",
                        fontSize = 11.sp,
                        color = SubtitleGray
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Bill Scope Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isIndividualBill,
                        onClick = { isIndividualBill = false },
                        label = { Text("Group Bill", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = isIndividualBill,
                        onClick = { isIndividualBill = true },
                        label = { Text("Member Bill", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (isIndividualBill) {
                    ExposedDropdownMenuBox(
                        expanded = memberDropdownExpanded,
                        onExpandedChange = { memberDropdownExpanded = !memberDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedMemberName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Member to Bill") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = memberDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = memberDropdownExpanded,
                            onDismissRequest = { memberDropdownExpanded = false }
                        ) {
                            summary.memberSummaries.forEach { mSummary ->
                                val dueText = if (mSummary.netBalance < -0.01) " (Owes $currency${String.format(Locale.US, "%.2f", -mSummary.netBalance)})"
                                else if (mSummary.netBalance > 0.01) " (Gets $currency${String.format(Locale.US, "%.2f", mSummary.netBalance)})"
                                else " (Settled)"

                                DropdownMenuItem(
                                    text = { Text("${mSummary.member.name}$dueText", fontSize = 13.sp) },
                                    onClick = {
                                        selectedMemberName = mSummary.member.name
                                        memberDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // UPI ID input field
                OutlinedTextField(
                    value = upiId,
                    onValueChange = { upiId = it },
                    label = { Text("UPI ID (Google Pay / PhonePe / Paytm)") },
                    placeholder = { Text("e.g. yourname@okaxis or 9876543210@paytm") },
                    leadingIcon = {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("bill_upi_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Payee Name
                OutlinedTextField(
                    value = payeeName,
                    onValueChange = { payeeName = it },
                    label = { Text("Payee / Receiver Name") },
                    placeholder = { Text("e.g. Party Host or Organizer") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Bill Amount ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("bill_amount_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Payment Note
                OutlinedTextField(
                    value = paymentNote,
                    onValueChange = { paymentNote = it },
                    label = { Text("Payment Note") },
                    leadingIcon = {
                        Icon(Icons.Default.Description, contentDescription = null, tint = SubtitleGray)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Live QR Code Preview Card
                if (upiId.isNotBlank() && liveQrBitmap != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .background(Color.White, RoundedCornerShape(8.dp))
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                liveQrBitmap?.let { bmp ->
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Live UPI QR Preview",
                                        modifier = Modifier.size(68.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode,
                                        contentDescription = null,
                                        tint = GainGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Live UPI QR Generated",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = GainGreen
                                    )
                                }
                                Text(
                                    text = upiId.trim(),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Will be placed cleanly down at the bottom of the bill PDF.",
                                    fontSize = 10.sp,
                                    color = SubtitleGray,
                                    lineHeight = 14.sp
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
                    // Save UPI ID & payee to preferences for instant recall
                    if (upiId.isNotBlank()) {
                        prefs.edit()
                            .putString("last_upi_id", upiId.trim())
                            .putString("last_payee_name", payeeName.trim())
                            .apply()
                    }

                    val config = BillPdfConfig(
                        upiId = upiId.trim(),
                        payeeName = payeeName.trim(),
                        note = paymentNote.trim(),
                        customAmount = amountText.toDoubleOrNull(),
                        targetMemberId = if (isIndividualBill) selectedMemberSummary?.member?.id else null,
                        targetMemberName = if (isIndividualBill) selectedMemberName else null
                    )
                    onGenerateBillPdf(config)
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("create_and_share_bill_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create & Share Bill PDF", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
