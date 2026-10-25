package io.github.hatsu3140.pianotab.converter;

import java.util.ArrayList;
import java.util.List;

/**
 * チューニングとカポの組み合わせの提案 1つ分。
 *
 * @param tuning             チューニングの名前（{@link io.github.hatsu3140.pianotab.converter.model.Tuning#PRESETS} のキー）
 * @param capo               カポの位置（0 = カポなし）
 * @param score              点数（高いほど良い）
 * @param openBassPercent    ベースのうち開放弦で弾ける割合（%）
 * @param droppedMelodyNotes 省くメロディの音の数
 * @param droppedBassNotes   省くベースの音の数
 * @param trimmedNotes       途中で短くする音の数
 */
public record SetupSuggestion(String tuning, int capo, double score, int openBassPercent,
                              int droppedMelodyNotes, int droppedBassNotes, int trimmedNotes) {

    /** 画面に出す理由 */
    public List<String> reasons() {
        List<String> reasons = new ArrayList<>();
        reasons.add("ベースの " + openBassPercent + "% を開放弦で弾けます");
        reasons.add(droppedMelodyNotes == 0 ? "メロディは全部残ります" : "メロディを " + droppedMelodyNotes + " 音省きます");
        if (droppedBassNotes > 0) {
            reasons.add("ベースを " + droppedBassNotes + " 音省きます");
        }
        if (trimmedNotes > 0) {
            reasons.add("途中で短くする音が " + trimmedNotes + " 個あります");
        }
        return reasons;
    }
}
