package com.kojinguide.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.kojinguide.app.data.InvoiceProfile
import com.kojinguide.app.data.InvoiceRepository
import com.kojinguide.app.data.TaxCalc
import com.kojinguide.app.util.Format
import java.time.LocalDate

@Composable
fun InvoiceScreen(contentPadding: PaddingValues) {
    val context = LocalContext.current
    val repo = remember { InvoiceRepository(context) }
    val saved = remember { repo.loadProfile() }

    var myName by remember { mutableStateOf(saved.name) }
    var myAddress by remember { mutableStateOf(saved.address) }
    var bank by remember { mutableStateOf(saved.bank) }
    var regNo by remember { mutableStateOf(saved.registrationNumber) }

    var client by remember { mutableStateOf("") }
    var item by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf(10) }
    var withhold by remember { mutableStateOf(false) }

    val net = Format.parseAmount(amountText)
    val tax = TaxCalc.consumptionTax(net, rate)
    val gross = net + tax
    val withholding = if (withhold) TaxCalc.withholdingTax(net) else 0L
    val payable = gross - withholding

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp, end = 16.dp,
                top = contentPadding.calculateTopPadding() + 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp
            ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionLabel("自分の情報(保存して再利用できます)")
        Field(myName, { myName = it }, "氏名・屋号")
        Field(myAddress, { myAddress = it }, "住所・連絡先")
        Field(bank, { bank = it }, "振込先口座")
        Field(regNo, { regNo = it }, "インボイス登録番号(T+13桁・任意)")
        OutlinedButton(
            onClick = {
                repo.saveProfile(InvoiceProfile(myName, myAddress, bank, regNo))
                Toast.makeText(context, "自分の情報を保存しました", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("自分の情報を保存") }

        Divider(Modifier.padding(vertical = 4.dp))
        SectionLabel("請求内容")
        Field(client, { client = it }, "請求先(取引先名)")
        Field(item, { item = it }, "品目・摘要")
        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("金額(税抜・円)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(rate == 10, { rate = 10 }, { Text("消費税10%") })
            FilterChip(rate == 8, { rate = 8 }, { Text("8%") })
            FilterChip(withhold, { withhold = !withhold }, { Text("源泉徴収あり") })
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.padding(16.dp)) {
                Amount("小計(税抜)", net)
                Amount("消費税($rate%)", tax)
                if (withhold) Amount("源泉徴収", -withholding)
                Divider(Modifier.padding(vertical = 6.dp))
                Amount("ご請求額", payable, big = true)
            }
        }

        Button(
            onClick = {
                val text = buildInvoiceText(
                    myName, myAddress, bank, regNo, client, item,
                    net, tax, gross, rate, withhold, withholding, payable
                )
                copyToClipboard(context, text)
                Toast.makeText(context, "請求書テキストをコピーしました", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("請求書テキストを作成してコピー") }

        Text(
            "コピーしたテキストをメールやメモアプリに貼り付けて使えます。インボイス対応の請求書には登録番号・税率ごとの金額の記載が必要です。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun buildInvoiceText(
    name: String, address: String, bank: String, regNo: String,
    client: String, item: String,
    net: Long, tax: Long, gross: Long, rate: Int,
    withhold: Boolean, withholding: Long, payable: Long
): String {
    val today = LocalDate.now()
    val sb = StringBuilder()
    sb.append("請求書\n")
    sb.append("発行日: ${today.year}年${today.monthValue}月${today.dayOfMonth}日\n\n")
    if (client.isNotBlank()) sb.append("$client 御中\n\n")
    sb.append("下記のとおりご請求申し上げます。\n\n")
    if (item.isNotBlank()) sb.append("品目: $item\n")
    sb.append("小計(税抜): ${Format.yen(net)}\n")
    sb.append("消費税($rate%): ${Format.yen(tax)}\n")
    sb.append("税込金額: ${Format.yen(gross)}\n")
    if (withhold) {
        sb.append("源泉徴収税額: -${Format.yen(withholding)}\n")
    }
    sb.append("ご請求額: ${Format.yen(payable)}\n\n")
    sb.append("―――――――――――\n")
    if (name.isNotBlank()) sb.append("$name\n")
    if (address.isNotBlank()) sb.append("$address\n")
    if (regNo.isNotBlank()) sb.append("登録番号: $regNo\n")
    if (bank.isNotBlank()) sb.append("お振込先: $bank\n")
    return sb.toString()
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("invoice", text))
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true
    )
}

@Composable
private fun Amount(label: String, value: Long, big: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(
            Format.yen(value),
            style = if (big) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
