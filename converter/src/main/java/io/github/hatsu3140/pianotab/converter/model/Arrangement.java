package io.github.hatsu3140.pianotab.converter.model;

import java.util.List;

/**
 * ソロギター用に音を減らした結果（メロディとベースの2声）。
 */
public record Arrangement(SongInfo info, List<Note> melody, List<Note> bass) {
    public Arrangement {
        melody = List.copyOf(melody);
        bass = List.copyOf(bass);
    }
}
