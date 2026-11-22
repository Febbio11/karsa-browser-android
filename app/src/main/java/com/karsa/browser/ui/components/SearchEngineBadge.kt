package com.karsa.browser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karsa.browser.browser.SearchEngineConfig
import com.karsa.browser.browser.SearchEngineId

/**
 * Badge mesin pencari (porting SearchEngineBadge.tsx).
 * sm = lingkaran 20.dp, teks 10.sp; md = kotak 28.dp rounded, teks 11.sp.
 * Di Android digambar sebagai komponen kecil (huruf + warna engine),
 * bukan SVG — cukup untuk pengenal engine di pill pencarian & sheet.
 */
@Composable
fun SearchEngineBadge(engine: SearchEngineConfig, md: Boolean = false) {
    val (bg, fg, label) = when (engine.id) {
        SearchEngineId.GOOGLE -> Triple(Color.White, Color(0xFF4285F4), "G")
        SearchEngineId.BRAVE -> Triple(Color(0xFFFB542B).copy(alpha = 0.14f), Color(0xFFD9431F), "Br")
        SearchEngineId.DUCKDUCKGO -> Triple(Color(0xFFDE5833).copy(alpha = 0.14f), Color(0xFFC64724), "DDG")
        SearchEngineId.BING -> Triple(Color(0xFF008373).copy(alpha = 0.14f), Color(0xFF007366), "B")
        SearchEngineId.YANDEX -> Triple(Color(0xFFD93829).copy(alpha = 0.14f), Color(0xFFC22B1E), "Y")
        SearchEngineId.CUSTOM -> Triple(MaterialTheme.colorScheme.surfaceVariant, Color(0xFF8E3C21), "URL")
    }
    val size = if (md) 28.dp else 20.dp
    val shape = if (md) RoundedCornerShape(8.dp) else CircleShape
    Box(
        modifier = Modifier
            .size(size)
            .background(bg, shape)
            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), shape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = if (md) 11.sp else if (label.length > 1) 8.sp else 10.sp,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}
