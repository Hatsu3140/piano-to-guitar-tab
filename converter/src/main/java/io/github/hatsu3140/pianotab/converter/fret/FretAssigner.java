package io.github.hatsu3140.pianotab.converter.fret;

import io.github.hatsu3140.pianotab.converter.model.Arrangement;
import io.github.hatsu3140.pianotab.converter.model.Note;
import io.github.hatsu3140.pianotab.converter.model.TabNote;
import io.github.hatsu3140.pianotab.converter.model.TabScore;
import io.github.hatsu3140.pianotab.converter.model.Tuning;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 手順3: 各音を「何弦の何フレット」で弾くか決める。
 *
 * <p>今は仮の実装で、同時に鳴っている音が同じ弦にならないようにしつつ、
 * なるべく低いフレットを選ぶだけ。ギターの音域から外れた音はオクターブ移動して収め、
 * それでも弾けない音は、弾けない譜面を作らないよう捨てる。
 * TODO: 指の届く範囲（ストレッチ）や前後の音とのつながりを考えた割り当て（DP）にする。
 */
public class FretAssigner {

    public static final int MAX_FRET = 19;

    private final Tuning tuning;

    public FretAssigner(Tuning tuning) {
        this.tuning = tuning;
    }

    public TabScore assign(Arrangement arrangement) {
        Map<Long, List<VoiceNote>> notesByStart = new TreeMap<>();
        for (Note note : arrangement.melody()) {
            notesByStart.computeIfAbsent(note.startTick(), tick -> new ArrayList<>()).add(new VoiceNote(note, true));
        }
        for (Note note : arrangement.bass()) {
            notesByStart.computeIfAbsent(note.startTick(), tick -> new ArrayList<>()).add(new VoiceNote(note, false));
        }

        List<TabNote> melody = new ArrayList<>();
        List<TabNote> bass = new ArrayList<>();
        List<TabNote> sounding = new ArrayList<>();
        for (Map.Entry<Long, List<VoiceNote>> entry : notesByStart.entrySet()) {
            long start = entry.getKey();
            List<VoiceNote> chord = entry.getValue();
            // まだ伸びている音の弦は使えない（押さえ直すと前の音が止まってしまう）
            sounding.removeIf(tabNote -> tabNote.note().startTick() + tabNote.note().durationTicks() <= start);
            Set<Integer> usedStrings = new HashSet<>();
            for (TabNote tabNote : sounding) {
                usedStrings.add(tabNote.string());
            }
            // 高い音から順に、まだ使っていない弦へ割り当てる
            chord.sort(Comparator.comparingInt((VoiceNote voiceNote) -> voiceNote.note().pitch()).reversed());
            for (VoiceNote voiceNote : chord) {
                TabNote tabNote = findPosition(fitIntoRange(voiceNote.note()), usedStrings);
                if (tabNote != null) {
                    usedStrings.add(tabNote.string());
                    sounding.add(tabNote);
                    (voiceNote.isMelody() ? melody : bass).add(tabNote);
                }
            }
        }
        return new TabScore(arrangement.info(), tuning, melody, bass);
    }

    /** ギターで出せる高さになるまでオクターブ単位で上げ下げする */
    private Note fitIntoRange(Note note) {
        int lowest = tuning.openStringPitches().stream().min(Integer::compare).orElseThrow();
        int highest = tuning.openStringPitches().stream().max(Integer::compare).orElseThrow() + MAX_FRET;
        int pitch = note.pitch();
        while (pitch < lowest) {
            pitch += 12;
        }
        while (pitch > highest) {
            pitch -= 12;
        }
        return new Note(pitch, note.startTick(), note.durationTicks());
    }

    private TabNote findPosition(Note note, Set<Integer> usedStrings) {
        TabNote best = null;
        for (int string = 1; string <= tuning.stringCount(); string++) {
            int fret = note.pitch() - tuning.openPitch(string);
            if (fret < 0 || fret > MAX_FRET || usedStrings.contains(string)) {
                continue;
            }
            if (best == null || fret < best.fret()) {
                best = new TabNote(note, string, fret);
            }
        }
        return best;
    }

    private record VoiceNote(Note note, boolean isMelody) {
    }
}
