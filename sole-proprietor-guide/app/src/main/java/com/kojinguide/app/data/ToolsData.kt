package com.kojinguide.app.data

/** 経費FAQの1問 */
data class ExpenseFaq(
    val question: String,
    val verdict: Verdict,
    val explanation: String
) {
    enum class Verdict(val label: String, val mark: String) {
        OK("経費にできる", "○"),
        PARTIAL("按分すれば可", "△"),
        NG("経費にできない", "×")
    }
}

/** 節税チェッカーの1項目 */
data class TaxSavingItem(
    val id: String,
    val title: String,
    val effect: String,
    val detail: String
)

/** 年間スケジュールの1ヶ月分 */
data class ScheduleMonth(
    val month: Int,
    val tasks: List<String>
)

object ToolsData {

    val expenseFaqs: List<ExpenseFaq> = listOf(
        ExpenseFaq(
            "カフェでPC作業したときの飲食代",
            ExpenseFaq.Verdict.PARTIAL,
            "打ち合わせや取引先との会食なら会議費・接待交際費として経費にできます。1人での作業の飲食代は原則として経費になりませんが、場所代として認められる余地もあるためグレー。記録(誰と・目的)を残しておきましょう。"
        ),
        ExpenseFaq(
            "スマホ・携帯電話の料金",
            ExpenseFaq.Verdict.PARTIAL,
            "事業で使っている割合分だけ通信費として家事按分で経費にできます。仕事専用の番号なら全額。プライベート兼用なら使用割合(例:50%)で按分します。"
        ),
        ExpenseFaq(
            "自宅の家賃",
            ExpenseFaq.Verdict.PARTIAL,
            "自宅兼事務所なら、仕事に使っている床面積の割合などで家事按分して地代家賃に計上できます。持ち家の場合は減価償却費・住宅ローン金利などが対象になり得ます。"
        ),
        ExpenseFaq(
            "仕事用に買ったパソコン(15万円)",
            ExpenseFaq.Verdict.OK,
            "事業用なら経費。ただし10万円以上は原則減価償却ですが、青色申告者なら30万円未満の少額減価償却資産の特例で、その年に一括経費にできます(年間合計300万円まで)。"
        ),
        ExpenseFaq(
            "スーツ・私服などの衣服代",
            ExpenseFaq.Verdict.NG,
            "通常の衣服は私生活でも使えるため原則として経費になりません。撮影用の衣装、作業用の制服・安全靴など、事業専用と明確に言えるものは経費にできます。"
        ),
        ExpenseFaq(
            "自分の国民健康保険料・国民年金",
            ExpenseFaq.Verdict.NG,
            "経費(必要経費)にはできませんが、確定申告で社会保険料控除として全額が所得控除になります。経費ではなく控除で差し引く点に注意。"
        ),
        ExpenseFaq(
            "事業に関係する書籍・セミナー代",
            ExpenseFaq.Verdict.OK,
            "事業の知識・スキル向上のための書籍は新聞図書費、セミナー・研修は研修費として経費にできます。事業との関連を説明できることが前提です。"
        ),
        ExpenseFaq(
            "取引先への手土産・お中元",
            ExpenseFaq.Verdict.OK,
            "事業上の付き合いのための贈答は接待交際費として経費にできます。誰に渡したかを記録しておきましょう。"
        ),
        ExpenseFaq(
            "自分の健康診断・人間ドック",
            ExpenseFaq.Verdict.NG,
            "個人事業主本人の健康診断費用は経費になりません(従業員のための健康診断は福利厚生費として可)。医療費控除の対象にもなりません。"
        ),
        ExpenseFaq(
            "車の購入費・ガソリン代",
            ExpenseFaq.Verdict.PARTIAL,
            "事業で使う車なら、購入費は減価償却、ガソリン・車検・保険などは事業利用割合で家事按分して経費にできます。走行距離などで割合の根拠を残しましょう。"
        ),
        ExpenseFaq(
            "罰金・交通違反の反則金",
            ExpenseFaq.Verdict.NG,
            "業務中であっても、罰金・科料・反則金は経費になりません(税法上、必要経費に算入できないと定められています)。"
        ),
        ExpenseFaq(
            "電気・水道・ガスなどの光熱費",
            ExpenseFaq.Verdict.PARTIAL,
            "自宅兼事務所なら水道光熱費として家事按分で経費にできます。仕事で電気を多く使う場合は使用時間・面積などで割合を決めます。"
        ),
        ExpenseFaq(
            "事業用口座の振込手数料",
            ExpenseFaq.Verdict.OK,
            "報酬の入金や支払いにかかる振込手数料は支払手数料として全額経費にできます。"
        ),
        ExpenseFaq(
            "プライベートと兼用の文房具・消耗品",
            ExpenseFaq.Verdict.PARTIAL,
            "事業で使う分は消耗品費として経費。明確に分けにくいものは事業利用割合で按分します。少額で事業利用が主なら全額計上することが多いです。"
        )
    )

    val taxSavingItems: List<TaxSavingItem> = listOf(
        TaxSavingItem(
            "aoiro65", "青色申告で65万円控除を受ける",
            "最大65万円の所得控除",
            "複式簿記+e-Tax申告(または電子帳簿保存)で最大65万円。会計ソフトを使えば簿記の知識がなくても達成できます。未申請なら承認申請書の提出を。"
        ),
        TaxSavingItem(
            "kyosai", "小規模企業共済に加入する",
            "掛金が全額所得控除",
            "月1,000〜70,000円の掛金が全額所得控除。個人事業主の退職金として積み立てられ、節税効果が非常に大きい制度です。"
        ),
        TaxSavingItem(
            "ideco", "iDeCoに加入する",
            "掛金が全額所得控除+運用益非課税",
            "個人事業主は掛金上限が大きく、全額が所得控除。ただし原則60歳まで引き出せないため余裕資金で。"
        ),
        TaxSavingItem(
            "fuka", "付加年金に加入する",
            "少額で将来の年金を上乗せ",
            "月400円の付加保険料で、将来の年金額が『200円×納付月数』増えます。2年受給で元が取れる手軽な制度。"
        ),
        TaxSavingItem(
            "kazoku", "家族への給与(専従者給与)を活用する",
            "家族への給与を経費に",
            "青色申告なら、事前届出のうえで生計を一にする家族へ支払う給与を経費にできます(青色事業専従者給与)。"
        ),
        TaxSavingItem(
            "shougaku", "30万円未満の資産を一括経費にする",
            "高額機材をその年に経費化",
            "青色申告者は30万円未満の備品を購入年に一括経費にできる特例(年間300万円まで)。利益が出た年の調整に有効。"
        ),
        TaxSavingItem(
            "anbun", "家事按分を漏れなく計上する",
            "自宅経費を取りこぼさない",
            "家賃・光熱費・通信費・車関連費などの事業利用分を按分して経費に。計上漏れは多いので見直しを。"
        ),
        TaxSavingItem(
            "furusato", "ふるさと納税を活用する",
            "実質負担2,000円で寄附",
            "所得に応じた上限内なら、自己負担2,000円で寄附金控除(住民税・所得税)を受けつつ返礼品がもらえます。"
        ),
        TaxSavingItem(
            "safety", "経営セーフティ共済を検討する",
            "掛金を経費にしながら備える",
            "取引先倒産に備える共済。掛金(月5,000〜20万円)を必要経費(または損金)にでき、解約手当金も受け取れます。"
        )
    )

    val annualSchedule: List<ScheduleMonth> = listOf(
        ScheduleMonth(1, listOf(
            "前年分の帳簿を締めて整理する",
            "源泉徴収された分・支払調書を確認する",
            "1/31までに法定調書・償却資産申告(該当者)"
        )),
        ScheduleMonth(2, listOf(
            "確定申告の準備(2/16〜受付開始)",
            "各種控除証明書(国民年金・保険など)を揃える",
            "決算書・申告書を作成する"
        )),
        ScheduleMonth(3, listOf(
            "3/15までに確定申告・所得税の納付",
            "3/15までに消費税の申告(課税事業者・前年分は3/31)",
            "振替納税の利用も検討"
        )),
        ScheduleMonth(4, listOf(
            "固定資産税 第1期の納付(該当者)",
            "新年度の帳簿づけをスタート"
        )),
        ScheduleMonth(5, listOf(
            "自動車税の納付(車を所有している場合)"
        )),
        ScheduleMonth(6, listOf(
            "住民税の納付(第1期/通知書が届く)",
            "前年所得に応じた国民健康保険料の通知"
        )),
        ScheduleMonth(7, listOf(
            "所得税の予定納税 第1期(前年の税額が多い場合)",
            "固定資産税 第2期の納付(該当者)"
        )),
        ScheduleMonth(8, listOf(
            "個人事業税 第1期の納付(対象業種)",
            "住民税 第2期の納付"
        )),
        ScheduleMonth(9, listOf(
            "上半期の売上・経費を振り返る",
            "節税策(共済・iDeCo等)の検討に良い時期"
        )),
        ScheduleMonth(10, listOf(
            "住民税 第3期の納付",
            "年末に向けて経費・設備投資の計画"
        )),
        ScheduleMonth(11, listOf(
            "所得税の予定納税 第2期(該当者)",
            "個人事業税 第2期の納付(対象業種)"
        )),
        ScheduleMonth(12, listOf(
            "今年の利益を確認し最終的な節税対策",
            "30万円未満の設備購入は年内に",
            "領収書・請求書を整理して申告準備"
        ))
    )
}
