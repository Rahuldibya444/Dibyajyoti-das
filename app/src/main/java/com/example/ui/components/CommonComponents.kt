package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SyncStatus

@Composable
fun MemberAvatar(
    name: String,
    modifier: Modifier = Modifier,
    colorHex: String = "#D0BCFF",
    size: Int = 40
) {
    val cleanName = name.trim()
    val initial = if (cleanName.contains(" ")) {
        val parts = cleanName.split(" ").filter { it.isNotBlank() }
        if (parts.size >= 2) "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
        else cleanName.take(2).uppercase()
    } else {
        cleanName.take(2).uppercase()
    }.ifEmpty { "?" }

    val (bg, textColor) = when (colorHex.uppercase()) {
        "#D0BCFF", "#818CF8", "#4F46E5" -> Pair(Color(0xFFD0BCFF), Color(0xFF381E72))
        "#EFB8C8", "#F97316", "#EF4444" -> Pair(Color(0xFFEFB8C8), Color(0xFF492532))
        "#CCC2DC", "#10B981" -> Pair(Color(0xFFCCC2DC), Color(0xFF332D41))
        else -> {
            val parsed = runCatching { Color(android.graphics.Color.parseColor(colorHex)) }.getOrDefault(Color(0xFFD0BCFF))
            Pair(parsed, Color(0xFF381E72))
        }
    }

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = (size * 0.36).sp
        )
    }
}

@Composable
fun SyncStatusChip(
    status: SyncStatus = SyncStatus.OFFLINE_READY,
    isOffline: Boolean = true,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Receipt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = " Local Storage",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "food", "bbq", "groceries", "dining" -> Icons.Default.Fastfood
        "drinks", "alcohol", "beverages", "bar" -> Icons.Default.LocalBar
        "music", "dj", "sound", "lights", "entertainment" -> Icons.Default.MusicNote
        "venue", "room", "rental", "supplies" -> Icons.Default.Store
        else -> Icons.Default.Receipt
    }
}

data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
