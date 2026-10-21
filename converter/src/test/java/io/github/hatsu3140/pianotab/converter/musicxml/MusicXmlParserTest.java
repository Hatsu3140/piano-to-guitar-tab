package io.github.hatsu3140.pianotab.converter.musicxml;

import io.github.hatsu3140.pianotab.converter.model.Measure;
import io.github.hatsu3140.pianotab.converter.model.Note;
import io.github.hatsu3140.pianotab.converter.model.Score;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MusicXmlParserTest {

    private final MusicXmlParser parser = new MusicXmlParser();

    @Test
    void readsNotesOfBothHands() throws Exception {
        Score score = parse(readResource("/two-hands.musicxml"));

        assertEquals("テスト曲", score.info().title());
        assertEquals(90, score.info().tempo());
        assertEquals(List.of(new Measure(0, 1920, 4, 4), new Measure(1920, 1920, 4, 4)), score.info().measures());
        assertEquals(List.of(
                new Note(72, 0, 480),      // C5
                new Note(76, 480, 480),    // E5
                new Note(79, 960, 480),    // G5
                new Note(84, 1440, 960),   // C6（タイで2小節目の4分音符までつながる）
                new Note(48, 0, 960),      // C3
                new Note(55, 0, 960),      // G3（C3 と同時の和音）
                new Note(42, 960, 960)),   // F#2
                score.notes());
    }

    @Test
    void readsCompressedMxl() throws Exception {
        ByteArrayOutputStream zipBytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(zipBytes)) {
            zip.putNextEntry(new ZipEntry("META-INF/container.xml"));
            zip.write("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <container><rootfiles><rootfile full-path="score.musicxml"/></rootfiles></container>
                    """.getBytes(StandardCharsets.UTF_8));
            zip.putNextEntry(new ZipEntry("score.musicxml"));
            zip.write(readResource("/two-hands.musicxml"));
        }

        Score score = parse(zipBytes.toByteArray());

        assertEquals(7, score.notes().size());
    }

    @Test
    void usesPickupMeasureLength() throws Exception {
        String xml = """
                <score-partwise><part id="P1">
                  <measure number="0" implicit="yes">
                    <attributes><divisions>1</divisions><time><beats>3</beats><beat-type>4</beat-type></time></attributes>
                    <note><pitch><step>G</step><octave>4</octave></pitch><duration>1</duration></note>
                  </measure>
                  <measure number="1">
                    <note><pitch><step>C</step><octave>5</octave></pitch><duration>3</duration></note>
                  </measure>
                </part></score-partwise>
                """;

        Score score = parse(xml.getBytes(StandardCharsets.UTF_8));

        assertEquals(List.of(new Measure(0, 480, 3, 4), new Measure(480, 1440, 3, 4)), score.info().measures());
        assertEquals(new Note(72, 480, 1440), score.notes().get(1));
    }

    @Test
    void rejectsNonMusicXml() {
        assertThrows(MusicXmlFormatException.class, () -> parse("<html></html>".getBytes(StandardCharsets.UTF_8)));
        assertThrows(MusicXmlFormatException.class, () -> parse("not xml".getBytes(StandardCharsets.UTF_8)));
    }

    private Score parse(byte[] data) throws IOException, MusicXmlFormatException {
        return parser.parse(new ByteArrayInputStream(data));
    }

    private static byte[] readResource(String name) throws IOException {
        try (InputStream in = MusicXmlParserTest.class.getResourceAsStream(name)) {
            return in.readAllBytes();
        }
    }
}
