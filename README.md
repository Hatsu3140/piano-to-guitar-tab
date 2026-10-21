# piano-to-guitar-tab

ピアノ譜（MusicXML）から、ソロギター用 TAB 譜（Guitar Pro 5 形式 `.gp5`）の下書きを作る Web アプリです。
細かい修正は [TuxGuitar](https://github.com/helge17/tuxguitar) などのソフトで行う前提です。

> まだ開発中です。MusicXML（.musicxml / .xml / .mxl）を読み込んで .gp5 を書き出すところまでできています。
> 弾きやすい運指（フレットの選び方）や3連符への対応はこれから改善していきます。

## 必要なもの

- Java 21（JDK）

Maven は同梱の `mvnw`（Maven Wrapper）が自動でダウンロードするので、別途インストール不要です。

## 動かし方

```bash
# ビルドとテスト
./mvnw verify

# アプリを起動（Windows は mvnw.cmd）
./mvnw -pl web -am spring-boot:run
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
│       ├── arrange/    2. メロディ・ベースの抽出
│       ├── fret/       3. 弦・フレットの割り当て
│       ├── gp5/        4. Guitar Pro 5 形式で書き出し
│       └── model/      音符や TAB 譜を表すデータ
└── web/             Web アプリ（Spring Boot）
    └── src/main/
        ├── java/.../web/      サーバー側（ファイルを受け取って converter を呼ぶ）
        └── resources/static/  画面（HTML / CSS / JavaScript）
```

## ライセンス

[MIT](LICENSE)
