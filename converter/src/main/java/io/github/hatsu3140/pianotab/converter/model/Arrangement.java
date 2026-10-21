package io.github.hatsu3140.pianotab.converter.model;

import java.util.List;

/**
 * ソロギター用に音を減らした結果。メロディとベースの2声と、和音に使える残りの音。
 *
 * @param harmony メロディにもベースにも選ばれなかったピアノの音。和音の中の音（内声）の候補にする。
 *                高さは元の楽譜のまま（使うときに、メロディとベースの間に入るオクターブへ移す）
 */
public record Arrangement(SongInfo info, List<Note> melody, List<Note> bass, List<Note> harmony) {
    public Arrangement {
        melody = List.copyOf(melody);
        bass = List.copyOf(bass);
        harmony = List.copyOf(harmony);
    }

    /** 和音の候補がない2声だけの編曲 */
    public Arrangement(SongInfo info, List<Note> melody, List<Note> bass) {
        this(info, melody, bass, List.of());
    }
}
