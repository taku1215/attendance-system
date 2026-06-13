package com.kojinguide.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kojinguide.app.data.ExpenseFaq
import com.kojinguide.app.data.ToolsData

@Composable
fun ExpenseFaqScreen(contentPadding: PaddingValues) {
    var query by remember { mutableStateOf("") }
    val faqs = if (query.isBlank()) ToolsData.expenseFaqs
    else ToolsData.expenseFaqs.filter { it.question.contains(query) || it.explanation.contains(query) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding())
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("キーワードで検索(例:スマホ)") },
            singleLine = true
        )
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Legend("○", "可", colorOk)
            Legend("△", "按分", colorPartial)
            Legend("×", "不可", colorNg)
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 4.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(faqs) { faq -> FaqCard(faq) }
        }
    }
}

private val colorOk = Color(0xFF2E7D32)
private val colorPartial = Color(0xFFE65100)
private val colorNg = Color(0xFFC62828)

private fun colorFor(v: ExpenseFaq.Verdict) = when (v) {
    ExpenseFaq.Verdict.OK -> colorOk
    ExpenseFaq.Verdict.PARTIAL -> colorPartial
    ExpenseFaq.Verdict.NG -> colorNg
}

@Composable
private fun Legend(mark: String, label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(mark, color = color, fontWeight = FontWeight.Bold)
        Text(" $label", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FaqCard(faq: ExpenseFaq) {
    val color = colorFor(faq.verdict)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color),
                    contentAlignment = Alignment.Center
                ) {
                    Text(faq.verdict.mark, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Spacer(Modifier.height(0.dp))
                Text(
                    "  ${faq.question}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(faq.verdict.label, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(faq.explanation, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
