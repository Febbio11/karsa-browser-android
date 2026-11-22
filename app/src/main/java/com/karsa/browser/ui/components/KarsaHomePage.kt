package com.karsa.browser.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karsa.browser.browser.HomeShortcut
import com.karsa.browser.browser.SearchEngineConfig
import com.karsa.browser.browser.shortcutBadgeFor
import com.karsa.browser.browser.shortcutLabelFor
import com.karsa.browser.ui.theme.KarsaSerif

/**
 * Home orisinal Karsa (porting KarsaHomePage.tsx):
 * wordmark serif "Karsa", subtitle Mode Mobile, pill pencarian (badge engine),
 * grid pintasan 4 kolom + tile Tambah + form tambah inline.
 */
@Composable
fun KarsaHomePage(
    shortcuts: List<HomeShortcut>,
    activeEngine: SearchEngineConfig,
    onOpenSearchFocus: (openEnginePicker: Boolean) -> Unit,
    onNavigateShortcut: (String, String) -> Unit,
    onAddShortcut: (String, String) -> Unit,
    onRemoveShortcut: (String) -> Unit
) {
    var adding by remember { mutableStateOf(false) }
    var editMode by remember { mutableStateOf(false) }
    var labelInput by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf("") }

    fun saveShortcut() {
        val cleanUrl = urlInput.trim()
        if (cleanUrl.isEmpty()) return
        val normalized = if (cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) cleanUrl else "https://$cleanUrl"
        val label = labelInput.trim().ifEmpty { shortcutLabelFor(normalized) }
        onAddShortcut(label, normalized)
        labelInput = ""; urlInput = ""; adding = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))
        // Wordmark orisinal Karsa (serif editorial).
        Text(
            text = "Karsa",
            fontFamily = KarsaSerif,
            fontSize = 36.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Mode Mobile · Cepat, tenang & bebas gangguan",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(modifier = Modifier.height(28.dp))
        // Pill pencarian (trigger -> focus mode).
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .clickable { onOpenSearchFocus(false) },
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(start = 8.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onOpenSearchFocus(true) }, modifier = Modifier.size(32.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SearchEngineBadge(engine = activeEngine)
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = "Ganti mesin pencari",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "Telusuri ${activeEngine.name} atau tempel URL...",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).clickable { onOpenSearchFocus(false) }
                )
            }
        }
        Spacer(modifier = Modifier.height(28.dp))
        // Pintasan Cepat.
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pintasan Cepat", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (shortcuts.isNotEmpty()) {
                    Row(
                        modifier = Modifier.clickable { editMode = !editMode }.padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (editMode) Icons.Rounded.Check else Icons.Rounded.Edit,
                            contentDescription = if (editMode) "Selesai" else "Atur",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (editMode) "Selesai" else "Atur", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxWidth().height(170.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                userScrollEnabled = false
            ) {
                items(shortcuts, key = { it.id }) { item ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box {
                            Surface(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { if (!editMode) onNavigateShortcut(item.url, item.label) },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                tonalElevation = 1.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(item.badge, fontFamily = KarsaSerif, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            if (editMode) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(20.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.onBackground)
                                        .clickable { onRemoveShortcut(item.id) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Hapus pintasan ${item.label}", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.background)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(item.label, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .clickable { editMode = false; adding = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Tambah pintasan", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Tambah", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (adding) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    tonalElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Language, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tambah Pintasan Cepat", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            IconButton(onClick = { adding = false }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Rounded.Close, contentDescription = "Tutup", modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = labelInput, onValueChange = { labelInput = it },
                            placeholder = { Text("Nama situs (opsional, misal: GitHub)", fontSize = 12.sp) },
                            singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = urlInput, onValueChange = { urlInput = it },
                            placeholder = { Text("URL situs (misal: github.com)", fontSize = 12.sp) },
                            singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            OutlinedButton(onClick = { adding = false }) { Text("Batal", fontSize = 12.sp) }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { saveShortcut() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.onBackground,
                                    contentColor = MaterialTheme.colorScheme.background
                                )
                            ) { Text("Simpan", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                        }
                        // Badge dihitung otomatis dari label saat simpan (2 huruf pertama).
                        Text("Badge: ${shortcutBadgeFor(labelInput.ifEmpty { shortcutLabelFor(urlInput.ifEmpty { "x" }) })}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}
