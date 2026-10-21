package io.github.hatsu3140.pianotab.converter.fret;

import io.github.hatsu3140.pianotab.converter.model.Arrangement;
import io.github.hatsu3140.pianotab.converter.model.Note;
import io.github.hatsu3140.pianotab.converter.model.TabNote;
import io.github.hatsu3140.pianotab.converter.model.TabScore;
import io.github.hatsu3140.pianotab.converter.model.Tuning;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FretAssignerTest {

    private final FretAssigner assigner = new FretAssigner(Tuning.STANDARD);

    @Test
    void openStringNotesUseFretZero() {
        // E4 は 1弦の開放、E2 は 6弦の開放
        Arrangement arrangement = new Arrangement(480,
                List.of(new Note(64, 0, 480)),
                List.of(new Note(40, 0, 480)));

        TabScore tab = assigner.assign(arrangement);

        assertEquals(2, tab.notes().size());
        assertTrue(tab.notes().contains(new TabNote(new Note(64, 0, 480), 1, 0)));
        assertTrue(tab.notes().contains(new TabNote(new Note(40, 0, 480), 6, 0)));
    }

    @Test
    void simultaneousNotesUseDifferentStrings() {
        Arrangement arrangement = new Arrangement(480,
                List.of(new Note(60, 0, 480)),
                List.of(new Note(59, 0, 480)));

        TabScore tab = assigner.assign(arrangement);

        assertEquals(2, tab.notes().size());
        assertNotEquals(tab.notes().get(0).string(), tab.notes().get(1).string());
    }

    @Test
    void dropsNotesOutsideGuitarRange() {
        // C2 はレギュラーチューニングのギターより低いので捨てる
        Arrangement arrangement = new Arrangement(480,
                List.of(new Note(64, 0, 480)),
                List.of(new Note(36, 0, 480)));

        TabScore tab = assigner.assign(arrangement);

        assertEquals(1, tab.notes().size());
        assertEquals(64, tab.notes().get(0).note().pitch());
    }
}
