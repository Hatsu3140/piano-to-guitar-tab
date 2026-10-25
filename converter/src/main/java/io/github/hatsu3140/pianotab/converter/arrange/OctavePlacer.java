package io.github.hatsu3140.pianotab.converter.arrange;

import io.github.hatsu3140.pianotab.converter.model.Arrangement;
import io.github.hatsu3140.pianotab.converter.model.Note;
import io.github.hatsu3140.pianotab.converter.model.Tuning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
 *
 * <p>それでも範囲からはみ出す音があるときは、その音だけを動かすとメロディの形が崩れる
 * （たとえば「ラ→ソ」が7度の跳躍になる）ので、前後のまとまり（なるべく休符で区切れるところまで）を
 * いっしょにオクターブ移動する。
 *
 * <p>移動した結果、同じ瞬間に鳴っているベースよりメロディが低くなったら（もとはメロディの方が高かったのに）、
 * メロディが埋もれないよう、そのベースを1オクターブ下げる。下げられなければメロディを1オクターブ上げ、
 * それも無理ならそのベースを省く。もとの楽譜でメロディがベースより低い箇所はそのままにする。
 */
public class OctavePlacer {

    /** メロディの範囲の幅（半音） */
    static final int MELODY_SPAN = 19;
    /** ベースの範囲の幅（半音） */
    static final int BASS_SPAN = 15;

    private static final int MAX_OCTAVES = 3;
    /** メロディの範囲の下限を決めるときに使う、1弦のフレット */
    private static final int RANGE_TOP_FRET = 12;
    /** 弾きやすいメロディの高さの目安（1弦のこのフレットまで） */
    static final int COMFORTABLE_FRET = 9;
    /** 範囲からはみ出すのは、弾きやすい高さを超えるより10倍悪い */
    private static final int RANGE_WEIGHT = 10;
    private static final double OUT_OF_RANGE_COST = 10.0;
    /**
     * メロディが範囲より少し（4半音まで）低くなるときの減点。少し低いだけならギターでは弾けるので軽く見る
     * （フレーズの途中でオクターブを変えるより、フレーズごと少し低めに置く方がよい）。それより低いと大きく減点
     */
    private static final int MELODY_SOFT_BELOW = 4;
    private static final double MELODY_BELOW_COST = 2.0;
    /** ギターで出せない高さ（押さえられない）は、ほぼ禁止 */
    private static final double UNPLAYABLE_COST = 1000.0;
    /** フレーズの途中でオクターブを切り替えるのは、よほどのことがない限りしない */
    private static final double SWITCH_COST = 100.0;
    private static final double PHRASE_SWITCH_COST = 1.0;
    /** 小節の頭での切り替えは、フレーズの途中よりはまし */
    private static final double BAR_SWITCH_COST = 20.0;

    private final int melodyLow;
    private final int melodyHigh;
    /** メロディを置きたい高さの上限。高いフレットまで使える上級でも、なるべくこの下に置く */
    private final int comfortableHigh;
    private final int bassLow;
    private final int bassHigh;
    /** ギターで出せる一番低い音（一番低い開放弦） */
    private final int playableLow;

    /** レギュラーチューニング・カポなし・12フレットまで */
    public OctavePlacer() {
        this(Tuning.STANDARD, 0, 12);
    }

    /**
     * @param maxFret 使う一番高いフレット（カポ基準）
     */
    public OctavePlacer(Tuning tuning, int capo, int maxFret) {
        this.melodyHigh = tuning.highestPitch() + capo + maxFret;
        this.comfortableHigh = tuning.highestPitch() + capo + Math.min(maxFret, COMFORTABLE_FRET);
        // 範囲の下限は、12フレットまでを使うときと同じにする。上級のように高いフレットまで使えても、
        // 下限まで上がると低めのフレーズを無理に上げてしまうため
        this.melodyLow = tuning.highestPitch() + capo + Math.min(maxFret, RANGE_TOP_FRET) - MELODY_SPAN;
        this.bassLow = tuning.lowestPitch() + capo;
        this.bassHigh = bassLow + BASS_SPAN;
        this.playableLow = bassLow;
    }

    public Arrangement place(Arrangement arrangement) {
        int melodyShift = bestShift(arrangement.melody(), melodyLow, melodyHigh, comfortableHigh);
        int bassShift = bestShift(arrangement.bass(), bassLow, bassHigh);
        int ticksPerQuarter = arrangement.info().ticksPerQuarter();
        Set<Long> barStarts = new HashSet<>();
        arrangement.info().measures().forEach(measure -> barStarts.add(measure.startTick()));
        List<Note> melody = foldSmoothly(shift(arrangement.melody(), melodyShift), ticksPerQuarter, barStarts,
                new Range(melodyLow, melodyHigh, comfortableHigh, playableLow, melodyHigh, MELODY_SOFT_BELOW));
        List<Note> bass = foldSmoothly(shift(arrangement.bass(), bassShift), ticksPerQuarter, barStarts,
                new Range(bassLow, bassHigh, bassHigh, playableLow, melodyHigh, 0));
        if (melodyShift != bassShift) {
            keepMelodyAboveBass(arrangement.melody(), melody, arrangement.bass(), bass);
        }
        return new Arrangement(arrangement.info(), melody, bass.stream().filter(note -> note != null).toList(),
                arrangement.harmony());
    }

    /**
     * 声部の高さの範囲。
     *
     * @param low             置きたい範囲の下限
     * @param high            置きたい範囲の上限
     * @param comfortableHigh 弾きやすい高さの上限（超えると軽く減点）
     * @param playableLow     ギターで出せる一番低い音
     * @param playableHigh    ギターで出せる一番高い音（使うフレットの上限まで）
     * @param softBelow       low より低くても、ここまで（半音）なら軽い減点ですませる
     */
    record Range(int low, int high, int comfortableHigh, int playableLow, int playableHigh, int softBelow) {
    }

    /**
     * 範囲からはみ出す音を、メロディの形をなるべく崩さずにオクターブ移動する。
     * 音ごとに「何オクターブずらすか」を DP で決める。はみ出すのは大きな減点で、ギターで出せない高さはほぼ禁止。
     * 前の音とずらし方が変わる（音の間隔が変わる）のも大きな減点で、切り替えてよいのはフレーズの区切り
     * （4分音符以上の休符、または2分音符以上の長い音のあと）だけにする。
     * そのため、ある音が範囲からはみ出すときは、その音だけでなくフレーズごと移す。
     * 弾きやすい高さ（comfortableHigh）を超えるのも軽く減点するので、高すぎるフレーズは丸ごと下がる。
     */
    static List<Note> foldSmoothly(List<Note> notes, int ticksPerQuarter, Set<Long> barStarts, Range range) {
        int count = notes.size();
        if (count == 0) {
            return new ArrayList<>(notes);
        }
        Integer[] order = new Integer[count];
        for (int i = 0; i < count; i++) {
            order[i] = i;
        }
        Arrays.sort(order, Comparator.comparingLong(i -> notes.get(i).startTick()));
        int[] shifts = {0, -12, 12, -24, 24};
        double[][] cost = new double[count][shifts.length];
        int[][] previous = new int[count][shifts.length];
        for (int n = 0; n < count; n++) {
            Note note = notes.get(order[n]);
            double switchCost = SWITCH_COST;
            if (n > 0) {
                Note before = notes.get(order[n - 1]);
                long gap = note.startTick() - (before.startTick() + before.durationTicks());
                // 次の音が鳴り始めるまでの長さ（のばしたまま次の音を弾く音は、そこで区切れていない）
                long heard = Math.min(before.durationTicks(), note.startTick() - before.startTick());
                if (gap >= ticksPerQuarter || heard >= ticksPerQuarter * 2L) {
                    switchCost = PHRASE_SWITCH_COST;
                } else if (barStarts.contains(note.startTick())) {
                    switchCost = BAR_SWITCH_COST;
                }
            }
            for (int s = 0; s < shifts.length; s++) {
                int pitch = note.pitch() + shifts[s];
                int below = Math.max(0, range.low() - pitch);
                double own = Math.min(below, range.softBelow()) * MELODY_BELOW_COST
                        + Math.max(0, below - range.softBelow()) * OUT_OF_RANGE_COST
                        + Math.max(0, pitch - range.high()) * OUT_OF_RANGE_COST
                        + (Math.max(0, range.playableLow() - pitch) + Math.max(0, pitch - range.playableHigh()))
                        * UNPLAYABLE_COST
                        + Math.max(0, pitch - range.comfortableHigh()) * OUT_OF_RANGE_COST / RANGE_WEIGHT
                        + Math.abs(shifts[s]) / 12 * 0.01;
                if (n == 0) {
                    cost[n][s] = own;
                    continue;
                }
                cost[n][s] = Double.POSITIVE_INFINITY;
                for (int t = 0; t < shifts.length; t++) {
                    double total = cost[n - 1][t] + own + (s == t ? 0 : switchCost);
                    if (total < cost[n][s]) {
                        cost[n][s] = total;
                        previous[n][s] = t;
                    }
                }
            }
        }
        int best = 0;
        for (int s = 1; s < shifts.length; s++) {
            if (cost[count - 1][s] < cost[count - 1][best]) {
                best = s;
            }
        }
        List<Note> result = new ArrayList<>(notes);
        for (int n = count - 1; n >= 0; n--) {
            Note note = notes.get(order[n]);
            result.set(order[n], withPitch(note, note.pitch() + shifts[best]));
            best = previous[n][best];
        }
        return result;
    }

    /** 移動でメロディがベースの下に潜った箇所を直す（original* は移動前） */
    private void keepMelodyAboveBass(List<Note> originalMelody, List<Note> melody,
                                     List<Note> originalBass, List<Note> bass) {
        for (int m = 0; m < melody.size(); m++) {
            for (int b = 0; b < bass.size(); b++) {
                Note bassNote = bass.get(b);
                Note melodyNote = melody.get(m);
                if (bassNote == null || !overlaps(melodyNote, bassNote) || melodyNote.pitch() > bassNote.pitch()
                        || originalMelody.get(m).pitch() <= originalBass.get(b).pitch()) {
                    continue;
                }
                if (bassNote.pitch() - 12 >= bassLow) {
                    bass.set(b, withPitch(bassNote, bassNote.pitch() - 12));
                } else if (melodyNote.pitch() + 12 <= melodyHigh) {
                    melody.set(m, withPitch(melodyNote, melodyNote.pitch() + 12));
                } else {
                    bass.set(b, null);
                }
            }
        }
    }

    private static boolean overlaps(Note a, Note b) {
        return a.startTick() < b.startTick() + b.durationTicks() && b.startTick() < a.startTick() + a.durationTicks();
    }

    private static Note withPitch(Note note, int pitch) {
        return new Note(pitch, note.startTick(), note.durationTicks());
    }

    /** はみ出す量が一番少ない移動量（半音単位、12の倍数）。同じなら移動の少ない方を選ぶ */
    static int bestShift(List<Note> notes, int low, int high) {
        return bestShift(notes, low, high, high);
    }

    /**
     * low〜high からはみ出す量が一番少ない移動量。comfortableHigh を超える量も（軽く）減点し、
     * 収まる高さが複数あるときは弾きやすい低めの方を選ぶ
     */
    static int bestShift(List<Note> notes, int low, int high, int comfortableHigh) {
        int best = 0;
        long bestCost = shiftCost(notes, 0, low, high, comfortableHigh);
        for (int octaves = 1; octaves <= MAX_OCTAVES; octaves++) {
            for (int shift : new int[] {-12 * octaves, 12 * octaves}) {
                long cost = shiftCost(notes, shift, low, high, comfortableHigh);
                if (cost < bestCost) {
                    best = shift;
                    bestCost = cost;
                }
            }
        }
        return best;
    }

    private static long shiftCost(List<Note> notes, int shift, int low, int high, int comfortableHigh) {
        return outOfRange(notes, shift, low, high) * RANGE_WEIGHT + outOfRange(notes, shift, low, comfortableHigh);
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
