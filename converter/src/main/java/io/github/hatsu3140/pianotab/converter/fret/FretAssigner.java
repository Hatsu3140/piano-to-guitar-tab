package io.github.hatsu3140.pianotab.converter.fret;

import io.github.hatsu3140.pianotab.converter.Difficulty;
import io.github.hatsu3140.pianotab.converter.model.Arrangement;
import io.github.hatsu3140.pianotab.converter.model.Note;
import io.github.hatsu3140.pianotab.converter.model.TabNote;
import io.github.hatsu3140.pianotab.converter.model.TabScore;
import io.github.hatsu3140.pianotab.converter.model.Tuning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

/**
 * 手順3: 各音を「何弦の何フレット」で弾くか決める。
 *
 * <p>曲の頭から順に、音が鳴り始めるタイミングごとに「メロディとベースをどこで押さえるか」の
 * 候補を全部出し、曲全体で一番弾きやすい組み合わせを選ぶ（動的計画法 = DP）。
 * 弾きやすさは次の「コスト」の合計が小さいほど良いとする。
 * <ul>
 *   <li>同時に押さえる2音のフレットが上限（難易度で決まる。中級は4フレット）より離れていたら弾けない</li>
 *   <li>前の音からのポジション移動が大きいほど弾きにくい</li>
 *   <li>高いフレット（特に12フレットより上）ほど弾きにくい</li>
 *   <li>メロディがベースより低い弦にくるのは弾きにくい</li>
 * </ul>
 * どう押さえても弾けないときは、弾けない譜面を作らないよう、ベースの音を捨てるか、
 * 伸ばしている音を途中で止める。
 *
 * <p>カポを付けたときは、カポの位置を0フレットとして考え、出力するときにカポの分を足す。
 */
public class FretAssigner {

    /** 難易度を指定しないときに使う一番高いフレット */
    public static final int MAX_FRET = 19;
    /** 難易度を指定しないときの、同時に押さえる音のフレットの差の上限 */
    public static final int MAX_STRETCH = 4;

    private static final double IMPOSSIBLE = Double.POSITIVE_INFINITY;
    private static final double FRET_COST = 0.2;
    private static final double HIGH_FRET_COST = 1.0;
    private static final double STRETCH_COST = 1.0;
    private static final double CROSSED_STRINGS_COST = 5.0;
    private static final double DROP_BASS_COST = 30.0;
    private static final double CUT_BASS_COST = 10.0;
    private static final double CUT_MELODY_COST = 15.0;

    private final Tuning tuning;
    private final int capo;
    private final int maxFret;
    private final int maxStretch;
    private final double moveCost;
    private final double frettedBassCost;

    /** カポなし・19フレットまで使う */
    public FretAssigner(Tuning tuning) {
        this(tuning, 0, MAX_FRET, MAX_STRETCH, 1.0, 0);
    }

    public FretAssigner(Tuning tuning, int capo, Difficulty difficulty) {
        this(tuning, capo, difficulty.maxFret(), difficulty.maxStretch(), difficulty.moveCost(),
                difficulty.frettedBassCost());
    }

    private FretAssigner(Tuning tuning, int capo, int maxFret, int maxStretch, double moveCost,
                         double frettedBassCost) {
        this.tuning = tuning;
        this.capo = capo;
        this.maxFret = maxFret;
        this.maxStretch = maxStretch;
        this.moveCost = moveCost;
        this.frettedBassCost = frettedBassCost;
    }

    public TabScore assign(Arrangement arrangement) {
        Voice melody = new Voice(arrangement.melody());
        Voice bass = new Voice(arrangement.bass());

        // 音が鳴り始めるタイミングを全部集める
        TreeSet<Long> startTicks = new TreeSet<>();
        melody.notes.forEach(note -> startTicks.add(note.startTick()));
        bass.notes.forEach(note -> startTicks.add(note.startTick()));
        long[] times = startTicks.stream().mapToLong(Long::longValue).toArray();
        if (times.length == 0) {
            return new TabScore(arrangement.info(), tuning, capo, List.of(), List.of());
        }

        // 各タイミングでの押さえ方の候補
        List<List<State>> states = new ArrayList<>();
        for (long time : times) {
            states.add(candidates(melody, bass, time));
        }

        // DP: cost[i][j] = タイミング i で候補 j を選んだときの、そこまでの最小コスト
        double[][] cost = new double[times.length][];
        int[][] previous = new int[times.length][];
        for (int i = 0; i < times.length; i++) {
            List<State> current = states.get(i);
            cost[i] = new double[current.size()];
            previous[i] = new int[current.size()];
            Arrays.fill(cost[i], IMPOSSIBLE);
            for (int j = 0; j < current.size(); j++) {
                State state = current.get(j);
                double own = stateCost(state);
                if (i == 0) {
                    cost[i][j] = own;
                    continue;
                }
                for (int k = 0; k < states.get(i - 1).size(); k++) {
                    double total = cost[i - 1][k] + own + transitionCost(states.get(i - 1).get(k), state);
                    if (total < cost[i][j]) {
                        cost[i][j] = total;
                        previous[i][j] = k;
                    }
                }
            }
        }

        // 一番コストの小さい終わり方から、選んだ候補を逆にたどる
        int last = times.length - 1;
        int best = 0;
        for (int j = 1; j < cost[last].length; j++) {
            if (cost[last][j] < cost[last][best]) {
                best = j;
            }
        }
        State[] chosen = new State[times.length];
        for (int i = last; i >= 0; i--) {
            chosen[i] = states.get(i).get(best);
            best = previous[i][best];
        }

        return new TabScore(arrangement.info(), tuning, capo,
                toTabNotes(melody, chosen, times, true), toTabNotes(bass, chosen, times, false));
    }

    /** タイミング time でのメロディとベースの押さえ方の候補をすべて作る */
    private List<State> candidates(Voice melody, Voice bass, long time) {
        Sounding melodyNote = melody.soundingAt(time);
        Sounding bassNote = bass.soundingAt(time);
        List<Position> melodyOptions = options(melodyNote, false);
        List<Position> bassOptions = options(bassNote, true);

        List<State> result = new ArrayList<>();
        for (Position melodyPosition : melodyOptions) {
            for (Position bassPosition : bassOptions) {
                result.add(new State(melodyNote, melodyPosition, bassNote, bassPosition));
            }
        }
        return result;
    }

    /** 1つの音の押さえ方の候補。null は「弾かない（捨てる・止める）」 */
    private List<Position> options(Sounding sounding, boolean canDrop) {
        List<Position> result = new ArrayList<>();
        if (sounding == null) {
            result.add(null);
            return result;
        }
        // 音域内でも押さえられない高さがある（例: ドロップD・5フレットまでの G#2）。
        // そのときは1オクターブずつ上下にずらして、押さえられる高さを探す
        int pitch = fitIntoRange(sounding.note().pitch());
        for (int shift : new int[] {0, 12, -12, 24, -24}) {
            for (int string = 1; string <= tuning.stringCount(); string++) {
                int fret = pitch + shift - tuning.openPitch(string) - capo;
                if (fret >= 0 && fret <= maxFret) {
                    result.add(new Position(string, fret));
                }
            }
            if (!result.isEmpty()) {
                break;
            }
        }
        // 鳴り始めのメロディは必ず弾く（押さえようがないときだけ捨てる）。それ以外は止める・捨てる選択肢も残す
        if (canDrop || !sounding.isNew() || result.isEmpty()) {
            result.add(null);
        }
        return result;
    }

    /** その瞬間の押さえ方そのもののコスト */
    private double stateCost(State state) {
        Position melody = state.melodyPosition();
        Position bass = state.bassPosition();
        double total = 0;

        if (melody != null && bass != null) {
            if (melody.string() == bass.string()) {
                return IMPOSSIBLE;
            }
            if (melody.fret() > 0 && bass.fret() > 0) {
                int stretch = Math.abs(melody.fret() - bass.fret());
                if (stretch > maxStretch) {
                    return IMPOSSIBLE;
                }
                total += Math.max(0, stretch - 2) * STRETCH_COST;
            }
            if (melody.string() > bass.string()) {
                total += CROSSED_STRINGS_COST;
            }
        }
        // フレットの高さのコストは、音の鳴り始めにだけ数える
        if (state.melody() != null && state.melody().isNew() && melody != null) {
            total += fretCost(melody.fret());
        }
        if (state.bass() != null && state.bass().isNew()) {
            if (bass == null) {
                total += DROP_BASS_COST;
            } else {
                total += fretCost(bass.fret()) + (bass.fret() > 0 ? frettedBassCost : 0);
            }
        }
        return total;
    }

    private static double fretCost(int fret) {
        return fret * FRET_COST + Math.max(0, fret - 12) * HIGH_FRET_COST;
    }

    /** 前のタイミングの押さえ方から今の押さえ方へ移るコスト */
    private double transitionCost(State before, State after) {
        double total = 0;
        // 伸ばしている音は同じ場所を押さえ続ける（止めるのは可）
        if (after.melody() != null && !after.melody().isNew()) {
            if (after.melodyPosition() == null) {
                total += before.melodyPosition() == null ? 0 : CUT_MELODY_COST;
            } else if (!after.melodyPosition().equals(before.melodyPosition())) {
                return IMPOSSIBLE;
            }
        }
        if (after.bass() != null && !after.bass().isNew()) {
            if (after.bassPosition() == null) {
                total += before.bassPosition() == null ? 0 : CUT_BASS_COST;
            } else if (!after.bassPosition().equals(before.bassPosition())) {
                return IMPOSSIBLE;
            }
        }
        int handBefore = before.handPosition();
        int handAfter = after.handPosition();
        if (handBefore > 0 && handAfter > 0) {
            total += Math.abs(handAfter - handBefore) * moveCost;
        }
        return total;
    }

    /** DP の結果から TabNote の一覧を作る。途中で止めた音は短くする */
    private List<TabNote> toTabNotes(Voice voice, State[] chosen, long[] times, boolean isMelody) {
        List<TabNote> result = new ArrayList<>();
        for (int n = 0; n < voice.notes.size(); n++) {
            Note note = voice.notes.get(n);
            int startIndex = Arrays.binarySearch(times, note.startTick());
            Position position = chosen[startIndex].position(isMelody);
            if (position == null) {
                continue; // 捨てた音
            }
            long end = voice.ends[n];
            for (int i = startIndex + 1; i < times.length && times[i] < end; i++) {
                if (chosen[i].position(isMelody) == null) {
                    end = times[i]; // ここで止めた
                    break;
                }
            }
            // 出力するフレットは、カポの分を足したネック上の実際のフレット
            int fret = position.fret() + capo;
            int pitch = tuning.openPitch(position.string()) + fret;
            result.add(new TabNote(new Note(pitch, note.startTick(), end - note.startTick()),
                    position.string(), fret));
        }
        return result;
    }

    /** ギターで出せる高さになるまでオクターブ単位で上げ下げする */
    private int fitIntoRange(int pitch) {
        int lowest = tuning.lowestPitch() + capo;
        int highest = tuning.highestPitch() + capo + maxFret;
        while (pitch < lowest) {
            pitch += 12;
        }
        while (pitch > highest) {
            pitch -= 12;
        }
        return pitch;
    }

    /**
     * 1つの声部（メロディまたはベース）。同時に鳴るのは1音だけになるよう、
     * 次の音が鳴り始めたら前の音は終わったことにする。
     */
    private static final class Voice {
        private final List<Note> notes;
        private final long[] ends;

        Voice(List<Note> notes) {
            List<Note> sorted = new ArrayList<>(notes);
            sorted.sort(Comparator.comparingLong(Note::startTick));
            // 同じタイミングに2音ある場合は最初の1音だけ使う
            List<Note> unique = new ArrayList<>();
            for (Note note : sorted) {
                if (unique.isEmpty() || unique.get(unique.size() - 1).startTick() != note.startTick()) {
                    unique.add(note);
                }
            }
            this.notes = unique;
            this.ends = new long[unique.size()];
            for (int i = 0; i < unique.size(); i++) {
                Note note = unique.get(i);
                long end = note.startTick() + note.durationTicks();
                if (i + 1 < unique.size()) {
                    end = Math.min(end, unique.get(i + 1).startTick());
                }
                ends[i] = end;
            }
        }

        /** time に鳴っている音（なければ null） */
        Sounding soundingAt(long time) {
            int low = 0;
            int high = notes.size() - 1;
            int found = -1;
            while (low <= high) {
                int middle = (low + high) / 2;
                if (notes.get(middle).startTick() <= time) {
                    found = middle;
                    low = middle + 1;
                } else {
                    high = middle - 1;
                }
            }
            if (found < 0 || ends[found] <= time) {
                return null;
            }
            return new Sounding(notes.get(found), notes.get(found).startTick() == time);
        }
    }

    /** ある瞬間に鳴っている音と、それがちょうど鳴り始めた音かどうか */
    private record Sounding(Note note, boolean isNew) {
    }

    /** 押さえる場所（弦と、カポを基準にしたフレット） */
    private record Position(int string, int fret) {
    }

    /** ある瞬間のメロディとベースの押さえ方 */
    private record State(Sounding melody, Position melodyPosition, Sounding bass, Position bassPosition) {

        Position position(boolean isMelody) {
            return isMelody ? melodyPosition : bassPosition;
        }

        /** 左手の位置 = 押さえているフレットのうち一番低いもの（開放弦だけなら 0） */
        int handPosition() {
            int hand = 0;
            for (Position position : new Position[] {melodyPosition, bassPosition}) {
                if (position != null && position.fret() > 0) {
                    hand = hand == 0 ? position.fret() : Math.min(hand, position.fret());
                }
            }
            return hand;
        }
    }
}
