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
    void definesWhichFingerPressesEachNote() {
        Fingering fingering = new Hand(0, 4, 4, true).fingering(F_CHORD).orElseThrow();

        assertEquals(1, fingering.barreFret());
        assertEquals(2, fingering.barreTo());
        assertEquals("人差し指 1フレット セーハ(1〜2弦) / 中指 3弦2 / 薬指 5弦3 / 小指 4弦3（F型セーハの形）",
                fingering.describe(0));
    }

    @Test
    void prefersNotToBarreWhenFingersAreEnough() {
        // C のコード: 人差し指 2弦1・中指 4弦2・薬指 5弦3、1弦と3弦は開放
        List<TabNote> c = List.of(note(1, 0), note(2, 1), note(3, 0), note(4, 2), note(5, 3));
        Fingering fingering = new Hand(0, 4, 4, true).fingering(c).orElseThrow();

        assertFalse(fingering.usesBarre());
        assertEquals("開放 1弦0 / 開放 3弦0 / 人差し指 2弦1 / 中指 4弦2 / 薬指 5弦3（Cの形）", fingering.describe(0));
    }

    @Test
    void describesBarreWithCapo() {
        // カポ2で、ネックの4フレットを1〜6弦セーハ（E の形）
        List<TabNote> e = List.of(note(1, 4), note(2, 4), note(3, 5), note(4, 6), note(5, 6), note(6, 4));
        Fingering fingering = new Hand(2, 4, 4, true).fingering(e).orElseThrow();

        assertEquals("人差し指 4フレット セーハ(1〜6弦) / 中指 3弦5 / 薬指 5弦6 / 小指 4弦6（F型セーハの形）",
                fingering.describe(2));
    }

    @Test
    void knowsCommonChordShapes() {
        Hand hand = new Hand(0, 4, 4, true);
        // G のコードの一部（6弦3・4弦開放・1弦3）は、ルールでは押さえられない形でも、G の形として押さえられる
        Fingering g = hand.fingering(List.of(note(1, 3), note(4, 0), note(6, 3))).orElseThrow();

        assertEquals("G", g.shape());
        // 同じ形をカポ2で
        assertTrue(new Hand(2, 4, 4, true).canPlay(List.of(note(1, 5), note(4, 2), note(6, 5))));
    }

    @Test
    void movesBarreShapesAlongTheNeck() {
        // B♭型セーハを3フレットに（C）
        Fingering c = new Hand(0, 4, 4, true)
                .fingering(List.of(note(1, 3), note(2, 5), note(3, 5), note(4, 5), note(5, 3))).orElseThrow();

        assertEquals("B♭型セーハ", c.shape());
        assertEquals(3, c.barreFret());
    }

    @Test
    void needsBarreWhenFingersAreNotEnough() {
        Hand hand = new Hand(0, 4, 4, false);

        assertEquals(5, hand.fingers(F_CHORD));
        assertFalse(hand.canPlay(F_CHORD));
    }

    @Test
    void cannotReachFarBeyondBarre() {
        // 1フレットを2〜4弦でセーハしたまま、1弦と6弦の4フレット・5弦3フレットは押さえられない
        // （4フレットまで指を伸ばすとセーハが保てない）
        List<TabNote> notes = List.of(note(1, 4), note(2, 1), note(3, 1), note(4, 1), note(5, 3), note(6, 4));
        Hand hand = new Hand(0, 4, 4, true);

        assertEquals(6, hand.fingers(notes));
        assertFalse(hand.canPlay(notes));
    }

    @Test
    void assignsFingersInOrderOfFrets() {
        Hand hand = new Hand(0, 4, 4, true);

        // 人差し指 3弦5・中指 4弦6・薬指 5弦6 のあと、小指で 2弦9 は遠すぎる（薬指から3フレット）
        assertFalse(hand.canPlay(List.of(note(2, 9), note(3, 5), note(4, 6), note(5, 6))));
        assertTrue(hand.fingering(List.of(note(2, 9), note(3, 5), note(4, 6), note(5, 6))).isEmpty());
        // 人差し指 5・中指 6・小指 9 なら届く
        assertTrue(hand.canPlay(List.of(note(2, 9), note(3, 5), note(4, 6))));
        // 7フレット付近なら、人差し指と小指で4フレット離せる
        assertTrue(hand.canPlay(List.of(note(1, 11), note(6, 7))));
    }

    @Test
    void spreadsLessNearTheNut() {
        Hand hand = new Hand(0, 4, 4, true);

        // 1フレット付近では、人差し指と小指は3フレットまで
        assertFalse(hand.canPlay(List.of(note(1, 5), note(6, 1))));
        assertTrue(hand.canPlay(List.of(note(1, 4), note(6, 1))));
        // 2・3弦1フレットを人差し指と中指、4弦3を薬指で押さえたまま、小指で1弦4には届かない
        assertFalse(hand.canPlay(List.of(note(1, 4), note(2, 1), note(3, 1), note(4, 3))));
        // 小指以外はそのまま: B♭（1フレットのセーハと、3フレットに中指・薬指・小指）
        assertTrue(hand.canPlay(List.of(note(1, 1), note(2, 3), note(3, 3), note(4, 3), note(5, 1))));
        // カポを付けても、ネックの実際のフレットで考える
        assertTrue(new Hand(5, 4, 4, true).canPlay(List.of(note(1, 9), note(6, 5))));
    }

    @Test
    void cannotPressOneFretWithFingersFarApart() {
        Hand hand = new Hand(0, 4, 4, true);

        // 6弦5と1弦5を人差し指と中指で（4弦は開放なのでセーハもできない）
        assertFalse(hand.canPlay(List.of(note(1, 5), note(4, 0), note(6, 5))));
        // A のように隣の弦なら並べられる
        assertTrue(hand.canPlay(List.of(note(2, 2), note(3, 2), note(4, 2))));
    }

    @Test
    void barreCoversStringsUpToTheFirst() {
        // 2・3弦の1フレットをセーハすると1弦も押さえるので、1弦の開放は鳴らせない
        List<TabNote> notes = List.of(note(1, 0), note(2, 1), note(3, 1));

        assertEquals(2, new Hand(0, 4, 4, true).fingers(notes));
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
