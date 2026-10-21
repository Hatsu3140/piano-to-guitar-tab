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

    /** 1拍の長さ。6/8 拍子のような複合拍子は付点4分音符を1拍とする */
    public long beatTicks(int ticksPerQuarter) {
        long unit = (long) ticksPerQuarter * 4 / beatType;
        boolean compound = beatType == 8 && beats % 3 == 0 && beats > 3;
        return compound ? unit * 3 : unit;
    }
}
