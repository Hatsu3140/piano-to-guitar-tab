package io.github.hatsu3140.pianotab.converter.fret;

import io.github.hatsu3140.pianotab.converter.model.Note;
import io.github.hatsu3140.pianotab.converter.model.TabNote;
import io.github.hatsu3140.pianotab.converter.model.TabScore;
import io.github.hatsu3140.pianotab.converter.model.Tuning;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.hatsu3140.pianotab.converter.TestScores.ONE_MEASURE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayabilityCheckerTest {

    private final Hand hand = new Hand(0, 4, 4, true);

    @Test
    void choosesFingerForHeldNoteThatAlsoFitsLaterNotes() {
        // 3弦2をのばしている間に2弦1を押さえるので、3弦2は最初から中指で押さえる
        TabNote held = note(3, 2, 0, 960);
        TabScore tab = new TabScore(ONE_MEASURE, Tuning.STANDARD, 0,
                List.of(note(1, 0, 0, 480), note(2, 1, 480, 480)), List.of(held));

        PlayabilityChecker.Result result = new PlayabilityChecker(hand).check(tab);

        assertEquals(List.of(), result.fingerSwapTicks());
        assertEquals(2, result.fingerings().get(0L).fingerOf(held));
        assertEquals(2, result.fingerings().get(480L).fingerOf(held));
    }

    @Test
    void findsPlaceWhereHeldNoteWouldNeedAnotherFinger() {
        // 最初は2弦1が人差し指なので3弦3は中指以上。あとで3弦3より高い音が3つ来ると、3弦3は人差し指しかない
        TabNote held = note(3, 3, 0, 960);
        TabScore tab = swapScore(held);

        PlayabilityChecker.Result result = new PlayabilityChecker(hand).check(tab);

        assertEquals(List.of(480L), result.fingerSwapTicks());
        assertEquals(0, result.unplayablePlaces());
    }

    @Test
    void trimsHeldNoteWhereFingersWouldHaveToChange() {
        HeldNoteTrimmer.Result result = new HeldNoteTrimmer(new PlayabilityChecker(hand))
                .trim(swapScore(note(3, 3, 0, 960)));

        assertEquals(1, result.trimmedNotes());
        assertEquals(480, result.tabScore().bass().get(0).note().durationTicks());
        assertEquals(List.of(), new PlayabilityChecker(hand).check(result.tabScore()).fingerSwapTicks());
    }

    @Test
    void slidingWholeBarreShapeIsEasierThanPlacingNewFingers() {
        Fingering f = hand.fingering(barreShape(1)).orElseThrow();
        Fingering g = hand.fingering(barreShape(3)).orElseThrow();
        Fingering c = hand.fingering(List.of(note(1, 0, 0, 480), note(2, 1, 0, 480), note(3, 0, 0, 480),
                note(4, 2, 0, 480), note(5, 3, 0, 480))).orElseThrow();

        assertEquals(0, PlayabilityChecker.movement(f, f));
        assertTrue(PlayabilityChecker.movement(f, g) < PlayabilityChecker.movement(c, g));
    }

    @Test
    void keepsChordShapeWhileMelodyMoves() {
        // F型セーハを4フレットで押さえたまま、1弦の4と2弦の4を順に鳴らす
        TabNote bass = note(6, 4, 0, 960);
        TabScore tab = new TabScore(ONE_MEASURE, Tuning.STANDARD, 0,
                List.of(note(1, 4, 0, 480), note(2, 4, 480, 480)), List.of(bass),
                List.of(note(4, 6, 0, 480), note(3, 5, 480, 480)), List.of());

        PlayabilityChecker.Result result = new PlayabilityChecker(hand).check(tab);

        assertEquals(1, result.fingerings().get(0L).fingerOf(bass));
        assertEquals(4, result.fingerings().get(0L).barreFret());
        assertEquals(4, result.fingerings().get(480L).barreFret());
    }

    private static List<TabNote> barreShape(int fret) {
        return List.of(note(1, fret, 0, 480), note(2, fret, 0, 480), note(3, fret + 1, 0, 480),
                note(4, fret + 2, 0, 480), note(5, fret + 2, 0, 480), note(6, fret, 0, 480));
    }

    private static TabScore swapScore(TabNote held) {
        return new TabScore(ONE_MEASURE, Tuning.STANDARD, 0,
                List.of(note(2, 1, 0, 480), note(2, 4, 480, 480)), List.of(held),
                List.of(note(1, 5, 480, 480), note(4, 6, 480, 480)), List.of());
    }

    private static TabNote note(int string, int fret, long start, long duration) {
        return new TabNote(new Note(60, start, duration), string, fret);
    }
}
