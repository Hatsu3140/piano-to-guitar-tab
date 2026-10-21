package io.github.hatsu3140.pianotab.converter.fret;

import io.github.hatsu3140.pianotab.converter.model.Note;
import io.github.hatsu3140.pianotab.converter.model.TabNote;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HandTest {

    // F のコード（1弦〜5弦）: 1・2弦の1フレットをセーハ、3弦2フレット、4・5弦3フレット
    private static final List<TabNote> F_CHORD = List.of(
            note(1, 1), note(2, 1), note(3, 2), note(4, 3), note(5, 3));

    @Test
    void countsBarreAsOneFinger() {
        Hand hand = new Hand(0, 4, 4, true);

        assertEquals(4, hand.fingers(F_CHORD));
        assertTrue(hand.canPlay(F_CHORD));
        assertTrue(hand.usesBarre(F_CHORD));
    }

    @Test
    void needsBarreWhenFingersAreNotEnough() {
        Hand hand = new Hand(0, 4, 4, false);

        assertEquals(5, hand.fingers(F_CHORD));
        assertFalse(hand.canPlay(F_CHORD));
    }

    @Test
    void cannotBarreOverOpenString() {
        // 1弦と3弦の1フレットの間に、開放弦の2弦がある → セーハすると2弦が鳴らない
        List<TabNote> notes = List.of(note(1, 1), note(2, 0), note(3, 1));

        assertEquals(2, new Hand(0, 4, 4, true).fingers(notes));
    }

    @Test
    void rejectsTwoNotesOnOneStringAndWideStretch() {
        Hand hand = new Hand(0, 4, 4, true);

        assertFalse(hand.canPlay(List.of(note(1, 1), note(1, 3))));
        assertFalse(hand.canPlay(List.of(note(1, 8), note(5, 3))));
        assertTrue(hand.canPlay(List.of(note(1, 0), note(5, 3))));
    }

    @Test
    void countsFretsFromCapo() {
        // カポ3なら、ネックの3フレットは開放弦と同じ
        Hand hand = new Hand(3, 4, 4, true);

        assertTrue(hand.canPlay(List.of(note(1, 3), note(5, 7))));
        assertFalse(hand.canPlay(List.of(note(1, 2))));
    }

    private static TabNote note(int string, int fret) {
        return new TabNote(new Note(60, 0, 480), string, fret);
    }
}
