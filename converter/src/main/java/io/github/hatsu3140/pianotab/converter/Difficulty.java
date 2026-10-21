package io.github.hatsu3140.pianotab.converter;

/**
 * 難易度。段階ごとに、ベースの入れ方・使うフレットの範囲・指の広げ方・ポジション移動の少なさを変える。
 * フレットはカポを基準に数える（カポ3なら、カポの位置が0フレット）。
 */
public enum Difficulty {
    /** 入門: メロディだけ。0〜5フレット */
    INTRODUCTORY("入門", BassRhythm.NONE, 5, 3, 3.0, 0),
    /** 初級: メロディ＋小節頭のベース（開放弦中心）。0〜5フレット */
    BEGINNER("初級", BassRhythm.MEASURE, 5, 3, 3.0, 2.0),
    /** 中級: メロディ＋拍頭のベース。0〜7フレット、指の幅は最大4フレット */
    INTERMEDIATE("中級", BassRhythm.BEAT, 7, 4, 1.0, 0),
    /** 上級: メロディ＋ベース（全部）。0〜15フレット */
    ADVANCED("上級", BassRhythm.ALL, 15, 4, 0.5, 0);

    /** ベースをどのくらい残すか */
    public enum BassRhythm {
        /** ベースなし */
        NONE,
        /** 小節の頭だけ */
        MEASURE,
        /** 拍の頭だけ */
        BEAT,
        /** すべて */
        ALL
    }

    private final String label;
    private final BassRhythm bassRhythm;
    private final int maxFret;
    private final int maxStretch;
    private final double moveCost;
    private final double frettedBassCost;

    Difficulty(String label, BassRhythm bassRhythm, int maxFret, int maxStretch, double moveCost,
               double frettedBassCost) {
        this.label = label;
        this.bassRhythm = bassRhythm;
        this.maxFret = maxFret;
        this.maxStretch = maxStretch;
        this.moveCost = moveCost;
        this.frettedBassCost = frettedBassCost;
    }

    /** 画面に表示する名前 */
    public String label() {
        return label;
    }

    public BassRhythm bassRhythm() {
        return bassRhythm;
    }

    /** 使う一番高いフレット（カポ基準） */
    public int maxFret() {
        return maxFret;
    }

    /** 同時に押さえる音のフレットの差の上限 */
    public int maxStretch() {
        return maxStretch;
    }

    /** ポジション移動1フレットあたりのコスト。大きいほど移動を避ける */
    public double moveCost() {
        return moveCost;
    }

    /** ベースを開放弦以外で弾くときの追加コスト。大きいほど開放弦のベースを好む */
    public double frettedBassCost() {
        return frettedBassCost;
    }
}
