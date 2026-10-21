package io.github.hatsu3140.pianotab.converter.model;

import java.util.List;

/**
 * MusicXML から読み込んだピアノ譜（右手・左手をまとめた全音符）。
 *
 * @param ticksPerQuarter 4分音符1つあたりの tick 数
 * @param notes           すべての音符
 */
public record Score(int ticksPerQuarter, List<Note> notes) {
    public Score {
        notes = List.copyOf(notes);
    }
}
