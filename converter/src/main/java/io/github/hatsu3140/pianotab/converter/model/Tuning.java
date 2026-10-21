package io.github.hatsu3140.pianotab.converter.model;

import java.util.List;

/**
 * ギターのチューニング。
 *
 * @param name              表示名
 * @param openStringPitches 開放弦の MIDI ノート番号。1弦（いちばん細い弦）から順に並べる
 */
public record Tuning(String name, List<Integer> openStringPitches) {
    /** レギュラーチューニング（1弦から E4 B3 G3 D3 A2 E2） */
    public static final Tuning STANDARD = new Tuning("Standard", List.of(64, 59, 55, 50, 45, 40));
    /** ドロップD（6弦だけ1音下げて D2） */
    public static final Tuning DROP_D = new Tuning("Drop D", List.of(64, 59, 55, 50, 45, 38));

    public Tuning {
        openStringPitches = List.copyOf(openStringPitches);
    }

    public int stringCount() {
        return openStringPitches.size();
    }

    /** @param string 弦番号（1 始まり） */
    public int openPitch(int string) {
        return openStringPitches.get(string - 1);
    }

    public int lowestPitch() {
        return openStringPitches.stream().min(Integer::compare).orElseThrow();
    }

    public int highestPitch() {
        return openStringPitches.stream().max(Integer::compare).orElseThrow();
    }
}
