package io.github.hatsu3140.pianotab.converter.model;

/**
 * 1小節の情報。
 *
 * @param startTick   小節の開始位置（tick 単位）
 * @param lengthTicks 小節の長さ（tick 単位）。弱起（アウフタクト）の小節は拍子より短くなる
 * @param beats       拍子の分子（3/4 拍子なら 3）
 * @param beatType    拍子の分母（3/4 拍子なら 4）
 */
public record Measure(long startTick, long lengthTicks, int beats, int beatType) {
}
