package io.github.hatsu3140.pianotab.converter.arrange;

import io.github.hatsu3140.pianotab.converter.Difficulty.BassRhythm;
import io.github.hatsu3140.pianotab.converter.model.Arrangement;
import io.github.hatsu3140.pianotab.converter.model.Measure;
import io.github.hatsu3140.pianotab.converter.model.Note;
import io.github.hatsu3140.pianotab.converter.model.SongInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.hatsu3140.pianotab.converter.TestScores.ONE_MEASURE;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BassSimplifierTest {

    // 8分音符のベース: 1拍目、1拍目の裏、2拍目
    private static final List<Note> BASS = List.of(
            new Note(40, 0, 240), new Note(47, 240, 240), new Note(52, 480, 240));
    private static final Arrangement ARRANGEMENT = new Arrangement(ONE_MEASURE, List.of(new Note(64, 0, 1920)), BASS);

    @Test
    void keepsOnlyMeasureStartsForBeginner() {
        Arrangement result = new BassSimplifier(BassRhythm.MEASURE).simplify(ARRANGEMENT);

        // 残した音は小節の終わりまで響かせる
        assertEquals(List.of(new Note(40, 0, 1920)), result.bass());
    }

    @Test
    void keepsOnlyBeatStartsForIntermediate() {
        Arrangement result = new BassSimplifier(BassRhythm.BEAT).simplify(ARRANGEMENT);

        assertEquals(List.of(new Note(40, 0, 480), new Note(52, 480, 1440)), result.bass());
    }

    @Test
    void letsEveryBassNoteRingUntilTheNextForAdvanced() {
        Arrangement result = new BassSimplifier(BassRhythm.ALL).simplify(ARRANGEMENT);

        assertEquals(List.of(new Note(40, 0, 240), new Note(47, 240, 240), new Note(52, 480, 1440)), result.bass());
    }

    @Test
    void keepsLongBassNoteLength() {
        // 次のベース音より長い音は、そのまま（重なる分はフレット決めのときに切れる）
        Arrangement arrangement = new Arrangement(ONE_MEASURE, List.of(), List.of(new Note(40, 0, 1920)));

        Arrangement result = new BassSimplifier(BassRhythm.BEAT).simplify(arrangement);

        assertEquals(List.of(new Note(40, 0, 1920)), result.bass());
    }

    @Test
    void removesBassForIntroductory() {
        Arrangement result = new BassSimplifier(BassRhythm.NONE).simplify(ARRANGEMENT);

        assertEquals(List.of(), result.bass());
        assertEquals(ARRANGEMENT.melody(), result.melody());
    }

    @Test
    void usesDottedQuarterAsBeatInSixEight() {
        // 6/8 拍子の1拍は付点4分音符（720 tick）
        SongInfo sixEight = new SongInfo("", 120, 480, List.of(new Measure(0, 1440, 6, 8)));
        Arrangement arrangement = new Arrangement(sixEight, List.of(), List.of(
                new Note(40, 0, 240), new Note(47, 480, 240), new Note(45, 720, 240)));

        Arrangement result = new BassSimplifier(BassRhythm.BEAT).simplify(arrangement);

        assertEquals(List.of(new Note(40, 0, 720), new Note(45, 720, 720)), result.bass());
    }
}
