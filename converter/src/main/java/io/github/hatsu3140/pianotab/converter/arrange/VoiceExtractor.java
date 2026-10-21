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
 * <p>同じタイミングで鳴り始める音のうち、一番高い音をメロディ、残りの一番低い音をベースにする。
 * ただし、メロディがまだ伸びている間に鳴り始めた低い音はメロディにしない
 * （伴奏の音がメロディに混ざらないようにするため）。ベースも同じように扱う。
 */
public class VoiceExtractor {

    public Arrangement extract(Score score) {
        Map<Long, List<Note>> notesByStart = new TreeMap<>();
        for (Note note : score.notes()) {
            notesByStart.computeIfAbsent(note.startTick(), tick -> new ArrayList<>()).add(note);
        }

        List<Note> melody = new ArrayList<>();
        List<Note> bass = new ArrayList<>();
        Note currentMelody = null;
        Note currentBass = null;
        for (Map.Entry<Long, List<Note>> entry : notesByStart.entrySet()) {
            long start = entry.getKey();
            List<Note> chord = new ArrayList<>(entry.getValue());
            chord.sort(Comparator.comparingInt(Note::pitch));

            Note highest = chord.get(chord.size() - 1);
            if (!isSounding(currentMelody, start) || highest.pitch() >= currentMelody.pitch()) {
                melody.add(highest);
                currentMelody = highest;
                chord.remove(chord.size() - 1);
            }

            if (!chord.isEmpty()) {
                Note lowest = chord.get(0);
                boolean belowMelody = lowest.pitch() < currentMelody.pitch() || !isSounding(currentMelody, start);
                if (belowMelody && (!isSounding(currentBass, start) || lowest.pitch() <= currentBass.pitch())) {
                    bass.add(lowest);
                    currentBass = lowest;
                }
            }
        }
        return new Arrangement(score.info(), melody, bass);
    }

    private static boolean isSounding(Note note, long tick) {
        return note != null && note.startTick() + note.durationTicks() > tick;
    }
}
