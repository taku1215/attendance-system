#!/usr/bin/env python3
"""
コード進行ジェネレーター
Claude APIを使って、スケールに基づいたコード進行を生成します。
"""

import anthropic

client = anthropic.Anthropic()

# 利用可能なキー（音名）
KEYS = ["C", "C#/Db", "D", "D#/Eb", "E", "F", "F#/Gb", "G", "G#/Ab", "A", "A#/Bb", "B"]

# 利用可能なスケール
SCALES = {
    "1": "メジャースケール (Major)",
    "2": "ナチュラルマイナースケール (Natural Minor)",
    "3": "ハーモニックマイナースケール (Harmonic Minor)",
    "4": "メロディックマイナースケール (Melodic Minor)",
    "5": "ドリアンスケール (Dorian)",
    "6": "フリジアンスケール (Phrygian)",
    "7": "リディアンスケール (Lydian)",
    "8": "ミクソリディアンスケール (Mixolydian)",
    "9": "ロクリアンスケール (Locrian)",
    "10": "ペンタトニックメジャースケール (Pentatonic Major)",
    "11": "ペンタトニックマイナースケール (Pentatonic Minor)",
    "12": "ブルーススケール (Blues Scale)",
}

# ジャンル/ムード
MOODS = {
    "1": "明るい・ポップ (Bright/Pop)",
    "2": "悲しい・メランコリック (Sad/Melancholic)",
    "3": "クール・ジャズ (Cool/Jazz)",
    "4": "ロック・パワフル (Rock/Powerful)",
    "5": "リラックス・アンビエント (Relax/Ambient)",
    "6": "ロマンチック・バラード (Romantic/Ballad)",
    "7": "緊張・サスペンス (Tense/Suspense)",
    "8": "おまかせ (Anything)",
}


def display_menu(title: str, options: dict) -> str:
    """メニューを表示して選択を受け取る"""
    print(f"\n{'='*50}")
    print(f"  {title}")
    print(f"{'='*50}")
    for key, value in options.items():
        print(f"  {key}. {value}")
    print(f"{'='*50}")
    while True:
        choice = input("番号を選んでください: ").strip()
        if choice in options:
            return choice
        print("無効な選択です。もう一度入力してください。")


def display_keys() -> str:
    """キー選択を表示"""
    print(f"\n{'='*50}")
    print("  キー（音名）を選択してください")
    print(f"{'='*50}")
    for i, key in enumerate(KEYS, 1):
        print(f"  {i:2d}. {key}")
    print(f"{'='*50}")
    while True:
        try:
            choice = int(input("番号を選んでください: ").strip())
            if 1 <= choice <= len(KEYS):
                return KEYS[choice - 1]
            print("無効な選択です。もう一度入力してください。")
        except ValueError:
            print("数字を入力してください。")


def get_chord_progression(key: str, scale: str, mood: str, bars: int) -> str:
    """Claude APIを使ってコード進行を生成"""
    system_prompt = """あなたは音楽理論の専門家です。
ユーザーが指定したキー、スケール、ムードに基づいて、実用的で美しいコード進行を提案してください。

以下の形式で回答してください：

## コード進行

**基本情報**
- キー: [キー名]
- スケール: [スケール名]
- ムード: [ムード名]

**スケール構成音**
[スケールの構成音をリスト表示]

**ダイアトニックコード**
[スケールから導かれるダイアトニックコードを表示]

**提案するコード進行**

### パターン1: [名前/説明]
`[コード進行]`
> 特徴: [このコード進行の特徴と使い方]

### パターン2: [名前/説明]
`[コード進行]`
> 特徴: [このコード進行の特徴と使い方]

### パターン3: [名前/説明]
`[コード進行]`
> 特徴: [このコード進行の特徴と使い方]

**アレンジのヒント**
[このコード進行をさらに発展させるためのアドバイス]

コードは明確に表記し（例: Cmaj7, Am7, G7, Fmaj7など）、実際の音楽制作ですぐに使えるように具体的に提案してください。"""

    user_message = f"""以下の条件でコード進行を生成してください：

- キー: {key}
- スケール: {scale}
- ムード/ジャンル: {mood}
- 小節数: {bars}小節

{bars}小節のコード進行を3パターン提案してください。
各コードは1小節ずつ（または適切な長さで）割り当ててください。"""

    print("\n♩ コード進行を生成中...")
    print("(Claude AIが考えています...)\n")

    with client.messages.stream(
        model="claude-opus-4-6",
        max_tokens=2000,
        thinking={"type": "adaptive"},
        system=system_prompt,
        messages=[{"role": "user", "content": user_message}],
    ) as stream:
        full_response = ""
        for text in stream.text_stream:
            print(text, end="", flush=True)
            full_response += text

    return full_response


def ask_another_question(key: str, scale: str, mood: str) -> str:
    """追加の質問をする"""
    print(f"\n{'='*50}")
    print("  追加の質問がありますか？")
    print(f"{'='*50}")
    print("  (例: 転調の方法、ボイシングのアドバイス、など)")
    print("  空白のままEnterを押すと終了します")
    question = input("\n質問: ").strip()
    return question


def main():
    print("\n" + "♪" * 25)
    print("  コード進行ジェネレーター")
    print("  Chord Progression Generator")
    print("♪" * 25)
    print("\nClaude AIがあなたの音楽制作をサポートします！")

    while True:
        # キー選択
        key = display_keys()
        print(f"✓ キー: {key}")

        # スケール選択
        scale_choice = display_menu("スケールを選択してください", SCALES)
        scale = SCALES[scale_choice]
        print(f"✓ スケール: {scale}")

        # ムード選択
        mood_choice = display_menu("ムード/ジャンルを選択してください", MOODS)
        mood = MOODS[mood_choice]
        print(f"✓ ムード: {mood}")

        # 小節数
        print(f"\n{'='*50}")
        print("  小節数を選択してください")
        print(f"{'='*50}")
        bars_options = {"1": "4小節", "2": "8小節", "3": "12小節", "4": "16小節"}
        for k, v in bars_options.items():
            print(f"  {k}. {v}")
        print(f"{'='*50}")
        while True:
            bars_choice = input("番号を選んでください: ").strip()
            if bars_choice in bars_options:
                bars = int(bars_options[bars_choice][0]) * 4  # "4小節" -> 4
                # Fix: parse the number correctly
                bars_map = {"1": 4, "2": 8, "3": 12, "4": 16}
                bars = bars_map[bars_choice]
                break
            print("無効な選択です。もう一度入力してください。")

        print(f"✓ 小節数: {bars}小節")

        # コード進行生成
        print(f"\n選択内容:")
        print(f"  キー: {key} / スケール: {scale}")
        print(f"  ムード: {mood} / {bars}小節")

        get_chord_progression(key, scale, mood, bars)

        # 追加質問ループ
        while True:
            question = ask_another_question(key, scale, mood)
            if not question:
                break

            print(f"\n♩ 回答を生成中...\n")
            with client.messages.stream(
                model="claude-opus-4-6",
                max_tokens=1500,
                thinking={"type": "adaptive"},
                system="""あなたは音楽理論の専門家です。
音楽制作に関する質問に、実践的かつ具体的に答えてください。
コード名、スケール、音楽理論の用語を正確に使い、初心者にも分かりやすく説明してください。""",
                messages=[
                    {
                        "role": "user",
                        "content": f"キー: {key}, スケール: {scale}, ムード: {mood} の文脈で質問です：\n\n{question}",
                    }
                ],
            ) as stream:
                for text in stream.text_stream:
                    print(text, end="", flush=True)
            print()

        # 続けるか確認
        print(f"\n{'='*50}")
        print("  別のコード進行を生成しますか？")
        print(f"{'='*50}")
        print("  1. はい (別の条件で生成)")
        print("  2. いいえ (終了)")
        print(f"{'='*50}")
        cont = input("選択: ").strip()
        if cont != "1":
            print("\n♪ ご利用ありがとうございました！素敵な音楽を！♪\n")
            break


if __name__ == "__main__":
    main()
