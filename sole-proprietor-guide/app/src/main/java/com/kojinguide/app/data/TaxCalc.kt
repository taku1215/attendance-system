package com.kojinguide.app.data

import kotlin.math.roundToLong

/** 税金の計算ロジック(概算)。最新の税制は国税庁等で確認すること。 */
object TaxCalc {

    /** 税抜金額から消費税額を求める */
    fun consumptionTax(netAmount: Long, ratePercent: Int): Long =
        (netAmount * ratePercent / 100.0).roundToLong()

    /** 税込金額に変換 */
    fun toGross(netAmount: Long, ratePercent: Int): Long =
        netAmount + consumptionTax(netAmount, ratePercent)

    /** 税込金額から税抜を逆算 */
    fun toNet(grossAmount: Long, ratePercent: Int): Long =
        (grossAmount / (1.0 + ratePercent / 100.0)).roundToLong()

    /**
     * 源泉徴収額(所得税+復興特別所得税)。
     * 100万円以下:支払額 × 10.21%
     * 100万円超 :100万円までは10.21%、超えた部分は20.42%
     */
    fun withholdingTax(payment: Long): Long {
        val tax = if (payment <= 1_000_000) {
            payment * 0.1021
        } else {
            1_000_000 * 0.1021 + (payment - 1_000_000) * 0.2042
        }
        return tax.roundToLong()
    }

    /** 源泉徴収後の手取り */
    fun afterWithholding(payment: Long): Long = payment - withholdingTax(payment)

    /**
     * 所得税の速算(課税所得 → 所得税額。復興特別所得税は含まない)
     * 2026年時点の超過累進税率。
     */
    fun incomeTax(taxableIncome: Long): Long {
        val t = taxableIncome.coerceAtLeast(0)
        val tax = when {
            t <= 1_950_000 -> t * 0.05
            t <= 3_300_000 -> t * 0.10 - 97_500
            t <= 6_950_000 -> t * 0.20 - 427_500
            t <= 9_000_000 -> t * 0.23 - 636_000
            t <= 18_000_000 -> t * 0.33 - 1_536_000
            t <= 40_000_000 -> t * 0.40 - 2_796_000
            else -> t * 0.45 - 4_796_000
        }
        return tax.coerceAtLeast(0.0).roundToLong()
    }

    /** 復興特別所得税込みの所得税(所得税額 × 2.1%上乗せ) */
    fun incomeTaxWithSurtax(taxableIncome: Long): Long =
        (incomeTax(taxableIncome) * 1.021).roundToLong()

    data class SimulatorInput(
        val revenue: Long,            // 売上
        val expenses: Long,           // 経費
        val blueDeduction: Long,      // 青色申告特別控除(65/55/10/0万)
        val socialInsurance: Long,    // 社会保険料控除(国民年金・国保など)
        val otherDeduction: Long,     // その他控除(iDeCo・小規模共済など)
        val applyBusinessTax: Boolean // 個人事業税の対象業種か
    )

    data class SimulatorResult(
        val businessIncome: Long,     // 事業所得(売上−経費)
        val taxableForIncomeTax: Long,
        val incomeTax: Long,          // 所得税(復興税込み)
        val residentTax: Long,        // 住民税(概算)
        val businessTax: Long,        // 個人事業税(概算)
        val totalTax: Long,
        val takeHome: Long            // 事業所得から税金を引いた目安(社保等は別)
    ) {
        val effectiveRatePercent: Double
            get() = if (businessIncome > 0) totalTax * 100.0 / businessIncome else 0.0
    }

    // 基礎控除(概算)
    private const val BASIC_DEDUCTION_INCOME = 480_000L  // 所得税
    private const val BASIC_DEDUCTION_RESIDENT = 430_000L // 住民税
    private const val RESIDENT_RATE = 0.10               // 住民税所得割
    private const val RESIDENT_PER_CAPITA = 5_000L       // 住民税均等割(概算)
    private const val BUSINESS_TAX_DEDUCTION = 2_900_000L // 事業主控除
    private const val BUSINESS_TAX_RATE = 0.05           // 個人事業税(業種により3〜5%)

    fun simulate(input: SimulatorInput): SimulatorResult {
        val businessIncome = (input.revenue - input.expenses).coerceAtLeast(0)
        val commonDeductions =
            input.blueDeduction + input.socialInsurance + input.otherDeduction

        val taxableIncomeTax =
            (businessIncome - commonDeductions - BASIC_DEDUCTION_INCOME).coerceAtLeast(0)
        val incomeTax = incomeTaxWithSurtax(taxableIncomeTax)

        val taxableResident =
            (businessIncome - commonDeductions - BASIC_DEDUCTION_RESIDENT).coerceAtLeast(0)
        val residentTax = if (taxableResident > 0) {
            (taxableResident * RESIDENT_RATE).roundToLong() + RESIDENT_PER_CAPITA
        } else 0L

        val businessTax = if (input.applyBusinessTax) {
            ((businessIncome - BUSINESS_TAX_DEDUCTION).coerceAtLeast(0) * BUSINESS_TAX_RATE)
                .roundToLong()
        } else 0L

        val total = incomeTax + residentTax + businessTax
        return SimulatorResult(
            businessIncome = businessIncome,
            taxableForIncomeTax = taxableIncomeTax,
            incomeTax = incomeTax,
            residentTax = residentTax,
            businessTax = businessTax,
            totalTax = total,
            takeHome = businessIncome - total
        )
    }
}
