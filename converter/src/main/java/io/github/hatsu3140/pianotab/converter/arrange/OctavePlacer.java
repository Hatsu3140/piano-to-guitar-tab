package io.github.hatsu3140.pianotab.converter.arrange;

import io.github.hatsu3140.pianotab.converter.model.Arrangement;
import io.github.hatsu3140.pianotab.converter.model.Note;
import io.github.hatsu3140.pianotab.converter.model.Tuning;

import java.util.List;

/**
 * 手順2.5: メロディとベースを、それぞれギターで弾きやすい高さにオクターブ単位で移す。
 *
 * <p>曲のキー（調）は変えず、声部ごとに丸ごと上げ下げするだけ。
 * たとえばボーカルのメロディが高すぎる曲では、メロディだけ1オクターブ下げる。
 * どの高さが一番弾きやすいかは、範囲からはみ出す量が一番少ない移動量で決める。
 *
 * <p>弾きやすい範囲はチューニング・カポ・使うフレットの上限から決める。
 * メロディは「1弦の一番高いフレット」から下の約1オクターブ半、ベースは「一番低い開放弦」から上の
 * 1オクターブ少々（レギュラー・カポなし・12フレットまでなら、メロディ A3〜E5、ベース E2〜G3）。
 */
public class OctavePlacer {

    /** メロディの範囲の幅（半音） */
    static final int MELODY_SPAN = 19;
    /** ベースの範囲の幅（半音） */
    static final int BASS_SPAN = 15;

    private static final int MAX_OCTAVES = 3;

    private final int melodyLow;
    private final int melodyHigh;
    private final int bassLow;
    private final int bassHigh;

    /** レギュラーチューニング・カポなし・12フレットまで */
    public OctavePlacer() {
        this(Tuning.STANDARD, 0, 12);
    }

    /**
     * @param maxFret 使う一番高いフレット（カポ基準）
     */
    public OctavePlacer(Tuning tuning, int capo, int maxFret) {
        this.melodyHigh = tuning.highestPitch() + capo + maxFret;
        this.melodyLow = melodyHigh - MELODY_SPAN;
        this.bassLow = tuning.lowestPitch() + capo;
        this.bassHigh = bassLow + BASS_SPAN;
    }

    public Arrangement place(Arrangement arrangement) {
        int melodyShift = bestShift(arrangement.melody(), melodyLow, melodyHigh);
        int bassShift = bestShift(arrangement.bass(), bassLow, bassHigh);
        return new Arrangement(arrangement.info(),
                shift(arrangement.melody(), melodyShift),
                shift(arrangement.bass(), bassShift));
    }

    /** はみ出す量が一番少ない移動量（半音単位、12の倍数）。同じなら移動の少ない方を選ぶ */
    static int bestShift(List<Note> notes, int low, int high) {
        int best = 0;
        long bestCost = outOfRange(notes, 0, low, high);
        for (int octaves = 1; octaves <= MAX_OCTAVES; octaves++) {
            for (int shift : new int[] {-12 * octaves, 12 * octaves}) {
                long cost = outOfRange(notes, shift, low, high);
                if (cost < bestCost) {
                    best = shift;
                    bestCost = cost;
                }
            }
        }
        return best;
    }

    private static long outOfRange(List<Note> notes, int shift, int low, int high) {
        long total = 0;
        for (Note note : notes) {
            int pitch = note.pitch() + shift;
            total += Math.max(0, low - pitch) + Math.max(0, pitch - high);
        }
        return total;
    }

    private static List<Note> shift(List<Note> notes, int shift) {
        return notes.stream()
                .map(note -> new Note(note.pitch() + shift, note.startTick(), note.durationTicks()))
                .toList();
    }
}
