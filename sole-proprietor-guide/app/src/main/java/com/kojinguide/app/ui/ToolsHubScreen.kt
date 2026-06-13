package com.kojinguide.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ToolEntry(val route: String, val title: String, val desc: String, val emoji: String)

val toolEntries = listOf(
    ToolEntry("calculator", "計算機", "消費税・源泉徴収をすぐ計算", "🧮"),
    ToolEntry("simulator", "税金シミュレーター", "売上と経費から税負担を試算", "📊"),
    ToolEntry("bookkeeping", "かんたん帳簿", "収入・支出を記録、CSV出力", "📔"),
    ToolEntry("invoice", "請求書メモ", "請求金額の計算とテキスト作成", "📨"),
    ToolEntry("expensefaq", "経費になる?FAQ", "迷いやすい支出を○△×で判定", "🤔"),
    ToolEntry("taxsaving", "節税チェッカー", "使えていない節税策を見える化", "💡"),
    ToolEntry("schedule", "年間スケジュール", "1年の手続き・納税カレンダー", "📅")
)

@Composable
fun ToolsHubScreen(onToolClick: (String) -> Unit, contentPadding: PaddingValues) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(toolEntries) { tool ->
            Card(
                onClick = { onToolClick(tool.route) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(tool.emoji, fontSize = 28.sp)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(tool.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(2.dp))
                        Text(tool.desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
