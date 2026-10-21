package io.github.hatsu3140.pianotab.converter.fret;

import io.github.hatsu3140.pianotab.converter.model.TabNote;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 左手で押さえられるかどうかの判定。同時に鳴っている音の組み合わせについて、
 * <ul>
 *   <li>1本の弦で鳴らせるのは1音だけ</li>
 *   <li>押さえるフレットの幅（開放弦は除く）が上限以内</li>
 *   <li>押さえる指の数が上限以内。セーハ（人差し指で複数の弦をまとめて押さえる）は1本と数える</li>
 * </ul>
 * を確かめる。フレットはカポを基準に数える。
 *
 * <p>セーハは一番低いフレットでだけ使える。セーハする弦の間に開放弦の音があると、
 * その弦も押さえてしまうので使えない。
 */
public class Hand {

    private final int capo;
    private final int maxStretch;
    private final int maxFingers;
    private final boolean barre;

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
        return fingers(notes) <= maxFingers;
    }

    /** 押さえるのに必要な指の数 */
    public int fingers(List<TabNote> notes) {
        int fretted = 0;
        int lowest = Integer.MAX_VALUE;
        for (TabNote note : notes) {
            int fret = relativeFret(note);
            if (fret > 0) {
                fretted++;
                lowest = Math.min(lowest, fret);
            }
        }
        if (!barre || fretted < 2) {
            return fretted;
        }
        int atLowest = 0;
        int firstString = Integer.MAX_VALUE;
        int lastString = Integer.MIN_VALUE;
        for (TabNote note : notes) {
            if (relativeFret(note) == lowest) {
                atLowest++;
                firstString = Math.min(firstString, note.string());
                lastString = Math.max(lastString, note.string());
            }
        }
        if (atLowest < 2) {
            return fretted;
        }
        for (TabNote note : notes) {
            if (relativeFret(note) == 0 && note.string() > firstString && note.string() < lastString) {
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
