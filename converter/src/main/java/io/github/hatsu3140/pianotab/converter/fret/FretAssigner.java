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
 * <p>今は仮の実装で、同時に鳴る音が同じ弦にならないようにしつつ、
 * なるべく低いフレットを選ぶだけ。ギターで出せない音は弾けない譜面を作らないよう捨てる。
 * TODO: 指の届く範囲（ストレッチ）や前後の音とのつながりを考えた割り当て（DP）にする。
 */
public class FretAssigner {

    public static final int MAX_FRET = 19;

    private final Tuning tuning;

    public FretAssigner(Tuning tuning) {
        this.tuning = tuning;
    }

    public TabScore assign(Arrangement arrangement) {
        Map<Long, List<Note>> notesByStart = new TreeMap<>();
        for (Note note : arrangement.melody()) {
            notesByStart.computeIfAbsent(note.startTick(), tick -> new ArrayList<>()).add(note);
        }
        for (Note note : arrangement.bass()) {
            notesByStart.computeIfAbsent(note.startTick(), tick -> new ArrayList<>()).add(note);
        }

        List<TabNote> tabNotes = new ArrayList<>();
        for (List<Note> chord : notesByStart.values()) {
            Set<Integer> usedStrings = new HashSet<>();
            // 高い音から順に、まだ使っていない弦へ割り当てる
            chord.sort(Comparator.comparingInt(Note::pitch).reversed());
            for (Note note : chord) {
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
                if (best != null) {
                    usedStrings.add(best.string());
                    tabNotes.add(best);
                }
            }
        }
        return new TabScore(arrangement.ticksPerQuarter(), tuning, tabNotes);
    }
}
