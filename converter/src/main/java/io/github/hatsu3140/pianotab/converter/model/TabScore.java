package io.github.hatsu3140.pianotab.converter.model;

import java.util.ArrayList;
import java.util.List;

/**
 * フレット割り当てが終わった TAB 譜。メロディとベースは Guitar Pro の別々の声部になる。
 */
public record TabScore(SongInfo info, Tuning tuning, List<TabNote> melody, List<TabNote> bass) {
    public TabScore {
        melody = List.copyOf(melody);
        bass = List.copyOf(bass);
    }

    /** メロディとベースをまとめたすべての音 */
    public List<TabNote> notes() {
        List<TabNote> all = new ArrayList<>(melody);
        all.addAll(bass);
        return all;
    }
}
