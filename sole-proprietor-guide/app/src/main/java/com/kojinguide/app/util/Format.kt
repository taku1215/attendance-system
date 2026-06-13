package com.kojinguide.app.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

object Format {
    private val yen = NumberFormat.getNumberInstance(Locale.JAPAN)

    fun yen(value: Long): String = "¥" + yen.format(value)

    /** "1,234" のようなカンマ区切り(円記号なし) */
    fun number(value: Long): String = yen.format(value)

    /** 入力文字列を数値に(カンマ・全角・空白を許容) */
    fun parseAmount(text: String): Long {
        val normalized = text
            .replace(",", "")
            .replace("，", "")
            .replace(" ", "")
            .replace("　", "")
            .map { c -> if (c in '０'..'９') ('0' + (c - '０')) else c }
            .joinToString("")
        return normalized.toLongOrNull() ?: 0L
    }
}

object DeadlineUtil {

    /** 次回の確定申告期限(3/15)までの日数。3/15当日は0、過ぎたら翌年の3/15まで */
    fun daysUntilTaxFiling(today: LocalDate = LocalDate.now()): Long {
        var deadline = LocalDate.of(today.year, 3, 15)
        if (today.isAfter(deadline)) {
            deadline = LocalDate.of(today.year + 1, 3, 15)
        }
        return ChronoUnit.DAYS.between(today, deadline)
    }

    fun nextDeadlineYear(today: LocalDate = LocalDate.now()): Int {
        val deadline = LocalDate.of(today.year, 3, 15)
        return if (today.isAfter(deadline)) today.year + 1 else today.year
    }
}
