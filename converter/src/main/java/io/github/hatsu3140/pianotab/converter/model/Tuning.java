package io.github.hatsu3140.pianotab.converter.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    /** DADGAD（6弦から D2 A2 D3 G3 A3 D4） */
    public static final Tuning DADGAD = new Tuning("DADGAD", List.of(62, 57, 55, 50, 45, 38));
    /** オープンD（6弦から D2 A2 D3 F#3 A3 D4。開放弦で D のコード） */
    public static final Tuning OPEN_D = new Tuning("Open D", List.of(62, 57, 54, 50, 45, 38));
    /** オープンG（6弦から D2 G2 D3 G3 B3 D4。開放弦で G のコード） */
    public static final Tuning OPEN_G = new Tuning("Open G", List.of(62, 59, 55, 50, 43, 38));
    /** CGDGBD（6弦から C2 G2 D3 G3 B3 D4。オープンG の6弦をさらに下げたもの） */
    public static final Tuning CGDGBD = new Tuning("CGDGBD", List.of(62, 59, 55, 50, 43, 36));

    /** 画面で選べるチューニング。キーは画面とのやり取りに使う名前 */
    public static final Map<String, Tuning> PRESETS = presets();

    public Tuning {
        openStringPitches = List.copyOf(openStringPitches);
    }

    private static Map<String, Tuning> presets() {
        Map<String, Tuning> presets = new LinkedHashMap<>();
        presets.put("STANDARD", STANDARD);
        presets.put("DROP_D", DROP_D);
        presets.put("DADGAD", DADGAD);
        presets.put("OPEN_D", OPEN_D);
        presets.put("OPEN_G", OPEN_G);
        presets.put("CGDGBD", CGDGBD);
        return Collections.unmodifiableMap(presets);
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
