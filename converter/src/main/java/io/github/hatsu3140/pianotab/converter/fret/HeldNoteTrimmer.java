package io.github.hatsu3140.pianotab.converter.fret;

import io.github.hatsu3140.pianotab.converter.model.Note;
import io.github.hatsu3140.pianotab.converter.model.TabNote;
import io.github.hatsu3140.pianotab.converter.model.TabScore;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * のばしている音の指を入れ替えないと次の音が押さえられない所で、のばしている音をそこで止める。
 * 先にベース（と内声）を止め、それでも押さえられなければメロディも止める。
 */
public class HeldNoteTrimmer {

    private final PlayabilityChecker checker;

    public HeldNoteTrimmer(PlayabilityChecker checker) {
        this.checker = checker;
    }

    /**
     * 結果
     *
     * @param tabScore    止めたあとの TAB 譜
     * @param trimmedNotes 途中で止めた音の数
     */
    public record Result(TabScore tabScore, int trimmedNotes) {
    }

    public Result trim(TabScore tab) {
        int trimmed = 0;
        for (boolean melodyToo : new boolean[] {false, true}) {
            List<Long> swaps = checker.check(tab).fingerSwapTicks();
            if (swaps.isEmpty()) {
                break;
            }
            TreeSet<Long> at = new TreeSet<>(swaps);
            List<TabNote> bass = new ArrayList<>();
            List<TabNote> inner = new ArrayList<>();
            List<TabNote> melody = new ArrayList<>();
            trimmed += cut(tab.bass(), at, bass) + cut(tab.inner(), at, inner);
            if (melodyToo) {
                trimmed += cut(tab.melody(), at, melody);
            } else {
                melody.addAll(tab.melody());
            }
            tab = new TabScore(tab.info(), tab.tuning(), tab.capo(), melody, bass, inner, tab.strokes());
        }
        return new Result(tab, trimmed);
    }

    /** ticks のどれかをまたいで鳴っている音を、最初にまたぐ所で止める */
    private static int cut(List<TabNote> notes, TreeSet<Long> ticks, List<TabNote> result) {
        int count = 0;
        for (TabNote note : notes) {
            long start = note.note().startTick();
            Long cutAt = ticks.higher(start);
            if (cutAt != null && cutAt < start + note.note().durationTicks()) {
                Note shorter = new Note(note.note().pitch(), start, cutAt - start);
                result.add(new TabNote(shorter, note.string(), note.fret()));
                count++;
            } else {
                result.add(note);
            }
        }
        return count;
    }
}
