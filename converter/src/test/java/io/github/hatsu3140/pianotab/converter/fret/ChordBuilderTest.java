package io.github.hatsu3140.pianotab.converter.fret;

import io.github.hatsu3140.pianotab.converter.Difficulty;
import io.github.hatsu3140.pianotab.converter.model.Dynamic;
import io.github.hatsu3140.pianotab.converter.model.DynamicLevel;
import io.github.hatsu3140.pianotab.converter.model.Measure;
import io.github.hatsu3140.pianotab.converter.model.Note;
import io.github.hatsu3140.pianotab.converter.model.SongInfo;
import io.github.hatsu3140.pianotab.converter.model.Stroke;
import io.github.hatsu3140.pianotab.converter.model.TabNote;
import io.github.hatsu3140.pianotab.converter.model.TabScore;
import io.github.hatsu3140.pianotab.converter.model.Tuning;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.hatsu3140.pianotab.converter.TestScores.ONE_MEASURE;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ChordBuilderTest {

    // メロディ E4（1弦開放）とベース C3（5弦3フレット）、ピアノはその間で G3 と C4 を弾いている
    private static final TabNote MELODY = new TabNote(new Note(64, 0, 960), 1, 0);
    private static final TabNote BASS = new TabNote(new Note(48, 0, 1920), 5, 3);
    private static final List<Note> HARMONY = List.of(new Note(55, 0, 960), new Note(60, 0, 960));

    @Test
    void addsChordToneOnFreeStringBetweenMelodyAndBass() {
        TabScore tab = new TabScore(ONE_MEASURE, Tuning.STANDARD, 0, List.of(MELODY), List.of(BASS));

        TabScore result = new ChordBuilder(Difficulty.INTERMEDIATE, 0, true, false).build(tab, HARMONY);

        // 中級は3音まで。押さえやすい3弦開放の G3 を足す
        assertEquals(List.of(new TabNote(new Note(55, 0, 960), 3, 0)), result.inner());
        assertEquals(List.of(), result.strokes());
    }

    @Test
    void addsNothingForIntroductory() {
        TabScore tab = new TabScore(ONE_MEASURE, Tuning.STANDARD, 0, List.of(MELODY), List.of());

        TabScore result = new ChordBuilder(Difficulty.INTRODUCTORY, 0, true, true).build(tab, HARMONY);

        assertEquals(List.of(), result.inner());
        assertEquals(List.of(), result.strokes());
    }

    @Test
    void strokesLoudChord() {
        SongInfo loud = new SongInfo("", 120, 480, List.of(new Measure(0, 1920, 4, 4)),
                List.of(new Dynamic(0, DynamicLevel.F)));
        TabScore tab = new TabScore(loud, Tuning.STANDARD, 0, List.of(MELODY), List.of(BASS));

        TabScore result = new ChordBuilder(Difficulty.INTERMEDIATE, 0, true, true).build(tab, HARMONY);

        assertEquals(List.of(new Stroke(0, false)), result.strokes());
        // ストロークの和音は音を多くしてよい（中級は4音まで）
        assertEquals(2, result.inner().size());
    }

    @Test
    void softlyStrokesLongQuietChord() {
        TabScore tab = new TabScore(ONE_MEASURE, Tuning.STANDARD, 0, List.of(MELODY), List.of(BASS));

        TabScore result = new ChordBuilder(Difficulty.INTERMEDIATE, 0, true, true).build(tab, HARMONY);

        assertEquals(List.of(new Stroke(0, true)), result.strokes());
    }

    @Test
    void doesNotStrokeWhenPianoPlaysNoChord() {
        TabNote shortMelody = new TabNote(new Note(64, 0, 960), 1, 0);
        TabScore tab = new TabScore(ONE_MEASURE, Tuning.STANDARD, 0, List.of(shortMelody), List.of(BASS));

        TabScore result = new ChordBuilder(Difficulty.INTERMEDIATE, 0, true, true)
                .build(tab, List.of(new Note(60, 0, 960)));

        assertEquals(List.of(), result.strokes());
    }

    @Test
    void keepsEveryChordPlayable() {
        SongInfo loud = new SongInfo("", 120, 480, List.of(new Measure(0, 1920, 4, 4)),
                List.of(new Dynamic(0, DynamicLevel.FF)));
        // ピアノが広い和音を弾いていても、届く範囲・指の数の範囲でだけ足す
        List<Note> wide = List.of(new Note(40, 0, 960), new Note(52, 0, 960), new Note(55, 0, 960),
                new Note(59, 0, 960), new Note(62, 0, 960), new Note(66, 0, 960));
        TabNote high = new TabNote(new Note(67, 0, 960), 1, 3);
        TabNote bass = new TabNote(new Note(43, 0, 960), 6, 3);
        TabScore tab = new TabScore(loud, Tuning.STANDARD, 0, List.of(high), List.of(bass));

        for (Difficulty difficulty : Difficulty.values()) {
            for (boolean barre : new boolean[] {true, false}) {
                TabScore result = new ChordBuilder(difficulty, 0, barre, true).build(tab, wide);
                Hand hand = new Hand(0, difficulty.maxStretch(), Math.max(2, difficulty.maxFingers()), barre);
                assertEquals(0, new PlayabilityChecker(hand).check(result).unplayablePlaces());
            }
        }
    }
}
