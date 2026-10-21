package io.github.hatsu3140.pianotab.converter.arrange;

import io.github.hatsu3140.pianotab.converter.model.Arrangement;
import io.github.hatsu3140.pianotab.converter.model.Note;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.hatsu3140.pianotab.converter.TestScores.ONE_MEASURE;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OctavePlacerTest {

    private final OctavePlacer placer = new OctavePlacer();

    @Test
    void lowersHighMelodyAndRaisesLowBassByOctaves() {
        // メロディ C6〜E6 は高すぎるので1オクターブ下げ、ベース C2〜G2 は低すぎるので1オクターブ上げる
        Arrangement arrangement = new Arrangement(ONE_MEASURE,
                List.of(new Note(84, 0, 480), new Note(86, 480, 480), new Note(88, 960, 480)),
                List.of(new Note(36, 0, 960), new Note(43, 960, 960)));

        Arrangement placed = placer.place(arrangement);

        assertEquals(List.of(new Note(72, 0, 480), new Note(74, 480, 480), new Note(76, 960, 480)), placed.melody());
        assertEquals(List.of(new Note(48, 0, 960), new Note(55, 960, 960)), placed.bass());
    }

    @Test
    void keepsVoicesThatAreAlreadyPlayable() {
        Arrangement arrangement = new Arrangement(ONE_MEASURE,
                List.of(new Note(64, 0, 480), new Note(76, 480, 480)),
                List.of(new Note(40, 0, 960)));

        Arrangement placed = placer.place(arrangement);

        assertEquals(arrangement.melody(), placed.melody());
        assertEquals(arrangement.bass(), placed.bass());
    }

    @Test
    void keepsMelodyAboveBassAfterShifting() {
        // メロディ E4, E6 は高すぎるので1オクターブ下がって E3, E5 になる。
        // すると E3 が同時に鳴っているベース G3 より低くなるので、ベースを G2 に下げる
        Arrangement arrangement = new Arrangement(ONE_MEASURE,
                List.of(new Note(64, 0, 960), new Note(88, 960, 960)),
                List.of(new Note(55, 0, 1920)));

        Arrangement placed = placer.place(arrangement);

        assertEquals(List.of(new Note(52, 0, 960), new Note(76, 960, 960)), placed.melody());
        assertEquals(List.of(new Note(43, 0, 1920)), placed.bass());
    }

    @Test
    void leavesMelodyBelowBassWhenTheOriginalIsSo() {
        // メロディ全体が1オクターブ下がる。最初の F#3 はもとからベース G3 より低いので、ベースは動かさない
        Arrangement arrangement = new Arrangement(ONE_MEASURE,
                List.of(new Note(54, 0, 480), new Note(88, 480, 480), new Note(88, 960, 960)),
                List.of(new Note(55, 0, 480)));

        Arrangement placed = placer.place(arrangement);

        assertEquals(List.of(new Note(42, 0, 480), new Note(76, 480, 480), new Note(76, 960, 960)), placed.melody());
        assertEquals(List.of(new Note(55, 0, 480)), placed.bass());
    }
}
