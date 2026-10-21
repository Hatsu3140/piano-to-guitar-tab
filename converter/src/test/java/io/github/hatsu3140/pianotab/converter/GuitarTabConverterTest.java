package io.github.hatsu3140.pianotab.converter;

import io.github.hatsu3140.pianotab.converter.model.TabNote;
import io.github.hatsu3140.pianotab.converter.model.Tuning;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuitarTabConverterTest {

    @Test
    void convertsMusicXmlToGp5() throws Exception {
        ConversionResult result;
        try (InputStream in = getClass().getResourceAsStream("/two-hands.musicxml")) {
            result = new GuitarTabConverter().convert(in);
        }

        // メロディ4音（C5 E5 G5 C6）、ベース2音（C3, F#2）
        assertEquals(4, result.tabScore().melody().size());
        assertEquals(2, result.tabScore().bass().size());
        assertTrue(result.gp5().length > 0);
    }

    @Test
    void introductoryHasNoBass() throws Exception {
        ConversionOptions options = new ConversionOptions(Difficulty.INTRODUCTORY, Tuning.STANDARD, 0, 0);

        ConversionResult result = convert(options);

        assertEquals(4, result.tabScore().melody().size());
        assertEquals(0, result.tabScore().bass().size());
    }

    @Test
    void appliesTuningCapoAndTranspose() throws Exception {
        ConversionOptions options = new ConversionOptions(Difficulty.ADVANCED, Tuning.DROP_D, 2, -2);

        ConversionResult result = convert(options);

        assertEquals(Tuning.DROP_D, result.tabScore().tuning());
        assertEquals(2, result.tabScore().capo());
        for (TabNote note : result.tabScore().notes()) {
            // カポより低いフレットは押さえられない
            assertTrue(note.fret() >= 2);
            // 2半音下げているので、元の C（ド）は B♭ になる
            assertEquals(note.note().pitch(), Tuning.DROP_D.openPitch(note.string()) + note.fret());
        }
        assertEquals(10, result.tabScore().melody().get(0).note().pitch() % 12);
    }

    @Test
    void rejectsCapoOutOfRange() {
        assertThrows(IllegalArgumentException.class,
                () -> new ConversionOptions(Difficulty.INTERMEDIATE, Tuning.STANDARD, 8, 0));
    }

    private ConversionResult convert(ConversionOptions options) throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/two-hands.musicxml")) {
            return new GuitarTabConverter().convert(in, options);
        }
    }
}
