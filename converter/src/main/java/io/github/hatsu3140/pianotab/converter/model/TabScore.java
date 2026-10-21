package io.github.hatsu3140.pianotab.converter.model;

import java.util.ArrayList;
import java.util.List;

/**
 * フレット割り当てが終わった TAB 譜。メロディ（と和音の内声）とベースは Guitar Pro の別々の声部になる。
 *
 * @param capo    カポの位置。TabNote のフレットはカポの分を足した、ネック上の実際のフレット
 * @param inner   和音の中の音（内声）。メロディと同じタイミング・同じ長さで鳴る
 * @param strokes ストロークを付ける和音の位置
 */
public record TabScore(SongInfo info, Tuning tuning, int capo, List<TabNote> melody, List<TabNote> bass,
                       List<TabNote> inner, List<Stroke> strokes) {
    public TabScore {
        melody = List.copyOf(melody);
        bass = List.copyOf(bass);
        inner = List.copyOf(inner);
        strokes = List.copyOf(strokes);
    }

    /** 内声もストロークもない TAB 譜 */
    public TabScore(SongInfo info, Tuning tuning, int capo, List<TabNote> melody, List<TabNote> bass) {
        this(info, tuning, capo, melody, bass, List.of(), List.of());
    }

    /** メロディ・ベース・内声をまとめたすべての音 */
    public List<TabNote> notes() {
        List<TabNote> all = new ArrayList<>(melody);
        all.addAll(bass);
        all.addAll(inner);
        return all;
    }

    public TabScore withInner(List<TabNote> newInner) {
        return new TabScore(info, tuning, capo, melody, bass, newInner, strokes);
    }

    public TabScore withStrokes(List<Stroke> newStrokes) {
        return new TabScore(info, tuning, capo, melody, bass, inner, newStrokes);
    }
}
