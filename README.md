# piano-to-guitar-tab

ピアノ譜（MusicXML）から、ソロギター用 TAB 譜（Guitar Pro 5 形式 `.gp5`）の下書きを作る Web アプリです。
細かい修正は [TuxGuitar](https://github.com/helge17/tuxguitar) などのソフトで行う前提です。

> まだ開発中です。MusicXML（.musicxml / .xml / .mxl）を読み込んで .gp5 を書き出すところまでできています。
> 難易度（入門・初級・中級・上級）、チューニング（レギュラー / ドロップD / DADGAD / オープンD / オープンG / CGDGBD）、カポ、移調を画面で選べます。
> 歌の段があれば歌をメロディに、ピアノは右手・左手を分けて読み、強弱記号と3連符も書き出します。
> 難易度に合わせて和音の音を足し、ピアノが和音を弾いている所にはストロークを入れます（セーハ・ストロークは画面でオフにできます）。
> できた TAB 譜は、どの瞬間も指が届くか・指が足りるかを確かめ、結果を画面に表示します。
> 画面でのプレビュー、PDF の読み込みはこれから追加していきます。

## 必要なもの

- Java 21（JDK）

Maven は同梱の `mvnw`（Maven Wrapper）が自動でダウンロードするので、別途インストール不要です。

## 動かし方

```bash
# ビルドとテスト
./mvnw verify

# アプリを起動（Windows は mvnw.cmd）
./mvnw install -DskipTests
./mvnw -pl web spring-boot:run
```

起動したらブラウザで http://localhost:8080 を開きます。

## フォルダ構成

```
piano-to-guitar-tab/
├── pom.xml          全体の設定（Maven）
├── converter/       変換ロジック本体（Spring を使わない普通の Java）
│   └── src/main/java/io/github/hatsu3140/pianotab/converter/
│       ├── GuitarTabConverter.java  変換の入口（下の4手順を順に呼ぶ）
│       ├── musicxml/   1. MusicXML の読み込み
│       ├── arrange/    2. メロディ・ベースの抽出と、弾きやすい高さへのオクターブ調整
│       ├── fret/       3. 弦・フレットの割り当て（指が届き、移動が少ない押さえ方を選ぶ）、
│       │               和音・ストロークの追加、運指（どの指で押さえるか）を決めて弾けるかのチェック
│       ├── gp5/        4. Guitar Pro 5 形式で書き出し
│       └── model/      音符や TAB 譜を表すデータ
└── web/             Web アプリ（Spring Boot）
    └── src/main/
        ├── java/.../web/      サーバー側（ファイルを受け取って converter を呼ぶ）
        └── resources/static/  画面（HTML / CSS / JavaScript）
```

## ライセンス

[MIT](LICENSE)
