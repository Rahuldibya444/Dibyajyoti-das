package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.PartyFullSummary
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

object PdfReportExporter {

    fun generateAndSharePdf(context: Context, summary: PartyFullSummary): File? {
        try {
            val file = generatePdfFile(context, summary)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Party Report: ${summary.party.title}")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Here is the final expense summary report for \"${summary.party.title}\" with all member breakdowns and settlement transfers."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Party Summary PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun generatePdfFile(context: Context, summary: PartyFullSummary): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 points
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
        val currency = summary.party.currencySymbol

        // Top Brand Header Banner
        paint.color = Color.parseColor("#1E1B4B")
        canvas.drawRect(0f, 0f, 595f, 90f, paint)

        // Title text
        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("PARTY CALCULATOR REPORT", 32f, 42f, paint)

        paint.textSize = 11f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.parseColor("#C7D2FE")
        canvas.drawText("Official Expense Settlement & Summary Audit", 32f, 62f, paint)

        val reportGeneratedTime = timeFormat.format(Date())
        paint.textSize = 9f
        paint.color = Color.parseColor("#A5B4FC")
        val dateTextWidth = paint.measureText(reportGeneratedTime)
        canvas.drawText(reportGeneratedTime, 595f - 32f - dateTextWidth, 52f, paint)

        var y = 118f

        // Party Info Header
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(summary.party.title, 32f, y, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.parseColor("#64748B")
        val partyDateStr = "Event Date: ${dateFormat.format(Date(summary.party.dateMillis))}"
        canvas.drawText(partyDateStr, 32f, y + 16f, paint)

        if (summary.party.description.isNotBlank()) {
            paint.textSize = 10f
            paint.color = Color.parseColor("#475569")
            canvas.drawText("Note: ${summary.party.description}", 32f, y + 32f, paint)
            y += 18f
        }

        y += 44f

        // KPI Summary Cards
        drawKpiCard(canvas, 32f, y, 120f, 50f, "TOTAL EXPENSE", "$currency${String.format(Locale.US, "%.2f", summary.totalExpense)}", Color.parseColor("#EEF2FF"), Color.parseColor("#4338CA"))
        drawKpiCard(canvas, 162f, y, 120f, 50f, "MEMBERS", "${summary.members.size} Persons", Color.parseColor("#F0FDF4"), Color.parseColor("#15803D"))
        drawKpiCard(canvas, 292f, y, 130f, 50f, "PER PERSON AVG", "$currency${String.format(Locale.US, "%.2f", summary.averagePerPerson)}", Color.parseColor("#FFF7ED"), Color.parseColor("#C2410C"))
        drawKpiCard(canvas, 432f, y, 131f, 50f, "TRANSACTIONS", "${summary.expenses.size} Items", Color.parseColor("#FAF5FF"), Color.parseColor("#7E22CE"))

        y += 72f

        // Section: Member Balances Table
        paint.color = Color.parseColor("#1E293B")
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("MEMBER EXPENSE & BALANCE AUDIT", 32f, y, paint)
        y += 14f

        // Table Header
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRoundRect(RectF(32f, y, 563f, y + 22f), 4f, 4f, paint)

        paint.color = Color.parseColor("#475569")
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("MEMBER", 42f, y + 15f, paint)
        canvas.drawText("PAID", 200f, y + 15f, paint)
        canvas.drawText("SHARE", 290f, y + 15f, paint)
        canvas.drawText("NET BALANCE", 390f, y + 15f, paint)
        canvas.drawText("SETTLEMENT STATUS", 470f, y + 15f, paint)
        y += 24f

        // Table Rows
        summary.memberSummaries.forEachIndexed { index, mSummary ->
            if (index % 2 == 1) {
                paint.color = Color.parseColor("#F8FAFC")
                canvas.drawRect(32f, y, 563f, y + 20f, paint)
            }

            paint.color = Color.parseColor("#1E293B")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 9.5f
            canvas.drawText(mSummary.member.name, 42f, y + 14f, paint)
            canvas.drawText("$currency${String.format(Locale.US, "%.2f", mSummary.totalPaid)}", 200f, y + 14f, paint)
            canvas.drawText("$currency${String.format(Locale.US, "%.2f", mSummary.totalShare)}", 290f, y + 14f, paint)

            val net = mSummary.netBalance
            val netStr = if (net >= 0) "+$currency${String.format(Locale.US, "%.2f", net)}" else "-$currency${String.format(Locale.US, "%.2f", -net)}"
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            if (net > 0.01) {
                paint.color = Color.parseColor("#15803D") // Green
                canvas.drawText(netStr, 390f, y + 14f, paint)
                canvas.drawText("Gets Back", 470f, y + 14f, paint)
            } else if (net < -0.01) {
                paint.color = Color.parseColor("#DC2626") // Red
                canvas.drawText(netStr, 390f, y + 14f, paint)
                canvas.drawText("Owes Money", 470f, y + 14f, paint)
            } else {
                paint.color = Color.parseColor("#64748B") // Gray
                canvas.drawText("$currency 0.00", 390f, y + 14f, paint)
                canvas.drawText("Settled Up", 470f, y + 14f, paint)
            }

            y += 20f
        }

        y += 18f

        // Section: Recommended Settlements (Who Pays Whom)
        paint.color = Color.parseColor("#1E293B")
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SMART SETTLEMENT TRANSFERS (MINIMIZED DEBT)", 32f, y, paint)
        y += 14f

        if (summary.settlements.isEmpty()) {
            paint.color = Color.parseColor("#15803D")
            paint.textSize = 10f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("✓ Everyone is fully settled! No payments pending.", 32f, y + 12f, paint)
            y += 24f
        } else {
            summary.settlements.forEach { transfer ->
                paint.color = Color.parseColor("#F8FAFC")
                canvas.drawRoundRect(RectF(32f, y, 563f, y + 26f), 4f, 4f, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f
                paint.color = Color.parseColor("#E2E8F0")
                canvas.drawRoundRect(RectF(32f, y, 563f, y + 26f), 4f, 4f, paint)
                paint.style = Paint.Style.FILL

                paint.color = Color.parseColor("#DC2626")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 10f
                canvas.drawText(transfer.fromMemberName, 44f, y + 17f, paint)

                paint.color = Color.parseColor("#64748B")
                paint.typeface = Typeface.DEFAULT
                canvas.drawText("pays", 140f, y + 17f, paint)

                paint.color = Color.parseColor("#15803D")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(transfer.toMemberName, 175f, y + 17f, paint)

                val amountStr = "$currency${String.format(Locale.US, "%.2f", transfer.amount)}"
                paint.color = Color.parseColor("#4338CA")
                val amtWidth = paint.measureText(amountStr)
                canvas.drawText(amountStr, 550f - amtWidth, y + 17f, paint)

                y += 30f
            }
        }

        y += 14f

        // Section: Itemized Expenses (up to space available)
        paint.color = Color.parseColor("#1E293B")
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("ITEMIZED EXPENSES", 32f, y, paint)
        y += 14f

        // Table header
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRoundRect(RectF(32f, y, 563f, y + 20f), 4f, 4f, paint)
        paint.color = Color.parseColor("#475569")
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("ITEM / DESCRIPTION", 42f, y + 14f, paint)
        canvas.drawText("CATEGORY", 230f, y + 14f, paint)
        canvas.drawText("PAID BY", 340f, y + 14f, paint)
        canvas.drawText("AMOUNT", 490f, y + 14f, paint)
        y += 22f

        val displayExpenses = summary.expenses.take(8)
        displayExpenses.forEachIndexed { idx, exp ->
            if (idx % 2 == 1) {
                paint.color = Color.parseColor("#F8FAFC")
                canvas.drawRect(32f, y, 563f, y + 18f, paint)
            }
            paint.color = Color.parseColor("#1E293B")
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 9f

            val itemTitle = if (exp.title.length > 28) exp.title.take(26) + "..." else exp.title
            canvas.drawText(itemTitle, 42f, y + 13f, paint)
            canvas.drawText(exp.category, 230f, y + 13f, paint)
            canvas.drawText(exp.payerName, 340f, y + 13f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val amtText = "$currency${String.format(Locale.US, "%.2f", exp.amount)}"
            val amtWidth = paint.measureText(amtText)
            canvas.drawText(amtText, 550f - amtWidth, y + 13f, paint)
            y += 18f
        }

        if (summary.expenses.size > 8) {
            paint.color = Color.parseColor("#64748B")
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas.drawText("... and ${summary.expenses.size - 8} more transactions itemized in app database.", 42f, y + 12f, paint)
            y += 18f
        }

        // Footer
        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawLine(32f, 800f, 563f, 800f, paint)

        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 8.5f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Report automatically generated by Party Calculator • Keep for personal and group records", 32f, 816f, paint)
        val copyText = "Offline Local-First Verified"
        val copyWidth = paint.measureText(copyText)
        canvas.drawText(copyText, 563f - copyWidth, 816f, paint)

        pdfDocument.finishPage(page)

        // Save to cache dir
        val cacheDir = File(context.cacheDir, "pdf_reports")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        val safeTitle = summary.party.title.replace("[^a-zA-Z0-9]".toRegex(), "_").take(20)
        val file = File(cacheDir, "PartyReport_${safeTitle}_${System.currentTimeMillis()}.pdf")
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
        canvas.drawText(label, x + 10f, y + 18f, paint)

        paint.color = textColor
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(value, x + 10f, y + 38f, paint)
    }
}
