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
        // メロディ C6〜E6 は高すぎるので、弾きやすい高さ（1弦9フレットまで）に入る C4〜E4 まで下げる。
        // ベース C2〜G2 は低すぎるので1オクターブ上げる
        Arrangement arrangement = new Arrangement(ONE_MEASURE,
                List.of(new Note(84, 0, 480), new Note(86, 480, 480), new Note(88, 960, 480)),
                List.of(new Note(36, 0, 960), new Note(43, 960, 960)));

        Arrangement placed = placer.place(arrangement);

        assertEquals(List.of(new Note(60, 0, 480), new Note(62, 480, 480), new Note(64, 960, 480)), placed.melody());
        assertEquals(List.of(new Note(48, 0, 960), new Note(55, 960, 960)), placed.bass());
    }

    @Test
    void movesWholePhraseInsteadOfSingleHighNotes() {
        // 休符のあとの E5 A5 G5 E5 のうち A5 と G5 だけが高すぎる。その2音だけ下げると E5→A4 のような
        // 跳躍になるので、休符で区切られたまとまり4音をいっしょに1オクターブ下げる
        Arrangement arrangement = new Arrangement(ONE_MEASURE, List.of(
                new Note(64, 0, 480), new Note(64, 480, 480), new Note(64, 960, 480), new Note(64, 1440, 480),
                new Note(76, 2400, 480), new Note(81, 2880, 480), new Note(79, 3360, 480), new Note(76, 3840, 480)),
                List.of());

        Arrangement placed = placer.place(arrangement);

        assertEquals(List.of(64, 64, 64, 64, 64, 69, 67, 64),
                placed.melody().stream().map(Note::pitch).toList());
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
        // E6 は高すぎるので E4 まで下げる。E4 まで下げると E3 になり、範囲より低く、
        // 同時に鳴っているベース G3 よりも低くなるので、E4 はそのままにして、ベースも動かさない
        Arrangement arrangement = new Arrangement(ONE_MEASURE,
                List.of(new Note(64, 0, 960), new Note(88, 960, 960)),
                List.of(new Note(55, 0, 1920)));

        Arrangement placed = placer.place(arrangement);

        assertEquals(List.of(new Note(64, 0, 960), new Note(64, 960, 960)), placed.melody());
        assertEquals(List.of(new Note(55, 0, 1920)), placed.bass());
    }

    @Test
    void leavesBassAloneWhenTheMelodyWasBelowItOriginally() {
        // 最初の F#3 はもとからベース G3 より低いのでベースは動かさない。
        // F#3 自体はメロディの範囲（A3 から上）に入らないので F#4 に、高すぎる E6 は E4 に移る
        Arrangement arrangement = new Arrangement(ONE_MEASURE,
                List.of(new Note(54, 0, 480), new Note(88, 480, 480), new Note(88, 960, 960)),
                List.of(new Note(55, 0, 480)));

        Arrangement placed = placer.place(arrangement);

        assertEquals(List.of(new Note(66, 0, 480), new Note(64, 480, 480), new Note(64, 960, 960)), placed.melody());
        assertEquals(List.of(new Note(55, 0, 480)), placed.bass());
    }
}
