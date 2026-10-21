package io.github.hatsu3140.pianotab.converter.fret;

import io.github.hatsu3140.pianotab.converter.model.Arrangement;
import io.github.hatsu3140.pianotab.converter.model.Note;
import io.github.hatsu3140.pianotab.converter.model.TabNote;
import io.github.hatsu3140.pianotab.converter.model.TabScore;
import io.github.hatsu3140.pianotab.converter.model.Tuning;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.hatsu3140.pianotab.converter.TestScores.ONE_MEASURE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class FretAssignerTest {

    private final FretAssigner assigner = new FretAssigner(Tuning.STANDARD);

    @Test
    void openStringNotesUseFretZero() {
        // E4 は 1弦の開放、E2 は 6弦の開放
        Arrangement arrangement = new Arrangement(ONE_MEASURE,
                List.of(new Note(64, 0, 480)),
                List.of(new Note(40, 0, 480)));

        TabScore tab = assigner.assign(arrangement);

        assertEquals(List.of(new TabNote(new Note(64, 0, 480), 1, 0)), tab.melody());
        assertEquals(List.of(new TabNote(new Note(40, 0, 480), 6, 0)), tab.bass());
    }

    @Test
    void simultaneousNotesUseDifferentStrings() {
        Arrangement arrangement = new Arrangement(ONE_MEASURE,
                List.of(new Note(60, 0, 480)),
                List.of(new Note(59, 0, 480)));

        TabScore tab = assigner.assign(arrangement);

        assertEquals(2, tab.notes().size());
        assertNotEquals(tab.melody().get(0).string(), tab.bass().get(0).string());
    }

    @Test
    void movesNotesOutsideGuitarRangeByOctaves() {
        // C2 はギターより低いので C3 に、C7 は高すぎるので（19フレットの B5 まで）C5 に移す
        Arrangement arrangement = new Arrangement(ONE_MEASURE,
                List.of(new Note(96, 0, 480)),
                List.of(new Note(36, 0, 480)));

        TabScore tab = assigner.assign(arrangement);

        assertEquals(72, tab.melody().get(0).note().pitch());
        assertEquals(48, tab.bass().get(0).note().pitch());
    }

    @Test
    void dropsNotesWhenNoStringIsLeft() {
        // 同時に7音は6弦のギターでは弾けないので、1音は捨てる
        List<Note> chord = List.of(
                new Note(64, 0, 480), new Note(62, 0, 480), new Note(60, 0, 480), new Note(59, 0, 480),
                new Note(57, 0, 480), new Note(55, 0, 480), new Note(52, 0, 480));
        Arrangement arrangement = new Arrangement(ONE_MEASURE, chord, List.of());

        TabScore tab = assigner.assign(arrangement);

        assertEquals(6, tab.notes().size());
    }

    @Test
    void doesNotReuseStringOfHeldNote() {
        // E4（1弦開放）を伸ばしている間に G4 が鳴る → G4 は1弦ではなく2弦8フレット
        Arrangement arrangement = new Arrangement(ONE_MEASURE,
                List.of(new Note(64, 0, 960)),
                List.of(new Note(67, 480, 480)));

        TabScore tab = assigner.assign(arrangement);

        assertEquals(new TabNote(new Note(67, 480, 480), 2, 8), tab.bass().get(0));
    }
}
