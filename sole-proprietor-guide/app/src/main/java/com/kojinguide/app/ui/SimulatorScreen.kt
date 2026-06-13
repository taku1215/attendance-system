package com.kojinguide.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.kojinguide.app.data.TaxCalc
import com.kojinguide.app.util.Format

@Composable
fun SimulatorScreen(contentPadding: PaddingValues) {
    var revenue by remember { mutableStateOf("") }
    var expenses by remember { mutableStateOf("") }
    var blueDeduction by remember { mutableStateOf(650_000L) }
    var social by remember { mutableStateOf("") }
    var other by remember { mutableStateOf("") }
    var businessTax by remember { mutableStateOf(true) }

    val input = TaxCalc.SimulatorInput(
        revenue = Format.parseAmount(revenue),
        expenses = Format.parseAmount(expenses),
        blueDeduction = blueDeduction,
        socialInsurance = Format.parseAmount(social),
        otherDeduction = Format.parseAmount(other),
        applyBusinessTax = businessTax
    )
    val result = TaxCalc.simulate(input)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding() + 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "1年間のおおよその税負担を試算します。金額を入力してください。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        NumberField(revenue, { revenue = it }, "年間の売上(円)")
        NumberField(expenses, { expenses = it }, "年間の経費(円)")

        Text("青色申告特別控除", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BlueChip("65万", 650_000L, blueDeduction) { blueDeduction = it }
            BlueChip("55万", 550_000L, blueDeduction) { blueDeduction = it }
            BlueChip("10万", 100_000L, blueDeduction) { blueDeduction = it }
            BlueChip("なし", 0L, blueDeduction) { blueDeduction = it }
        }

        NumberField(social, { social = it }, "社会保険料(国民年金+国保 等)")
        NumberField(other, { other = it }, "その他控除(iDeCo・小規模共済 等)")

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = businessTax, onCheckedChange = { businessTax = it })
            Spacer(Modifier.height(0.dp))
            Text(
                "  個人事業税の対象業種として計算する",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("試算結果", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.height(8.dp))
                Line("事業所得(売上−経費)", Format.yen(result.businessIncome))
                Line("課税所得(所得税)", Format.yen(result.taxableForIncomeTax))
                Divider(Modifier.padding(vertical = 8.dp))
                Line("所得税(復興税込み)", Format.yen(result.incomeTax))
                Line("住民税(概算)", Format.yen(result.residentTax))
                Line("個人事業税(概算)", Format.yen(result.businessTax))
                Divider(Modifier.padding(vertical = 8.dp))
                Line("税金の合計", Format.yen(result.totalTax), big = true)
                Line("実効税率(対 事業所得)", String.format("%.1f%%", result.effectiveRatePercent))
                Line("手取り目安(税引後)", Format.yen(result.takeHome), big = true)
            }
        }

        Text(
            "※ 基礎控除48万円(住民税は43万円)等を用いた概算です。国民健康保険料・国民年金は『社会保険料』の入力額として扱い、別途は計算しません。住民税は所得割10%+均等割の概算、個人事業税は事業主控除290万円・税率5%で計算しています。正確な金額は会計ソフトや税理士、自治体にご確認ください。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
}

@Composable
private fun BlueChip(label: String, value: Long, selected: Long, onSelect: (Long) -> Unit) {
    FilterChip(selected = selected == value, onClick = { onSelect(value) }, label = { Text(label) })
}

@Composable
private fun Line(label: String, value: String, big: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(
            value,
            style = if (big) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.End
        )
    }
}
