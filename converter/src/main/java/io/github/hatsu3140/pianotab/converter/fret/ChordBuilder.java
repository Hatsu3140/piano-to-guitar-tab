package io.github.hatsu3140.pianotab.converter.fret;

import io.github.hatsu3140.pianotab.converter.Difficulty;
import io.github.hatsu3140.pianotab.converter.model.DynamicLevel;
import io.github.hatsu3140.pianotab.converter.model.Measure;
import io.github.hatsu3140.pianotab.converter.model.Note;
import io.github.hatsu3140.pianotab.converter.model.SongInfo;
import io.github.hatsu3140.pianotab.converter.model.Stroke;
import io.github.hatsu3140.pianotab.converter.model.TabNote;
import io.github.hatsu3140.pianotab.converter.model.TabScore;
import io.github.hatsu3140.pianotab.converter.model.Tuning;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 手順3.5: メロディとベースの押さえ方が決まったあとで、その間に和音の音（内声）を足し、
 * ストロークを付ける場所を決める。
 *
 * <p>和音の音は、ピアノがそのタイミング（から1拍の間）に弾いている音から選ぶ。
 * 高さはメロディとベースの間に入るオクターブへ移し、メロディとベースの間の空いている弦に置く。
 * 押さえ方は {@link Hand} で確かめ、指が届かない・指が足りない組み合わせは使わない。
 * 足せる音の数は難易度で決まり、盛り上がる所（f 以上）やストロークの和音では多くしてよい。
 *
 * <p>ストロークは、ピアノ自体が和音（3種類以上の音）を弾いている拍の頭にだけ付ける。
 * <ul>
 *   <li>強い所（f 以上）: 普通のストローク</li>
 *   <li>それ以外: 小節の頭で2分音符以上のばす和音に、2小節に1回まで、軽く素早いストローク</li>
 * </ul>
 * 和音が3音にならなかった所には付けない。
 */
public class ChordBuilder {

    /** 和音を足すのは、メロディが8分音符以上の長さのときだけ */
    private static final int MIN_MELODY_DIVISION = 2;
    private static final int SOFT_STROKE_INTERVAL_MEASURES = 2;

    private final Difficulty difficulty;
    private final Hand hand;
    private final boolean strokesEnabled;

    public ChordBuilder(Difficulty difficulty, int capo, boolean barre, boolean strokes) {
        this.difficulty = difficulty;
        this.hand = new Hand(capo, difficulty.maxStretch(), difficulty.maxFingers(), barre);
        this.strokesEnabled = strokes;
    }

    public TabScore build(TabScore tab, List<Note> harmony) {
        if (difficulty.strokeNotes() <= 1) {
            return tab; // 入門はメロディだけ
        }
        SongInfo info = tab.info();
        int quarter = info.ticksPerQuarter();
        List<TabNote> melody = new ArrayList<>(tab.melody());
        melody.sort(Comparator.comparingLong(note -> note.note().startTick()));

        List<TabNote> inner = new ArrayList<>();
        List<Stroke> strokes = new ArrayList<>();
        int lastSoftStrokeMeasure = -SOFT_STROKE_INTERVAL_MEASURES;
        for (TabNote top : melody) {
            long start = top.note().startTick();
            long end = start + top.note().durationTicks();
            if (top.note().durationTicks() < quarter / MIN_MELODY_DIVISION) {
                continue;
            }
            int measureIndex = measureIndex(info.measures(), start);
            Measure measure = info.measures().get(measureIndex);
            long offset = start - measure.startTick();
            long beat = measure.beatTicks(quarter);
            long grid = difficulty == Difficulty.ADVANCED ? quarter / 2 : beat;
            if (offset % grid != 0) {
                continue;
            }
            TabNote bass = soundingAt(tab.bass(), start);
            if (startsWithin(tab.bass(), start, end)) {
                continue; // 和音を押さえている間にベースが変わるなら足さない
            }

            List<Note> chordTones = notesStartingIn(harmony, start, start + Math.min(beat, end - start));
            boolean pianoPlaysChord = pitchClasses(notesStartingIn(harmony, start, start + 1), top, bass, start)
                    .size() >= 3;
            DynamicLevel level = info.dynamicAt(start);
            boolean loud = level != null && level.compareTo(DynamicLevel.F) >= 0;

            Stroke stroke = null;
            if (strokesEnabled && pianoPlaysChord && offset % beat == 0) {
                if (loud) {
                    stroke = new Stroke(start, false);
                } else if (offset == 0 && top.note().durationTicks() >= 2L * quarter
                        && measureIndex - lastSoftStrokeMeasure >= SOFT_STROKE_INTERVAL_MEASURES) {
                    stroke = new Stroke(start, true);
                }
            }
            int limit = stroke != null ? difficulty.strokeNotes()
                    : loud ? difficulty.climaxNotes() : difficulty.maxNotes();
            int present = bass == null ? 1 : 2;
            List<TabNote> added = limit > present
                    ? choose(tab.tuning(), tab.capo(), top, bass, chordTones, limit - present, stroke != null)
                    : List.of();
            inner.addAll(added);
            if (stroke != null && present + added.size() >= 3) {
                strokes.add(stroke);
                if (stroke.soft()) {
                    lastSoftStrokeMeasure = measureIndex;
                }
            }
        }
        return tab.withInner(inner).withStrokes(strokes);
    }

    /**
     * メロディとベースの間の弦に置く音を選ぶ。できるだけ多く（最大 count 音）、そのうえで押さえやすいもの。
     *
     * @param allowDoubling true ならメロディと同じ音名（オクターブ違い）も使ってよい（ストロークの和音）
     */
    private List<TabNote> choose(Tuning tuning, int capo, TabNote top, TabNote bass, List<Note> chordTones,
                                 int count, boolean allowDoubling) {
        Set<Integer> classes = new HashSet<>();
        for (Note tone : chordTones) {
            classes.add(Math.floorMod(tone.pitch(), 12));
        }
        int melodyClass = Math.floorMod(top.note().pitch(), 12);
        if (!allowDoubling) {
            classes.remove(melodyClass);
        }
        if (classes.isEmpty()) {
            return List.of();
        }
        int lowestString = bass == null ? tuning.stringCount() + 1 : bass.string();
        int floor = bass == null ? Integer.MIN_VALUE : bass.note().pitch();
        List<List<TabNote>> perString = new ArrayList<>();
        for (int string = top.string() + 1; string < lowestString; string++) {
            List<TabNote> options = new ArrayList<>();
            for (int fret = capo; fret <= capo + difficulty.maxFret(); fret++) {
                int pitch = tuning.openPitch(string) + fret;
                if (pitch < top.note().pitch() && pitch > floor && classes.contains(Math.floorMod(pitch, 12))) {
                    options.add(new TabNote(new Note(pitch, top.note().startTick(), top.note().durationTicks()),
                            string, fret));
                }
            }
            perString.add(options);
        }

        List<TabNote> fixed = new ArrayList<>();
        fixed.add(top);
        if (bass != null) {
            fixed.add(bass);
        }
        Search search = new Search(perString, fixed, count, allowDoubling, melodyClass);
        search.run(0, new ArrayList<>());
        return search.best;
    }

    /** 弦ごとの候補から、弾ける組み合わせのうち一番よいものを全部試して探す */
    private final class Search {
        private final List<List<TabNote>> perString;
        private final List<TabNote> fixed;
        private final int count;
        private final boolean allowDoubling;
        private final int melodyClass;
        private List<TabNote> best = List.of();
        private double bestCost = Double.POSITIVE_INFINITY;

        Search(List<List<TabNote>> perString, List<TabNote> fixed, int count, boolean allowDoubling,
               int melodyClass) {
            this.perString = perString;
            this.fixed = fixed;
            this.count = count;
            this.allowDoubling = allowDoubling;
            this.melodyClass = melodyClass;
        }

        void run(int index, List<TabNote> chosen) {
            if (index == perString.size()) {
                consider(chosen);
                return;
            }
            run(index + 1, chosen); // この弦は使わない
            if (chosen.size() >= count) {
                return;
            }
            for (TabNote option : perString.get(index)) {
                chosen.add(option);
                run(index + 1, chosen);
                chosen.remove(chosen.size() - 1);
            }
        }

        private void consider(List<TabNote> chosen) {
            if (chosen.isEmpty()) {
                return;
            }
            Set<Integer> pitches = new HashSet<>();
            Set<Integer> classes = new HashSet<>();
            for (TabNote note : fixed) {
                pitches.add(note.note().pitch());
            }
            for (TabNote note : chosen) {
                int pitchClass = Math.floorMod(note.note().pitch(), 12);
                // 同じ高さの音を2つ（ユニゾン）は使わない。ストローク以外は同じ音名も1つまで
                if (!pitches.add(note.note().pitch()) || (!allowDoubling && !classes.add(pitchClass))) {
                    return;
                }
            }
            List<TabNote> all = new ArrayList<>(fixed);
            all.addAll(chosen);
            if (!hand.canPlay(all)) {
                return;
            }
            // 音が多いほどよい。同じ数なら、低いフレット・押さえる指が少ない・メロディの音名と重ならない方
            double cost = -chosen.size() * 100.0 + hand.fingers(all) * 2.0;
            for (TabNote note : chosen) {
                cost += hand.relativeFret(note) * 0.2;
                if (Math.floorMod(note.note().pitch(), 12) == melodyClass) {
                    cost += 1.0;
                }
            }
            if (cost < bestCost) {
                bestCost = cost;
                best = List.copyOf(chosen);
            }
        }
    }

    private static Set<Integer> pitchClasses(List<Note> harmony, TabNote top, TabNote bass, long start) {
        Set<Integer> classes = new HashSet<>();
        for (Note note : harmony) {
            classes.add(Math.floorMod(note.pitch(), 12));
        }
        classes.add(Math.floorMod(top.note().pitch(), 12));
        if (bass != null && bass.note().startTick() == start) {
            classes.add(Math.floorMod(bass.note().pitch(), 12));
        }
        return classes;
    }

    private static List<Note> notesStartingIn(List<Note> notes, long from, long to) {
        return notes.stream().filter(note -> note.startTick() >= from && note.startTick() < to).toList();
    }

    private static TabNote soundingAt(List<TabNote> notes, long tick) {
        for (TabNote note : notes) {
            long start = note.note().startTick();
            if (start <= tick && tick < start + note.note().durationTicks()) {
                return note;
            }
        }
        return null;
    }

    private static boolean startsWithin(List<TabNote> notes, long from, long to) {
        return notes.stream().anyMatch(note -> note.note().startTick() > from && note.note().startTick() < to);
    }

    private static int measureIndex(List<Measure> measures, long tick) {
        int found = 0;
        for (int i = 0; i < measures.size(); i++) {
            if (measures.get(i).startTick() > tick) {
                break;
            }
            found = i;
        }
        return found;
    }

}
