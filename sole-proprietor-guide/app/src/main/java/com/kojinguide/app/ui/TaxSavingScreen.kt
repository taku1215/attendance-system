package com.kojinguide.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kojinguide.app.data.ChecklistRepository
import com.kojinguide.app.data.TaxSavingItem
import com.kojinguide.app.data.ToolsData

@Composable
fun TaxSavingScreen(contentPadding: PaddingValues) {
    val context = LocalContext.current
    // チェック状態は ChecklistRepository を流用(キーが衝突しないようprefix付与)
    val prefs = remember { context.getSharedPreferences("tax_saving", android.content.Context.MODE_PRIVATE) }
    val usingState = remember {
        mutableStateMapOf<String, Boolean>().apply {
            ToolsData.taxSavingItems.forEach { put(it.id, prefs.getBoolean(it.id, false)) }
        }
    }

    val items = ToolsData.taxSavingItems
    val notUsing = items.filter { usingState[it.id] != true }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (notUsing.isEmpty())
                            "主な節税策はすべて活用できています!"
                        else
                            "まだ使えていない節税策が ${notUsing.size} 件あります。チェックを入れて管理しましょう。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
        items(items) { item ->
            val using = usingState[item.id] == true
            TaxSavingRow(item, using) { checked ->
                usingState[item.id] = checked
                prefs.edit().putBoolean(item.id, checked).apply()
            }
        }
        item {
            Text(
                "※ 各制度には加入条件・上限・注意点があります。利用前に公式情報をご確認ください。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun TaxSavingRow(item: TaxSavingItem, using: Boolean, onChange: (Boolean) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!using) },
        colors = CardDefaults.cardColors(
            containerColor = if (using) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Checkbox(checked = using, onCheckedChange = onChange)
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    if (using) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "活用中", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Text(item.effect, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(item.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
