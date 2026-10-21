package io.github.hatsu3140.pianotab.converter.fret;

import io.github.hatsu3140.pianotab.converter.model.TabNote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 左手で押さえられるかどうかの判定。同時に鳴っている音の組み合わせについて、
 * <ul>
 *   <li>1本の弦で鳴らせるのは1音だけ</li>
 *   <li>押さえるフレットの幅（開放弦は除く）が上限以内</li>
 *   <li>押さえる指の数が上限以内。セーハ（人差し指で複数の弦をまとめて押さえる）は1本と数える</li>
 *   <li>指を実際に割り当てられる。フレットの低い順に人差し指・中指・薬指・小指を（飛ばしてもよいが順番は守って）
 *       割り当て、隣どうしに割り当てた指のフレットの差が「指の番号の差 + 1」以内であること。
 *       たとえば中指と薬指は2フレットまで、人差し指と小指は4フレットまで離せる</li>
 * </ul>
 * を確かめる。フレットはカポを基準に数える。
 *
 * <p>セーハは一番低いフレットでだけ使える。人差し指を寝かせて押さえるので、セーハは1弦から
 * 一番太いセーハの弦までをまとめて押さえることになる。そのため、
 * <ul>
 *   <li>その範囲に開放弦の音があると使えない（押さえてしまって鳴らない）</li>
 *   <li>セーハしたまま他の指で押さえられるのは、セーハから2フレット先まで
 *       （F や B♭ のような普通のセーハの形）</li>
 * </ul>
 */
public class Hand {

    private final int capo;
    private final int maxStretch;
    private final int maxFingers;
    private final boolean barre;

    /** セーハしたまま他の指で押さえられる、セーハからのフレットの差 */
    static final int BARRE_REACH = 2;
    private static final int FINGER_COUNT = 4;

    public Hand(int capo, int maxStretch, int maxFingers, boolean barre) {
        this.capo = capo;
        this.maxStretch = maxStretch;
        this.maxFingers = maxFingers;
        this.barre = barre;
    }

    public boolean canPlay(List<TabNote> notes) {
        Set<Integer> strings = new HashSet<>();
        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;
        for (TabNote note : notes) {
            if (!strings.add(note.string())) {
                return false;
            }
            int fret = relativeFret(note);
            if (fret < 0) {
                return false;
            }
            if (fret > 0) {
                lowest = Math.min(lowest, fret);
                highest = Math.max(highest, fret);
            }
        }
        if (highest - lowest > maxStretch) {
            return false;
        }
        if (canAssignFingers(fingerFrets(notes, false))) {
            return true;
        }
        return barre && canAssignFingers(fingerFrets(notes, true));
    }

    /** 指で押さえる場所のフレット（低い順）。useBarre なら、セーハできるときはセーハを1か所にまとめる */
    private List<Integer> fingerFrets(List<TabNote> notes, boolean useBarre) {
        List<Integer> frets = new ArrayList<>();
        boolean barred = useBarre && fingers(notes) < frettedCount(notes);
        int lowest = Integer.MAX_VALUE;
        for (TabNote note : notes) {
            if (relativeFret(note) > 0) {
                lowest = Math.min(lowest, relativeFret(note));
            }
        }
        if (barred) {
            frets.add(lowest);
        }
        for (TabNote note : notes) {
            int fret = relativeFret(note);
            if (fret > 0 && !(barred && fret == lowest)) {
                frets.add(fret);
            }
        }
        Collections.sort(frets);
        return frets;
    }

    /** 低い順のフレットに、人差し指(1)〜小指(4)を順番を守って割り当てられるか */
    private boolean canAssignFingers(List<Integer> frets) {
        if (frets.size() > Math.min(maxFingers, FINGER_COUNT)) {
            return false;
        }
        return assign(frets, 0, 0, 0);
    }

    private static boolean assign(List<Integer> frets, int index, int previousFinger, int previousFret) {
        if (index == frets.size()) {
            return true;
        }
        int remaining = frets.size() - index;
        for (int finger = previousFinger + 1; finger <= FINGER_COUNT - remaining + 1; finger++) {
            int fret = frets.get(index);
            boolean reachable = previousFinger == 0 || fret - previousFret <= finger - previousFinger + 1;
            if (reachable && assign(frets, index + 1, finger, fret)) {
                return true;
            }
        }
        return false;
    }

    private int frettedCount(List<TabNote> notes) {
        return (int) notes.stream().filter(note -> relativeFret(note) > 0).count();
    }

    /** 押さえるのに必要な指の数 */
    public int fingers(List<TabNote> notes) {
        int fretted = 0;
        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;
        for (TabNote note : notes) {
            int fret = relativeFret(note);
            if (fret > 0) {
                fretted++;
                lowest = Math.min(lowest, fret);
                highest = Math.max(highest, fret);
            }
        }
        if (!barre || fretted < 2 || highest - lowest > BARRE_REACH) {
            return fretted;
        }
        int atLowest = 0;
        int thickestString = Integer.MIN_VALUE;
        for (TabNote note : notes) {
            if (relativeFret(note) == lowest) {
                atLowest++;
                thickestString = Math.max(thickestString, note.string());
            }
        }
        if (atLowest < 2) {
            return fretted;
        }
        for (TabNote note : notes) {
            if (relativeFret(note) == 0 && note.string() < thickestString) {
                return fretted; // セーハすると開放弦が鳴らなくなる
            }
        }
        return fretted - atLowest + 1;
    }

    /** セーハが必要か（1本ずつ押さえると指が足りないが、セーハなら足りる） */
    public boolean usesBarre(List<TabNote> notes) {
        return barre && new Hand(capo, maxStretch, maxFingers, false).fingers(notes) > maxFingers
                && fingers(notes) <= maxFingers;
    }

    /** カポを基準にしたフレット */
    public int relativeFret(TabNote note) {
        return note.fret() - capo;
    }
}
