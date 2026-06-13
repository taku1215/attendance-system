package com.kojinguide.app.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.core.content.FileProvider
import com.kojinguide.app.data.BookEntry
import com.kojinguide.app.data.BookkeepingRepository
import com.kojinguide.app.util.Format
import java.io.File
import java.time.LocalDate

@Composable
fun BookkeepingScreen(contentPadding: PaddingValues) {
    val context = LocalContext.current
    val repo = remember { BookkeepingRepository(context) }
    var entries by remember { mutableStateOf(repo.loadAll()) }
    var showAdd by remember { mutableStateOf(false) }

    fun reload() { entries = repo.loadAll() }

    val summaries = repo.monthlySummaries()
    val totalIncome = entries.filter { it.isIncome }.sumOf { it.amount }
    val totalExpense = entries.filterNot { it.isIncome }.sumOf { it.amount }

    androidx.compose.material3.Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = "取引を追加")
            }
        }
    ) { _ ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = contentPadding.calculateTopPadding() + 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 88.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("合計", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.height(6.dp))
                        SummaryLine("収入", totalIncome, incomeColor)
                        SummaryLine("支出", totalExpense, expenseColor)
                        SummaryLine("差引(利益)", totalIncome - totalExpense, MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            if (entries.isNotEmpty()) {
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("取引一覧", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.primary)
                        TextButton(onClick = { exportCsv(context, repo) }) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("CSV出力")
                        }
                    }
                }
            }

            if (summaries.isNotEmpty()) {
                items(summaries) { s ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(s.yearMonth.replace("-", "年") + "月", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("収 ${Format.yen(s.income)} / 支 ${Format.yen(s.expense)}", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            items(entries) { entry ->
                EntryRow(entry) {
                    repo.delete(entry.id)
                    reload()
                }
            }

            if (entries.isEmpty()) {
                item {
                    Text(
                        "右下の＋ボタンから収入・支出を記録できます。記録したデータはCSVで書き出して会計ソフトに取り込めます。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }
    }

    if (showAdd) {
        AddEntryDialog(
            onDismiss = { showAdd = false },
            onConfirm = { entry ->
                repo.add(entry)
                reload()
                showAdd = false
            }
        )
    }
}

private val incomeColor = Color(0xFF2E7D32)
private val expenseColor = Color(0xFFC62828)

@Composable
private fun SummaryLine(label: String, value: Long, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(Format.yen(value), fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun EntryRow(entry: BookEntry, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${entry.date}  ${entry.category}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (entry.memo.isNotBlank()) {
                    Text(entry.memo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                (if (entry.isIncome) "+" else "−") + Format.yen(entry.amount),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (entry.isIncome) incomeColor else expenseColor
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "削除", tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEntryDialog(onDismiss: () -> Unit, onConfirm: (BookEntry) -> Unit) {
    var isIncome by remember { mutableStateOf(false) }
    var amountText by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }
    val categories = if (isIncome) BookkeepingRepository.incomeCategories else BookkeepingRepository.expenseCategories
    var category by remember { mutableStateOf(categories.first()) }
    var expanded by remember { mutableStateOf(false) }

    // 区分を切り替えたら科目を先頭に戻す
    if (category !in categories) category = categories.first()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = Format.parseAmount(amountText)
                    if (amount > 0) {
                        onConfirm(
                            BookEntry(
                                id = System.currentTimeMillis(),
                                date = LocalDate.now().toString(),
                                isIncome = isIncome,
                                category = category,
                                amount = amount,
                                memo = memo
                            )
                        )
                    }
                }
            ) { Text("追加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } },
        title = { Text("取引を追加") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !isIncome,
                        onClick = { isIncome = false },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) { Text("支出") }
                    SegmentedButton(
                        selected = isIncome,
                        onClick = { isIncome = true },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) { Text("収入") }
                }

                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("科目") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        categories.forEach { c ->
                            DropdownMenuItem(text = { Text(c) }, onClick = { category = c; expanded = false })
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("金額(円)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("メモ(任意)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

private fun exportCsv(context: Context, repo: BookkeepingRepository) {
    try {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "kakeibo_${LocalDate.now()}.csv")
        // ExcelでUTF-8を正しく開くためBOMを付与
        file.writeText("﻿" + repo.toCsv())
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "CSVを共有"))
    } catch (e: Exception) {
        Toast.makeText(context, "出力に失敗しました: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
