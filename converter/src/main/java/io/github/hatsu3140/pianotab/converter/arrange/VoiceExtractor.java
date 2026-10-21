package io.github.hatsu3140.pianotab.converter.arrange;

import io.github.hatsu3140.pianotab.converter.model.Arrangement;
import io.github.hatsu3140.pianotab.converter.model.Note;
import io.github.hatsu3140.pianotab.converter.model.Score;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 手順2: ピアノの音からメロディとベースだけを取り出す。
 *
 * <p>今は最もシンプルな方法として、同じタイミングで鳴る音のうち
 * 一番高い音をメロディ、一番低い音をベースにしている。
 */
public class VoiceExtractor {

    public Arrangement extract(Score score) {
        Map<Long, List<Note>> notesByStart = new TreeMap<>();
        for (Note note : score.notes()) {
            notesByStart.computeIfAbsent(note.startTick(), tick -> new ArrayList<>()).add(note);
        }

        List<Note> melody = new ArrayList<>();
        List<Note> bass = new ArrayList<>();
        for (List<Note> chord : notesByStart.values()) {
            Note highest = chord.stream().max(Comparator.comparingInt(Note::pitch)).orElseThrow();
            Note lowest = chord.stream().min(Comparator.comparingInt(Note::pitch)).orElseThrow();
            melody.add(highest);
            if (lowest.pitch() != highest.pitch()) {
                bass.add(lowest);
            }
        }
        return new Arrangement(score.ticksPerQuarter(), melody, bass);
    }
}
