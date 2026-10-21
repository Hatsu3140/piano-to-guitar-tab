package io.github.hatsu3140.pianotab.converter.gp5;

import io.github.hatsu3140.pianotab.converter.gp5.Gp5Writer.Beat;
import io.github.hatsu3140.pianotab.converter.gp5.Gp5Writer.Duration;
import io.github.hatsu3140.pianotab.converter.model.Measure;
import io.github.hatsu3140.pianotab.converter.model.Note;
import io.github.hatsu3140.pianotab.converter.model.TabNote;
import io.github.hatsu3140.pianotab.converter.model.TabScore;
import io.github.hatsu3140.pianotab.converter.model.Tuning;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static io.github.hatsu3140.pianotab.converter.TestScores.ONE_MEASURE;
import static org.junit.jupiter.api.Assertions.assertEquals;

class Gp5WriterTest {

    private final Gp5Writer.Rhythm rhythm = new Gp5Writer.Rhythm(480);

    @Test
    void startsWithGuitarProVersion() {
        TabScore tab = new TabScore(ONE_MEASURE, Tuning.STANDARD, List.of(), List.of());

        byte[] gp5 = new Gp5Writer().write(tab);

        String version = "FICHIER GUITAR PRO v5.00";
        assertEquals(version.length(), gp5[0]);
        assertEquals(version, new String(gp5, 1, version.length(), StandardCharsets.US_ASCII));
    }

    @Test
    void fillsGapsWithRests() {
        // 2拍目から2分音符 → 4分休符, 2分音符, 4分休符
        TabNote note = new TabNote(new Note(64, 480, 960), 1, 0);

        List<Beat> beats = rhythm.toBeats(List.of(note), ONE_MEASURE.measures()).get(0);

        assertEquals(List.of(
                new Beat(new Duration(0, false), null, false),
                new Beat(new Duration(-1, false), note, false),
                new Beat(new Duration(0, false), null, false)), beats);
    }

    @Test
    void tiesNotesAcrossBarLines() {
        // 1小節目の4拍目から2分音符 → 1小節目に4分音符、2小節目にタイの4分音符
        List<Measure> measures = List.of(new Measure(0, 1920, 4, 4), new Measure(1920, 1920, 4, 4));
        TabNote note = new TabNote(new Note(64, 1440, 960), 1, 0);

        List<List<Beat>> beats = rhythm.toBeats(List.of(note), measures);

        assertEquals(new Beat(new Duration(0, false), note, false), beats.get(0).get(1));
        assertEquals(new Beat(new Duration(0, false), note, true), beats.get(1).get(0));
        assertEquals(new Beat(new Duration(-1, true), null, false), beats.get(1).get(1));
    }

    @Test
    void roundsToSixteenthNotes() {
        // 少しずれた位置（10 tick 後ろ）の音は 16分音符の位置にそろえる
        TabNote note = new TabNote(new Note(64, 10, 1910), 1, 0);

        List<Beat> beats = rhythm.toBeats(List.of(note), ONE_MEASURE.measures()).get(0);

        assertEquals(List.of(new Beat(new Duration(-2, false), note, false)), beats);
    }
}
