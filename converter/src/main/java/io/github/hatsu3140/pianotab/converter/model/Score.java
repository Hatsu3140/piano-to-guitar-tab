package io.github.hatsu3140.pianotab.converter.model;

import java.util.List;

/**
 * MusicXML から読み込んだピアノ譜（右手・左手をまとめた全音符）。
 *
 * @param info  曲全体の情報
 * @param notes すべての音符
 */
public record Score(SongInfo info, List<Note> notes) {
    public Score {
        notes = List.copyOf(notes);
    }
}
