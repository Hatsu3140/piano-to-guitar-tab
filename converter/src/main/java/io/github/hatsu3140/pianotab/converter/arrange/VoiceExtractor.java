package io.github.hatsu3140.pianotab.converter.arrange;

import io.github.hatsu3140.pianotab.converter.model.Arrangement;
import io.github.hatsu3140.pianotab.converter.model.Note;
import io.github.hatsu3140.pianotab.converter.model.Score;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 手順2: ピアノの音からメロディとベースだけを取り出す。
 *
 * <ul>
 *   <li>歌の段があれば、メロディは歌の段から取る。なければピアノの上段（右手）の一番高い音</li>
 *   <li>ベースはピアノの下段（左手）の一番低い音</li>
 *   <li>段の区別がない楽譜では、同じタイミングで鳴り始める音のうち一番高い音をメロディ、
 *       残りの一番低い音をベースにする</li>
 * </ul>
 * どの場合も、メロディがまだ伸びている間に鳴り始めた低い音はメロディにしない
 * （伴奏の音がメロディに混ざらないようにするため）。ベースも同じように扱う。
 *
 * <p>メロディにもベースにも選ばれなかったピアノの音は、和音の候補（harmony）として残す。
 */
public class VoiceExtractor {

    public Arrangement extract(Score score) {
        List<Note> piano = new ArrayList<>(score.upper());
        piano.addAll(score.lower());
        List<Note> melodySource = score.vocal().isEmpty() ? score.upper() : score.vocal();
        Arrangement twoVoices;
        if (!score.lower().isEmpty()) {
            twoVoices = new Arrangement(score.info(), topLine(melodySource), bottomLine(score.lower()));
        } else if (!score.vocal().isEmpty()) {
            twoVoices = new Arrangement(score.info(), topLine(score.vocal()), bottomLine(score.upper()));
        } else {
            twoVoices = extractFromOneStaff(score);
        }
        return new Arrangement(score.info(), twoVoices.melody(), twoVoices.bass(),
                remaining(piano, twoVoices.melody(), twoVoices.bass()));
    }

    /** notes のうち、メロディにもベースにも使われなかった音 */
    private static List<Note> remaining(List<Note> notes, List<Note> melody, List<Note> bass) {
        Set<Note> used = Collections.newSetFromMap(new IdentityHashMap<>());
        used.addAll(melody);
        used.addAll(bass);
        return notes.stream().filter(note -> !used.contains(note)).toList();
    }

    /** 一番上の線: 各タイミングの一番高い音。伸びている音より低い音は取らない */
    private static List<Note> topLine(List<Note> notes) {
        List<Note> line = new ArrayList<>();
        Note current = null;
        for (Map.Entry<Long, List<Note>> entry : groupByStart(notes).entrySet()) {
            List<Note> chord = entry.getValue();
            Note highest = chord.get(chord.size() - 1);
            if (!isSounding(current, entry.getKey()) || highest.pitch() >= current.pitch()) {
                line.add(highest);
                current = highest;
            }
        }
        return line;
    }

    /** 一番下の線: 各タイミングの一番低い音。伸びている音より高い音は取らない */
    private static List<Note> bottomLine(List<Note> notes) {
        List<Note> line = new ArrayList<>();
        Note current = null;
        for (Map.Entry<Long, List<Note>> entry : groupByStart(notes).entrySet()) {
            Note lowest = entry.getValue().get(0);
            if (!isSounding(current, entry.getKey()) || lowest.pitch() <= current.pitch()) {
                line.add(lowest);
                current = lowest;
            }
        }
        return line;
    }

    /** 段の区別がない楽譜: 同じ音の集まりからメロディとベースを両方取る */
    private static Arrangement extractFromOneStaff(Score score) {
        List<Note> melody = new ArrayList<>();
        List<Note> bass = new ArrayList<>();
        Note currentMelody = null;
        Note currentBass = null;
        for (Map.Entry<Long, List<Note>> entry : groupByStart(score.upper()).entrySet()) {
            long start = entry.getKey();
            List<Note> chord = new ArrayList<>(entry.getValue());

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

    /** 鳴り始めのタイミングごとにまとめ、それぞれ低い音から順に並べる */
    private static Map<Long, List<Note>> groupByStart(List<Note> notes) {
        Map<Long, List<Note>> notesByStart = new TreeMap<>();
        for (Note note : notes) {
            notesByStart.computeIfAbsent(note.startTick(), tick -> new ArrayList<>()).add(note);
        }
        notesByStart.values().forEach(chord -> chord.sort(Comparator.comparingInt(Note::pitch)));
        return notesByStart;
    }

    private static boolean isSounding(Note note, long tick) {
        return note != null && note.startTick() + note.durationTicks() > tick;
    }
}
