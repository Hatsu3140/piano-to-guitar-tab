package io.github.hatsu3140.pianotab.converter.fret;

import io.github.hatsu3140.pianotab.converter.model.TabNote;
import io.github.hatsu3140.pianotab.converter.model.TabScore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * できあがった TAB 譜が本当に弾けるかを、音が鳴り始めるタイミングごとに確かめる。
 * そのとき鳴っているすべての音（メロディ・ベース・内声）の押さえ方を {@link Hand} で決める。
 *
 * <p>のばしている音は、押さえている指を途中で変えられない。そこで曲の頭から順に、
 * 「のばしている音の指を変えずに続けられる押さえ方」をつないでいき（動的計画法）、
 * つなげない所（指を入れ替えないと次の音が押さえられない所）も数える。
 *
 * <p>つなぎ方は、押さえやすさの合計が一番小さくなるものを選ぶ。
 * <ul>
 *   <li>その瞬間の押さえ方: 指が多いほど、セーハするほど難しい。よく使うコードの形なら易しい</li>
 *   <li>前の押さえ方からの動き: 指ごとに、そのまま（0）・同じ弦の上をずらすだけ（セーハごとずらすのも含む）・
 *       離して別の所に置き直す、の順に難しい。コードを押さえたままメロディを弾く所は、ほとんど動きがない</li>
 * </ul>
 */
public class PlayabilityChecker {

    /** 押さえる指1本あたり */
    private static final double FINGER_COST = 0.5;
    /** セーハは指1本でも、普通に押さえるより手が疲れるので少し避ける */
    private static final double BARRE_COST = 1.0;
    /** よく使うコードの形どおりなら易しい */
    private static final double SHAPE_BONUS = 1.0;
    /** よく使うコードの形に近い */
    private static final double SIMILAR_SHAPE_BONUS = 0.5;
    /** 指を同じ弦の上でずらす */
    private static final double SLIDE_COST = 0.3;
    /** 指を離して別の所に置き直す */
    private static final double PLACE_COST = 1.0;

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
                    if (previousCost[i] + own >= cost[j] || !keepsFingers(previous.get(i), here.get(j), held)) {
                        continue;
                    }
                    double total = previousCost[i] + own + movement(previous.get(i), here.get(j));
                    if (total < cost[j]) {
                        cost[j] = total;
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

    /** その瞬間の押さえ方の難しさ */
    static double cost(Fingering fingering) {
        double cost = fingering.fingerCount() * FINGER_COST + (fingering.usesBarre() ? BARRE_COST : 0);
        if (fingering.shape() != null) {
            cost -= fingering.shape().endsWith("に近い") ? SIMILAR_SHAPE_BONUS : SHAPE_BONUS;
        }
        return Math.max(0, cost);
    }

    /** 前の押さえ方から今の押さえ方へ、指を動かす難しさ。指ごとに足す（離すだけならかからない） */
    static double movement(Fingering before, Fingering after) {
        double total = 0;
        for (int finger = Fingering.INDEX; finger <= Fingering.PINKY; finger++) {
            Set<Long> now = positions(after, finger);
            if (now.isEmpty()) {
                continue;
            }
            Set<Long> then = positions(before, finger);
            if (then.isEmpty()) {
                total += PLACE_COST;
            } else if (!now.equals(then) && !now.stream().allMatch(then::contains)) {
                total += sameStrings(then, now) ? SLIDE_COST : PLACE_COST;
            }
        }
        return total;
    }

    /**
     * 指が押さえている所（弦 × 100 + フレット）。セーハは1弦からセーハの一番太い弦までを押さえている
     */
    private static Set<Long> positions(Fingering fingering, int finger) {
        Set<Long> result = new HashSet<>();
        if (finger == Fingering.INDEX && fingering.usesBarre()) {
            for (int string = 1; string <= fingering.barreTo(); string++) {
                result.add(string * 100L + fingering.barreFret());
            }
            return result;
        }
        for (Fingering.Placement placement : fingering.placements()) {
            if (placement.finger() == finger) {
                result.add(placement.note().string() * 100L + placement.note().fret());
            }
        }
        return result;
    }

    /** 同じ弦の上を動くだけか */
    private static boolean sameStrings(Set<Long> before, Set<Long> after) {
        Set<Long> a = new HashSet<>();
        before.forEach(position -> a.add(position / 100));
        Set<Long> b = new HashSet<>();
        after.forEach(position -> b.add(position / 100));
        return a.containsAll(b);
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
