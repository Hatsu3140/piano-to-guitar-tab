package io.github.hatsu3140.pianotab.converter.arrange;

import io.github.hatsu3140.pianotab.converter.model.Arrangement;
import io.github.hatsu3140.pianotab.converter.model.Note;

import java.util.List;

/**
 * 手順2.5: メロディとベースを、それぞれギターで弾きやすい高さにオクターブ単位で移す。
 *
 * <p>曲のキー（調）は変えず、声部ごとに丸ごと上げ下げするだけ。
 * たとえばボーカルのメロディが高すぎる曲では、メロディだけ1オクターブ下げる。
 * どの高さが一番弾きやすいかは、範囲からはみ出す量が一番少ない移動量で決める。
 */
public class OctavePlacer {

    /** メロディが弾きやすい高さ: A3〜E5（1〜3弦の12フレットまで） */
    static final int MELODY_LOW = 57;
    static final int MELODY_HIGH = 76;
    /** ベースが弾きやすい高さ: E2〜G3（4〜6弦の低いポジション） */
    static final int BASS_LOW = 40;
    static final int BASS_HIGH = 55;

    private static final int MAX_OCTAVES = 3;

    public Arrangement place(Arrangement arrangement) {
        int melodyShift = bestShift(arrangement.melody(), MELODY_LOW, MELODY_HIGH);
        int bassShift = bestShift(arrangement.bass(), BASS_LOW, BASS_HIGH);
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
