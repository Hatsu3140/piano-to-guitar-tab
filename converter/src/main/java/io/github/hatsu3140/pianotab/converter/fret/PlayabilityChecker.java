package io.github.hatsu3140.pianotab.converter.fret;

import io.github.hatsu3140.pianotab.converter.model.TabNote;
import io.github.hatsu3140.pianotab.converter.model.TabScore;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * できあがった TAB 譜が本当に弾けるかを、音が鳴り始めるタイミングごとに確かめる。
 * そのとき鳴っているすべての音（メロディ・ベース・内声）を {@link Hand} で判定する。
 */
public class PlayabilityChecker {

    private final Hand hand;

    public PlayabilityChecker(Hand hand) {
        this.hand = hand;
    }

    /** 結果 */
    public record Result(int unplayablePlaces, int barres, List<Long> unplayableTicks) {
    }

    public Result check(TabScore tab) {
        List<TabNote> all = tab.notes();
        TreeSet<Long> starts = new TreeSet<>();
        all.forEach(note -> starts.add(note.note().startTick()));
        List<Long> unplayable = new ArrayList<>();
        int barres = 0;
        for (long tick : starts) {
            List<TabNote> sounding = new ArrayList<>();
            for (TabNote note : all) {
                long start = note.note().startTick();
                if (start <= tick && tick < start + note.note().durationTicks()) {
                    sounding.add(note);
                }
            }
            if (!hand.canPlay(sounding)) {
                unplayable.add(tick);
            } else if (hand.usesBarre(sounding)) {
                barres++;
            }
        }
        return new Result(unplayable.size(), barres, unplayable);
    }
}
