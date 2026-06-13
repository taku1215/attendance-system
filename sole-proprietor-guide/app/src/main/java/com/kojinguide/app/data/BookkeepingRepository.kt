package com.kojinguide.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** 簡易帳簿の1取引 */
data class BookEntry(
    val id: Long,
    val date: String,        // yyyy-MM-dd
    val isIncome: Boolean,   // true:収入 / false:支出
    val category: String,
    val amount: Long,
    val memo: String
)

/** 月ごとの集計 */
data class MonthlySummary(
    val yearMonth: String,   // yyyy-MM
    val income: Long,
    val expense: Long
) {
    val balance: Long get() = income - expense
}

/** 簡易帳簿を SharedPreferences に JSON で保存する */
class BookkeepingRepository(context: Context) {

    private val prefs = context.getSharedPreferences("bookkeeping", Context.MODE_PRIVATE)

    fun loadAll(): List<BookEntry> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        val array = JSONArray(raw)
        val list = ArrayList<BookEntry>(array.length())
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            list.add(
                BookEntry(
                    id = o.getLong("id"),
                    date = o.getString("date"),
                    isIncome = o.getBoolean("isIncome"),
                    category = o.getString("category"),
                    amount = o.getLong("amount"),
                    memo = o.optString("memo", "")
                )
            )
        }
        return list.sortedByDescending { it.date + it.id.toString() }
    }

    fun add(entry: BookEntry) {
        val list = loadAll().toMutableList()
        list.add(entry)
        save(list)
    }

    fun delete(id: Long) {
        save(loadAll().filterNot { it.id == id })
    }

    private fun save(list: List<BookEntry>) {
        val array = JSONArray()
        list.forEach { e ->
            array.put(
                JSONObject().apply {
                    put("id", e.id)
                    put("date", e.date)
                    put("isIncome", e.isIncome)
                    put("category", e.category)
                    put("amount", e.amount)
                    put("memo", e.memo)
                }
            )
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    fun monthlySummaries(): List<MonthlySummary> {
        return loadAll()
            .groupBy { it.date.take(7) } // yyyy-MM
            .map { (ym, entries) ->
                MonthlySummary(
                    yearMonth = ym,
                    income = entries.filter { it.isIncome }.sumOf { it.amount },
                    expense = entries.filterNot { it.isIncome }.sumOf { it.amount }
                )
            }
            .sortedByDescending { it.yearMonth }
    }

    /** CSV文字列を生成(Excel等で開ける) */
    fun toCsv(): String {
        val sb = StringBuilder()
        sb.append("日付,区分,科目,金額,メモ\n")
        loadAll()
            .sortedBy { it.date }
            .forEach { e ->
                val type = if (e.isIncome) "収入" else "支出"
                val memo = e.memo.replace("\"", "\"\"")
                sb.append("${e.date},$type,${e.category},${e.amount},\"$memo\"\n")
            }
        return sb.toString()
    }

    companion object {
        private const val KEY = "entries"

        val incomeCategories = listOf("売上", "雑収入", "その他")
        val expenseCategories = listOf(
            "仕入", "外注費", "通信費", "旅費交通費", "消耗品費",
            "接待交際費", "地代家賃", "水道光熱費", "広告宣伝費",
            "新聞図書費", "支払手数料", "租税公課", "その他"
        )
    }
}
