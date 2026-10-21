package io.github.hatsu3140.pianotab.converter;

import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
