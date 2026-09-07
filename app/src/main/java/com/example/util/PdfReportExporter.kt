package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.BillPdfConfig
import com.example.data.model.PartyFullSummary
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportExporter {

    fun generateAndSharePdf(
        context: Context,
        summary: PartyFullSummary,
        billConfig: BillPdfConfig? = null
    ): File? {
        try {
            val file = generatePdfFile(context, summary, billConfig)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareSubject = if (billConfig?.targetMemberName != null) {
                "Settlement Bill for ${billConfig.targetMemberName}: ${summary.party.title}"
            } else {
                "Party Bill & Report: ${summary.party.title}"
            }

            val shareText = if (billConfig?.upiId?.isNotBlank() == true) {
                "Attached is the official bill for \"${summary.party.title}\" with payment breakdown and scannable UPI QR code (UPI ID: ${billConfig.upiId})."
            } else {
                "Attached is the expense report for \"${summary.party.title}\"."
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, shareSubject)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Bill PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun generatePdfFile(
        context: Context,
        summary: PartyFullSummary,
        billConfig: BillPdfConfig? = null
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 points
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
        val currency = summary.party.currencySymbol

        val isTargetMemberBill = billConfig?.targetMemberName != null
        val targetMember = if (isTargetMemberBill) {
            summary.memberSummaries.find { it.member.name.equals(billConfig.targetMemberName, ignoreCase = true) }
        } else null

        val billAmount: Double = when {
            billConfig?.customAmount != null && billConfig.customAmount > 0.0 -> billConfig.customAmount
            targetMember != null && targetMember.netBalance < -0.01 -> -targetMember.netBalance
            else -> summary.totalExpense
        }

        // Top Brand Header Banner
        paint.color = Color.parseColor("#1E1B4B")
        canvas.drawRect(0f, 0f, 595f, 90f, paint)

        // Title text
        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val headerTitle = if (isTargetMemberBill) {
            "MEMBER SETTLEMENT BILL"
        } else {
            "OFFICIAL PARTY EXPENSE BILL"
        }
        canvas.drawText(headerTitle, 32f, 42f, paint)

        paint.textSize = 11f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.parseColor("#C7D2FE")
        val headerSubtitle = if (isTargetMemberBill) {
            "Payment Invoice & UPI Settlement for ${billConfig?.targetMemberName}"
        } else {
            "Expense Summary Audit & Group Settlement Invoice"
        }
        canvas.drawText(headerSubtitle, 32f, 62f, paint)

        val reportGeneratedTime = timeFormat.format(Date())
        paint.textSize = 9f
        paint.color = Color.parseColor("#A5B4FC")
        val dateTextWidth = paint.measureText(reportGeneratedTime)
        canvas.drawText(reportGeneratedTime, 595f - 32f - dateTextWidth, 52f, paint)

        var y = 116f

        // Party Info Header
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 17f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(summary.party.title, 32f, y, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.parseColor("#64748B")
        val partyDateStr = "Event Date: ${dateFormat.format(Date(summary.party.dateMillis))}"
        canvas.drawText(partyDateStr, 32f, y + 16f, paint)

        if (summary.party.description.isNotBlank()) {
            paint.textSize = 9.5f
            paint.color = Color.parseColor("#475569")
            canvas.drawText("Note: ${summary.party.description}", 32f, y + 30f, paint)
            y += 14f
        }

        y += 40f

        if (isTargetMemberBill && targetMember != null) {
            // Individual Member Bill Card
            paint.color = Color.parseColor("#F8FAFC")
            canvas.drawRoundRect(RectF(32f, y, 563f, y + 74f), 8f, 8f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            paint.color = Color.parseColor("#E2E8F0")
            canvas.drawRoundRect(RectF(32f, y, 563f, y + 74f), 8f, 8f, paint)
            paint.style = Paint.Style.FILL

            // Left: Bill To
            paint.color = Color.parseColor("#64748B")
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("BILL ISSUED TO", 44f, y + 20f, paint)

            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 15f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(targetMember.member.name, 44f, y + 40f, paint)

            paint.color = Color.parseColor("#64748B")
            paint.textSize = 9.5f
            paint.typeface = Typeface.DEFAULT
            val contactInfo = if (targetMember.member.emailOrPhone.isNotBlank()) "Contact: ${targetMember.member.emailOrPhone}" else "Party Participant"
            canvas.drawText(contactInfo, 44f, y + 58f, paint)

            // Right: Amount Due
            paint.color = Color.parseColor("#DC2626")
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val amtLabel = "TOTAL AMOUNT PAYABLE"
            val amtLabelWidth = paint.measureText(amtLabel)
            canvas.drawText(amtLabel, 550f - amtLabelWidth, y + 20f, paint)

            paint.color = Color.parseColor("#B91C1C")
            paint.textSize = 20f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val amtText = "$currency${String.format(Locale.US, "%.2f", billAmount)}"
            val amtTextWidth = paint.measureText(amtText)
            canvas.drawText(amtText, 550f - amtTextWidth, y + 46f, paint)

            val statusText = "STATUS: PENDING PAYMENT"
            paint.color = Color.parseColor("#DC2626")
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val statusWidth = paint.measureText(statusText)
            canvas.drawText(statusText, 550f - statusWidth, y + 62f, paint)

            y += 88f

            // Member Financial Breakdown Table
            paint.color = Color.parseColor("#1E293B")
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("INDIVIDUAL CALCULATION SUMMARY", 32f, y, paint)
            y += 12f

            drawKpiCard(canvas, 32f, y, 125f, 44f, "TOTAL PAID", "$currency${String.format(Locale.US, "%.2f", targetMember.totalPaid)}", Color.parseColor("#F0FDF4"), Color.parseColor("#15803D"))
            drawKpiCard(canvas, 167f, y, 125f, 44f, "ALLOCATED SHARE", "$currency${String.format(Locale.US, "%.2f", targetMember.totalShare)}", Color.parseColor("#FFF7ED"), Color.parseColor("#C2410C"))
            drawKpiCard(canvas, 302f, y, 125f, 44f, "NET BALANCE", "-$currency${String.format(Locale.US, "%.2f", billAmount)}", Color.parseColor("#FEF2F2"), Color.parseColor("#DC2626"))
            drawKpiCard(canvas, 437f, y, 126f, 44f, "PARTY TOTAL", "$currency${String.format(Locale.US, "%.2f", summary.totalExpense)}", Color.parseColor("#EEF2FF"), Color.parseColor("#4338CA"))
            y += 58f

            // Itemized expenses for this member
            val memberExpenses = summary.expenses.filter { exp ->
                exp.paidByMemberId == targetMember.member.id || exp.splitMemberIds.isBlank() || exp.splitMemberIds.split(",").contains(targetMember.member.id.toString())
            }.take(5)

            if (memberExpenses.isNotEmpty()) {
                paint.color = Color.parseColor("#1E293B")
                paint.textSize = 11f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("PARTICIPATING EXPENSES", 32f, y, paint)
                y += 10f

                paint.color = Color.parseColor("#F1F5F9")
                canvas.drawRoundRect(RectF(32f, y, 563f, y + 18f), 3f, 3f, paint)
                paint.color = Color.parseColor("#475569")
                paint.textSize = 8.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("DESCRIPTION", 40f, y + 12f, paint)
                canvas.drawText("CATEGORY", 240f, y + 12f, paint)
                canvas.drawText("PAID BY", 340f, y + 12f, paint)
                canvas.drawText("AMOUNT", 490f, y + 12f, paint)
                y += 20f

                memberExpenses.forEachIndexed { idx, exp ->
                    if (idx % 2 == 1) {
                        paint.color = Color.parseColor("#F8FAFC")
                        canvas.drawRect(32f, y, 563f, y + 16f, paint)
                    }
                    paint.color = Color.parseColor("#1E293B")
                    paint.textSize = 8.5f
                    paint.typeface = Typeface.DEFAULT
                    val shortTitle = if (exp.title.length > 25) exp.title.take(23) + "..." else exp.title
                    canvas.drawText(shortTitle, 40f, y + 12f, paint)
                    canvas.drawText(exp.category, 240f, y + 12f, paint)
                    canvas.drawText(exp.payerName, 340f, y + 12f, paint)

                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    val expAmt = "$currency${String.format(Locale.US, "%.2f", exp.amount)}"
                    val expAmtWidth = paint.measureText(expAmt)
                    canvas.drawText(expAmt, 550f - expAmtWidth, y + 12f, paint)
                    y += 16f
                }
            }

        } else {
            // Full Party Bill & Audit
            drawKpiCard(canvas, 32f, y, 120f, 44f, "TOTAL EXPENSE", "$currency${String.format(Locale.US, "%.2f", summary.totalExpense)}", Color.parseColor("#EEF2FF"), Color.parseColor("#4338CA"))
            drawKpiCard(canvas, 162f, y, 120f, 44f, "MEMBERS", "${summary.members.size} Persons", Color.parseColor("#F0FDF4"), Color.parseColor("#15803D"))
            drawKpiCard(canvas, 292f, y, 130f, 44f, "PER PERSON AVG", "$currency${String.format(Locale.US, "%.2f", summary.averagePerPerson)}", Color.parseColor("#FFF7ED"), Color.parseColor("#C2410C"))
            drawKpiCard(canvas, 432f, y, 131f, 44f, "TRANSACTIONS", "${summary.expenses.size} Items", Color.parseColor("#FAF5FF"), Color.parseColor("#7E22CE"))
            y += 56f

            // Member Balances Table
            paint.color = Color.parseColor("#1E293B")
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("MEMBER EXPENSE & BALANCE AUDIT", 32f, y, paint)
            y += 10f

            // Table Header
            paint.color = Color.parseColor("#F1F5F9")
            canvas.drawRoundRect(RectF(32f, y, 563f, y + 18f), 3f, 3f, paint)
            paint.color = Color.parseColor("#475569")
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("MEMBER", 42f, y + 13f, paint)
            canvas.drawText("PAID", 200f, y + 13f, paint)
            canvas.drawText("SHARE", 285f, y + 13f, paint)
            canvas.drawText("NET BALANCE", 380f, y + 13f, paint)
            canvas.drawText("STATUS", 480f, y + 13f, paint)
            y += 20f

            // Table Rows (up to 5 members to leave ample room for the bottom payment section)
            summary.memberSummaries.take(5).forEachIndexed { index, mSummary ->
                if (index % 2 == 1) {
                    paint.color = Color.parseColor("#F8FAFC")
                    canvas.drawRect(32f, y, 563f, y + 16f, paint)
                }

                paint.color = Color.parseColor("#1E293B")
                paint.typeface = Typeface.DEFAULT
                paint.textSize = 8.5f
                canvas.drawText(mSummary.member.name, 42f, y + 12f, paint)
                canvas.drawText("$currency${String.format(Locale.US, "%.2f", mSummary.totalPaid)}", 200f, y + 12f, paint)
                canvas.drawText("$currency${String.format(Locale.US, "%.2f", mSummary.totalShare)}", 285f, y + 12f, paint)

                val net = mSummary.netBalance
                val netStr = if (net >= 0) "+$currency${String.format(Locale.US, "%.2f", net)}" else "-$currency${String.format(Locale.US, "%.2f", -net)}"
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                if (net > 0.01) {
                    paint.color = Color.parseColor("#15803D")
                    canvas.drawText(netStr, 380f, y + 12f, paint)
                    canvas.drawText("Gets Back", 480f, y + 12f, paint)
                } else if (net < -0.01) {
                    paint.color = Color.parseColor("#DC2626")
                    canvas.drawText(netStr, 380f, y + 12f, paint)
                    canvas.drawText("Owes", 480f, y + 12f, paint)
                } else {
                    paint.color = Color.parseColor("#64748B")
                    canvas.drawText("$currency 0.00", 380f, y + 12f, paint)
                    canvas.drawText("Settled", 480f, y + 12f, paint)
                }
                y += 17f
            }

            if (summary.memberSummaries.size > 5) {
                paint.color = Color.parseColor("#64748B")
                paint.textSize = 8f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                canvas.drawText("+ ${summary.memberSummaries.size - 5} more members listed in app.", 42f, y + 10f, paint)
                y += 14f
            }

            y += 8f

            // Settlement transfers
            if (summary.settlements.isNotEmpty()) {
                paint.color = Color.parseColor("#1E293B")
                paint.textSize = 11f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("RECOMMENDED TRANSFERS", 32f, y, paint)
                y += 10f

                summary.settlements.take(3).forEach { transfer ->
                    paint.color = Color.parseColor("#F8FAFC")
                    canvas.drawRoundRect(RectF(32f, y, 563f, y + 20f), 3f, 3f, paint)
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 1f
                    paint.color = Color.parseColor("#E2E8F0")
                    canvas.drawRoundRect(RectF(32f, y, 563f, y + 20f), 3f, 3f, paint)
                    paint.style = Paint.Style.FILL

                    paint.color = Color.parseColor("#DC2626")
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paint.textSize = 9f
                    canvas.drawText(transfer.fromMemberName, 44f, y + 14f, paint)

                    paint.color = Color.parseColor("#64748B")
                    paint.typeface = Typeface.DEFAULT
                    canvas.drawText("pays", 140f, y + 14f, paint)

                    paint.color = Color.parseColor("#15803D")
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText(transfer.toMemberName, 170f, y + 14f, paint)

                    val amountStr = "$currency${String.format(Locale.US, "%.2f", transfer.amount)}"
                    paint.color = Color.parseColor("#4338CA")
                    val amtWidth = paint.measureText(amountStr)
                    canvas.drawText(amountStr, 550f - amtWidth, y + 14f, paint)
                    y += 24f
                }
            }
        }

        // =========================================================================
        // DOWN BOTTOM IN THE PDF: PAYMENT & UPI QR CODE SECTION
        // As strictly requested: "The qr and UPI open paper Down bottom in the pdf"
        // =========================================================================
        val bottomSectionTop = 618f
        val bottomSectionHeight = 168f
        val bottomRect = RectF(32f, bottomSectionTop, 563f, bottomSectionTop + bottomSectionHeight)

        // Card background & border
        paint.color = Color.parseColor("#F5F3FF") // Gentle lavender/indigo tint
        canvas.drawRoundRect(bottomRect, 10f, 10f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        paint.color = Color.parseColor("#C4B5FD") // Lavender outline
        canvas.drawRoundRect(bottomRect, 10f, 10f, paint)
        paint.style = Paint.Style.FILL

        // Header strip for payment box
        paint.color = Color.parseColor("#4338CA")
        canvas.drawRoundRect(RectF(32f, bottomSectionTop, 563f, bottomSectionTop + 26f), 10f, 10f, paint)
        // Overwrite bottom corners of header strip to be square
        canvas.drawRect(32f, bottomSectionTop + 14f, 563f, bottomSectionTop + 26f, paint)

        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val payTitle = if (billConfig?.upiId?.isNotBlank() == true) {
            "PAYMENT DETAILS • SCAN WITH ANY UPI APP TO PAY"
        } else {
            "SETTLEMENT & PAYMENT DETAILS"
        }
        canvas.drawText(payTitle, 46f, bottomSectionTop + 18f, paint)

        val upiId = billConfig?.upiId?.trim() ?: ""
        val payeeName = billConfig?.payeeName?.trim()?.ifBlank { "Party Organizer" } ?: "Party Organizer"
        val paymentNote = billConfig?.note?.trim()?.ifBlank { "Party bill for ${summary.party.title}" } ?: "Party bill for ${summary.party.title}"

        // Generate QR Code bitmap if UPI ID is present
        val qrSize = 115
        var qrDrawn = false

        if (upiId.isNotBlank()) {
            val upiUri = QrCodeHelper.buildUpiUri(
                upiId = upiId,
                payeeName = payeeName,
                amount = billAmount,
                note = paymentNote
            )
            val qrBitmap: Bitmap? = QrCodeHelper.generateQrBitmap(upiUri, 320)

            if (qrBitmap != null) {
                // White background card for QR Code
                val qrBoxX = 46f
                val qrBoxY = bottomSectionTop + 36f
                paint.color = Color.WHITE
                canvas.drawRoundRect(RectF(qrBoxX, qrBoxY, qrBoxX + qrSize + 10f, qrBoxY + qrSize + 10f), 6f, 6f, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f
                paint.color = Color.parseColor("#CBD5E1")
                canvas.drawRoundRect(RectF(qrBoxX, qrBoxY, qrBoxX + qrSize + 10f, qrBoxY + qrSize + 10f), 6f, 6f, paint)
                paint.style = Paint.Style.FILL

                // Draw QR Code bitmap
                val srcRect = Rect(0, 0, qrBitmap.width, qrBitmap.height)
                val dstRect = Rect(
                    (qrBoxX + 5f).toInt(),
                    (qrBoxY + 5f).toInt(),
                    (qrBoxX + 5f + qrSize).toInt(),
                    (qrBoxY + 5f + qrSize).toInt()
                )
                canvas.drawBitmap(qrBitmap, srcRect, dstRect, null)
                qrDrawn = true
            }
        }

        val textStartX = if (qrDrawn) 186f else 50f
        var textY = bottomSectionTop + 46f

        // UPI ID Label & Value
        paint.color = Color.parseColor("#475569")
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("UPI ID (VPA):", textStartX, textY, paint)

        paint.color = Color.parseColor("#1E1B4B")
        paint.textSize = 11.5f
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        val displayUpi = if (upiId.isNotBlank()) upiId else "Not provided (Pay directly to organizer)"
        canvas.drawText(displayUpi, textStartX, textY + 14f, paint)
        textY += 28f

        // Payee Name & Amount Row
        paint.color = Color.parseColor("#475569")
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("PAYEE NAME:", textStartX, textY, paint)
        canvas.drawText("PAYABLE AMOUNT:", textStartX + 170f, textY, paint)

        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(payeeName, textStartX, textY + 13f, paint)

        paint.color = Color.parseColor("#4338CA")
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("$currency${String.format(Locale.US, "%.2f", billAmount)}", textStartX + 170f, textY + 13f, paint)
        textY += 26f

        // Note
        paint.color = Color.parseColor("#475569")
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("PAYMENT NOTE:", textStartX, textY, paint)

        paint.color = Color.parseColor("#334155")
        paint.textSize = 8.5f
        paint.typeface = Typeface.DEFAULT
        val safeNote = if (paymentNote.length > 45) paymentNote.take(43) + "..." else paymentNote
        canvas.drawText(safeNote, textStartX, textY + 12f, paint)
        textY += 24f

        // Supported Apps Banner
        paint.color = Color.parseColor("#E0E7FF")
        canvas.drawRoundRect(RectF(textStartX, textY, 550f, textY + 18f), 4f, 4f, paint)
        paint.color = Color.parseColor("#3730A3")
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("ACCEPTED ON: Google Pay • PhonePe • Paytm • BHIM • Cred • Any UPI App", textStartX + 8f, textY + 12f, paint)

        // Bottom Document Footer
        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawLine(32f, 804f, 563f, 804f, paint)

        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 8f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Official Party Bill & Invoice • Keep for personal and payment settlement records", 32f, 818f, paint)
        val copyText = "Verified Bill Document"
        val copyWidth = paint.measureText(copyText)
        canvas.drawText(copyText, 563f - copyWidth, 818f, paint)

        pdfDocument.finishPage(page)

        // Save to cache dir
        val cacheDir = File(context.cacheDir, "pdf_bills")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        val safeTitle = summary.party.title.replace("[^a-zA-Z0-9]".toRegex(), "_").take(15)
        val targetSuffix = if (isTargetMemberBill) "_${billConfig?.targetMemberName?.replace("[^a-zA-Z0-9]".toRegex(), "_")}" else "_PartyBill"
        val file = File(cacheDir, "Bill_${safeTitle}${targetSuffix}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { outputStream ->
            pdfDocument.writeTo(outputStream)
        }
        pdfDocument.close()

        return file
    }

    private fun drawKpiCard(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        label: String,
        value: String,
        bgColor: Int,
        textColor: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = bgColor
        canvas.drawRoundRect(RectF(x, y, x + width, y + height), 6f, 6f, paint)

        paint.color = Color.parseColor("#64748B")
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(label, x + 8f, y + 15f, paint)

        paint.color = textColor
        paint.textSize = 11.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(value, x + 8f, y + 33f, paint)
    }
}
