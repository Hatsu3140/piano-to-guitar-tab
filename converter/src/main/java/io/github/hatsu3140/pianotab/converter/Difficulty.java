package io.github.hatsu3140.pianotab.converter;

/**
 * 難易度。段階ごとに、ベースの入れ方・使うフレットの範囲・指の広げ方・ポジション移動の少なさ・
 * 同時に鳴らす音の数を変える。
 * フレットはカポを基準に数える（カポ3なら、カポの位置が0フレット）。
 */
public enum Difficulty {
    /** 入門: メロディだけ。0〜5フレット */
    INTRODUCTORY("入門", BassRhythm.NONE, 5, 3, 3.0, 0, 1, 1, 1, 1),
    /** 初級: メロディ＋小節頭のベース（開放弦中心）。0〜5フレット。ふだんは2音、盛り上がる所（f 以上）は3音まで */
    BEGINNER("初級", BassRhythm.MEASURE, 5, 3, 3.0, 2.0, 2, 3, 4, 3),
    /** 中級: メロディ＋拍頭のベース＋和音。0〜7フレット、指の幅は最大4フレット、3音まで */
    INTERMEDIATE("中級", BassRhythm.BEAT, 7, 4, 1.0, 0, 3, 3, 4, 4),
    /** 上級: メロディ＋ベース（伴奏の刻みは除く）＋和音。0〜15フレット、5音まで */
    ADVANCED("上級", BassRhythm.ALL, 15, 4, 0.5, 0, 5, 5, 6, 4);

    /** ベースをどのくらい残すか */
    public enum BassRhythm {
        /** ベースなし */
        NONE,
        /** 小節の頭だけ */
        MEASURE,
        /** 拍の頭だけ */
        BEAT,
        /** すべて。ただし同じ拍の中でベースより上がる音（アルベルティ・バスなどの伴奏の刻み）は除く */
        ALL
    }

    private final String label;
    private final BassRhythm bassRhythm;
    private final int maxFret;
    private final int maxStretch;
    private final double moveCost;
    private final double frettedBassCost;
    private final int maxNotes;
    private final int climaxNotes;
    private final int strokeNotes;
    private final int maxFingers;

    Difficulty(String label, BassRhythm bassRhythm, int maxFret, int maxStretch, double moveCost,
               double frettedBassCost, int maxNotes, int climaxNotes, int strokeNotes, int maxFingers) {
        this.label = label;
        this.bassRhythm = bassRhythm;
        this.maxFret = maxFret;
        this.maxStretch = maxStretch;
        this.moveCost = moveCost;
        this.frettedBassCost = frettedBassCost;
        this.maxNotes = maxNotes;
        this.climaxNotes = climaxNotes;
        this.strokeNotes = strokeNotes;
        this.maxFingers = maxFingers;
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

    /** 同時に鳴らす音の数の上限（ストロークの和音は別。{@link #strokeNotes()}） */
    public int maxNotes() {
        return maxNotes;
    }

    /** 盛り上がる所（f 以上）で同時に鳴らす音の数の上限 */
    public int climaxNotes() {
        return climaxNotes;
    }

    /**
     * ストロークで鳴らす和音の音の数の上限。ストロークは弦をなぞるだけなので、
     * 音の数は多くても難しさは「1音」と同じくらいと考え、ふだんより多くしてよい
     */
    public int strokeNotes() {
        return strokeNotes;
    }

    /** 同時に押さえてよい指の数（セーハは1本と数える） */
    public int maxFingers() {
        return maxFingers;
    }
}
