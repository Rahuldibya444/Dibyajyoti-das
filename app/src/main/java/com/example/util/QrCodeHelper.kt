package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.net.URLEncoder
import java.util.Locale

object QrCodeHelper {

    fun buildUpiUri(
        upiId: String,
        payeeName: String,
        amount: Double? = null,
        note: String = ""
    ): String {
        val cleanUpi = upiId.trim()
        if (cleanUpi.isBlank()) return ""

        val builder = StringBuilder("upi://pay?pa=").append(cleanUpi)

        if (payeeName.isNotBlank()) {
            val encodedName = runCatching { URLEncoder.encode(payeeName.trim(), "UTF-8") }.getOrDefault(payeeName.trim())
            builder.append("&pn=").append(encodedName)
        }

        if (amount != null && amount > 0.009) {
            val amtFormatted = String.format(Locale.US, "%.2f", amount)
            builder.append("&am=").append(amtFormatted)
        }

        builder.append("&cu=INR")

        if (note.isNotBlank()) {
            val encodedNote = runCatching { URLEncoder.encode(note.trim(), "UTF-8") }.getOrDefault(note.trim())
            builder.append("&tn=").append(encodedNote)
        }

        return builder.toString()
    }

    fun generateQrBitmap(
        content: String,
        size: Int = 360
    ): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = hashMapOf<EncodeHintType, Any>(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
            )
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
