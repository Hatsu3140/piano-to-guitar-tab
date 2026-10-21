package io.github.hatsu3140.pianotab.converter.fret;

import io.github.hatsu3140.pianotab.converter.model.TabNote;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 左手の押さえ方（運指）を実際に決めて、弾けるかどうかを判定する。
 *
 * <p>同時に鳴っている音それぞれに、人差し指(1)・中指(2)・薬指(3)・小指(4) のどれで押さえるか
 * （開放弦なら指はいらない）を割り当てる。次のルールをすべて守れる割り当てがあれば弾ける。
 * <ul>
 *   <li>1本の弦で鳴らせるのは1音だけ</li>
 *   <li>押さえるフレットの幅（開放弦は除く）が上限以内</li>
 *   <li>使う指の本数が上限以内。セーハは1本と数える</li>
 *   <li>指の順番を守る: 番号の大きい指ほど同じかより高いフレットを押さえる</li>
 *   <li>指どうしの開き: 2本の指のフレットの差は「指の番号の差 + 1」まで。たとえば中指と薬指は
 *       2フレットまで、人差し指と小指は4フレットまで離せる。ただし5フレットより下はフレットの幅が
 *       広いので、小指は1フレット少なくしか開けない（1フレット付近の人差し指と小指は3フレットまで）</li>
 *   <li>同じフレットを別の指で押さえるときは、弦の差が「指の番号の差 + 1」まで（6弦と1弦を
 *       人差し指と中指で同じフレット、のような形はできない）</li>
 *   <li>1本の指で押さえるのは1か所だけ。ただし人差し指はセーハできる</li>
 * </ul>
 *
 * <p>セーハ（人差し指を寝かせて複数の弦をまとめて押さえる）は、
 * <ul>
 *   <li>押さえる中で一番低いフレットでだけ使える</li>
 *   <li>1弦から、セーハで押さえる一番太い弦までをまとめて押さえる。その範囲に開放弦の音があると使えない</li>
 *   <li>セーハしたまま他の指で押さえられるのは、セーハから2フレット先まで（F や B♭ のような形）</li>
 * </ul>
 * フレットはカポを基準に数える。
 */
public class Hand {

    private final int capo;
    private final int maxStretch;
    private final int maxFingers;
    private final boolean barre;

    /** セーハしたまま他の指で押さえられる、セーハからのフレットの差 */
    static final int BARRE_REACH = 2;
    /** 左手で押さえるのに使える指の数 */
    public static final int MAX_FINGERS = Fingering.PINKY;
    /** このフレット（ネックの実際のフレット）より下では、小指を1フレット少なくしか開けない */
    static final int WIDE_SPREAD_FROM = 5;

    public Hand(int capo, int maxStretch, int maxFingers, boolean barre) {
        this.capo = capo;
        this.maxStretch = maxStretch;
        this.maxFingers = maxFingers;
        this.barre = barre;
    }

    public boolean canPlay(List<TabNote> notes) {
        return fingering(notes).isPresent();
    }

    /**
     * 押さえ方を決める。弾けなければ空。
     * セーハしないで押さえられるならそれを、無理ならセーハする押さえ方を選ぶ。どちらも指の少ない方を選ぶ。
     */
    public Optional<Fingering> fingering(List<TabNote> notes) {
        return fingerings(notes).stream().min(PREFERENCE);
    }

    /** よい押さえ方の順: セーハしない方、指の少ない方 */
    static final Comparator<Fingering> PREFERENCE = Comparator.comparing(Fingering::usesBarre)
            .thenComparingInt(Fingering::fingerCount);

    /** ルールを守れる押さえ方をすべて挙げる。弾けなければ空 */
    public List<Fingering> fingerings(List<TabNote> notes) {
        Set<Integer> strings = new HashSet<>();
        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;
        List<TabNote> fretted = new ArrayList<>();
        for (TabNote note : notes) {
            if (!strings.add(note.string())) {
                return List.of();
            }
            int fret = relativeFret(note);
            if (fret < 0) {
                return List.of();
            }
            if (fret > 0) {
                fretted.add(note);
                lowest = Math.min(lowest, fret);
                highest = Math.max(highest, fret);
            }
        }
        if (!fretted.isEmpty() && highest - lowest > maxStretch) {
            return List.of();
        }
        // 低いフレットから順に指を決めていく（同じフレットなら太い弦から）
        fretted.sort(Comparator.comparingInt(this::relativeFret)
                .thenComparing(Comparator.comparingInt(TabNote::string).reversed()));
        List<Fingering> found = new ArrayList<>();
        new Search(notes, fretted, false, found).run(0);
        if (barre) {
            new Search(notes, fretted, true, found).run(0);
        }
        return found;
    }

    /** 押さえるのに必要な指の数（弾けないときは、押さえる音の数） */
    public int fingers(List<TabNote> notes) {
        return fingering(notes).map(Fingering::fingerCount)
                .orElse((int) notes.stream().filter(note -> relativeFret(note) > 0).count());
    }

    /** セーハが必要か（セーハしないと押さえられないが、セーハなら押さえられる） */
    public boolean usesBarre(List<TabNote> notes) {
        return fingering(notes).map(Fingering::usesBarre).orElse(false);
    }

    /**
     * 2本の指（lower < upper）を開けるフレットの数。基本は「指の番号の差 + 1」
     * （人差し指と中指は2フレット、人差し指と小指は4フレット）。ただしネックの根元はフレットの幅が
     * 広いので、{@link #WIDE_SPREAD_FROM} フレットより下では小指を1フレット少なくしか開けない
     * （人差し指と小指は3フレットまで）。
     */
    private int spread(int lower, int upper, int relativeFret) {
        int spread = upper - lower + 1;
        if (upper == Fingering.PINKY && relativeFret + capo < WIDE_SPREAD_FROM) {
            spread--;
        }
        return spread;
    }

    /** カポを基準にしたフレット */
    public int relativeFret(TabNote note) {
        return note.fret() - capo;
    }

    /** 押さえる音に指を1つずつ割り当てて、ルールを守れる割り当てを全部探す */
    private final class Search {
        private final List<TabNote> notes;
        private final List<TabNote> fretted;
        private final boolean useBarre;
        private final int[] assigned;
        /** 指ごとに押さえているフレット（0 = まだ使っていない） */
        private final int[] fingerFret = new int[Fingering.PINKY + 1];
        private final int[] fingerUses = new int[Fingering.PINKY + 1];
        /** 指ごとに、最初に押さえた弦 */
        private final int[] fingerString = new int[Fingering.PINKY + 1];
        private final List<Fingering> found;

        Search(List<TabNote> notes, List<TabNote> fretted, boolean useBarre, List<Fingering> found) {
            this.notes = notes;
            this.fretted = fretted;
            this.useBarre = useBarre;
            this.found = found;
            this.assigned = new int[fretted.size()];
        }

        void run(int index) {
            if (index == fretted.size()) {
                consider();
                return;
            }
            int fret = relativeFret(fretted.get(index));
            int string = fretted.get(index).string();
            for (int finger = Fingering.INDEX; finger <= Fingering.PINKY; finger++) {
                if (canPlace(finger, fret, string)) {
                    boolean first = fingerUses[finger] == 0;
                    if (first) {
                        fingerString[finger] = string;
                    }
                    fingerFret[finger] = fret;
                    fingerUses[finger]++;
                    assigned[index] = finger;
                    run(index + 1);
                    fingerUses[finger]--;
                    if (first) {
                        fingerFret[finger] = 0;
                    }
                }
            }
        }

        private boolean canPlace(int finger, int fret, int string) {
            if (fingerUses[finger] > 0) {
                // 同じ指でもう1か所押さえられるのは、人差し指のセーハ（同じフレット）だけ
                return useBarre && finger == Fingering.INDEX && fingerFret[finger] == fret;
            }
            for (int other = Fingering.INDEX; other <= Fingering.PINKY; other++) {
                if (other == finger || fingerUses[other] == 0) {
                    continue;
                }
                int gap = fret - fingerFret[other];
                int fingerGap = finger - other;
                // 指の順番（番号の大きい指ほど高いフレット）と、指どうしの開き
                int spread = spread(Math.min(finger, other), Math.max(finger, other),
                        Math.min(fret, fingerFret[other]));
                if (fingerGap > 0 ? gap < 0 || gap > spread : gap > 0 || -gap > spread) {
                    return false;
                }
                // 同じフレットを別の指で押さえるときは、間の弦を空けすぎない
                if (gap == 0 && Math.abs(string - fingerString[other]) > Math.abs(fingerGap) + 1) {
                    return false;
                }
            }
            return true;
        }

        private void consider() {
            int count = 0;
            for (int finger = Fingering.INDEX; finger <= Fingering.PINKY; finger++) {
                if (fingerUses[finger] > 0) {
                    count++;
                }
            }
            if (count > maxFingers) {
                return;
            }
            int barreFret = 0;
            int barreTo = 0;
            if (fingerUses[Fingering.INDEX] > 1) {
                barreFret = fingerFret[Fingering.INDEX];
                for (int i = 0; i < fretted.size(); i++) {
                    if (assigned[i] == Fingering.INDEX) {
                        barreTo = Math.max(barreTo, fretted.get(i).string());
                    }
                }
                if (!barreIsPossible(barreFret, barreTo)) {
                    return;
                }
            } else if (useBarre) {
                return; // セーハしない押さえ方は、もう調べてある
            }
            List<Fingering.Placement> placements = new ArrayList<>();
            for (TabNote note : notes) {
                int index = fretted.indexOf(note);
                placements.add(new Fingering.Placement(note, index < 0 ? 0 : assigned[index]));
            }
            found.add(new Fingering(List.copyOf(placements), barreFret, barreTo));
        }

        /** セーハは一番低いフレットで、1弦から barreTo 弦までに開放弦がなく、他の指が届く範囲にある */
        private boolean barreIsPossible(int barreFret, int barreTo) {
            for (TabNote note : notes) {
                int fret = relativeFret(note);
                if (note.string() <= barreTo && fret < barreFret) {
                    return false; // セーハすると開放弦（やセーハより低い音）が鳴らない
                }
                if (fret > barreFret + BARRE_REACH) {
                    return false;
                }
            }
            return true;
        }
    }
}
