package io.github.hatsu3140.pianotab.converter.fret;

import io.github.hatsu3140.pianotab.converter.model.TabNote;
import io.github.hatsu3140.pianotab.converter.model.TabScore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * できあがった TAB 譜が本当に弾けるかを、音が鳴り始めるタイミングごとに確かめる。
 * そのとき鳴っているすべての音（メロディ・ベース・内声）の押さえ方を {@link Hand} で決める。
 *
 * <p>のばしている音は、押さえている指を途中で変えられない。そこで曲の頭から順に、
 * 「のばしている音の指を変えずに続けられる押さえ方」をつないでいき（動的計画法）、
 * つなげない所（指を入れ替えないと次の音が押さえられない所）も数える。
 */
public class PlayabilityChecker {

    /** セーハは指1本でも、普通に押さえるより手が疲れるので少し避ける */
    private static final double BARRE_COST = 2.0;

    private final Hand hand;

    public PlayabilityChecker(Hand hand) {
        this.hand = hand;
    }

    /**
     * 結果
     *
     * @param unplayablePlaces どう押さえても弾けない所の数
     * @param barres           セーハを使う所の数
     * @param unplayableTicks  どう押さえても弾けない所
     * @param fingerSwapTicks  のばしている音の指を入れ替えないと押さえられない所
     * @param fingerings       タイミングごとに決めた押さえ方
     */
    public record Result(int unplayablePlaces, int barres, List<Long> unplayableTicks, List<Long> fingerSwapTicks,
                         Map<Long, Fingering> fingerings) {
    }

    public Result check(TabScore tab) {
        List<TabNote> all = tab.notes();
        TreeSet<Long> starts = new TreeSet<>();
        all.forEach(note -> starts.add(note.note().startTick()));
        Long[] ticks = starts.toArray(Long[]::new);

        List<Long> unplayable = new ArrayList<>();
        List<Long> swaps = new ArrayList<>();
        List<List<Fingering>> options = new ArrayList<>();
        List<int[]> back = new ArrayList<>();
        double[] previousCost = null;
        List<Fingering> previous = null;
        for (long tick : ticks) {
            List<TabNote> sounding = new ArrayList<>();
            List<TabNote> held = new ArrayList<>();
            for (TabNote note : all) {
                long start = note.note().startTick();
                if (start <= tick && tick < start + note.note().durationTicks()) {
                    sounding.add(note);
                    if (start < tick) {
                        held.add(note);
                    }
                }
            }
            List<Fingering> here = hand.fingerings(sounding);
            int[] from = new int[here.size()];
            double[] cost = new double[here.size()];
            Arrays.fill(from, -1);
            Arrays.fill(cost, Double.POSITIVE_INFINITY);
            if (here.isEmpty()) {
                unplayable.add(tick);
            }
            for (int j = 0; j < here.size(); j++) {
                double own = cost(here.get(j));
                if (previous == null) {
                    cost[j] = own;
                    continue;
                }
                for (int i = 0; i < previous.size(); i++) {
                    if (previousCost[i] + own < cost[j] && keepsFingers(previous.get(i), here.get(j), held)) {
                        cost[j] = previousCost[i] + own;
                        from[j] = i;
                    }
                }
            }
            if (previous != null && !here.isEmpty() && Arrays.stream(cost).allMatch(Double::isInfinite)) {
                swaps.add(tick); // 指を入れ替えるしかない。ここからつなぎ直す
                for (int j = 0; j < here.size(); j++) {
                    cost[j] = cost(here.get(j));
                }
            }
            options.add(here);
            back.add(from);
            previous = here.isEmpty() ? null : here;
            previousCost = here.isEmpty() ? null : cost;
        }

        // 後ろからたどって、タイミングごとの押さえ方を決める
        Map<Long, Fingering> chosen = new LinkedHashMap<>();
        int index = -1;
        for (int k = ticks.length - 1; k >= 0; k--) {
            List<Fingering> here = options.get(k);
            if (here.isEmpty()) {
                index = -1;
                continue;
            }
            if (index < 0) {
                index = cheapest(here);
            }
            chosen.put(ticks[k], here.get(index));
            index = back.get(k)[index];
        }
        int barres = (int) chosen.values().stream().filter(Fingering::usesBarre).count();
        Map<Long, Fingering> ordered = new LinkedHashMap<>();
        for (long tick : ticks) {
            if (chosen.containsKey(tick)) {
                ordered.put(tick, chosen.get(tick));
            }
        }
        return new Result(unplayable.size(), barres, unplayable, swaps, ordered);
    }

    private static double cost(Fingering fingering) {
        return fingering.fingerCount() + (fingering.usesBarre() ? BARRE_COST : 0);
    }

    private static int cheapest(List<Fingering> fingerings) {
        int best = 0;
        for (int j = 1; j < fingerings.size(); j++) {
            if (cost(fingerings.get(j)) < cost(fingerings.get(best))) {
                best = j;
            }
        }
        return best;
    }

    /** のばしている音を、前と同じ指で押さえ続けているか */
    private static boolean keepsFingers(Fingering before, Fingering after, List<TabNote> held) {
        for (TabNote note : held) {
            int finger = before.fingerOf(note);
            if (finger >= 0 && finger != after.fingerOf(note)) {
                return false;
            }
        }
        return true;
    }
}
