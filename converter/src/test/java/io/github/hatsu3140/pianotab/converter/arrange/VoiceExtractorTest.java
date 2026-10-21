package io.github.hatsu3140.pianotab.converter.arrange;

import io.github.hatsu3140.pianotab.converter.model.Arrangement;
import io.github.hatsu3140.pianotab.converter.model.Note;
import io.github.hatsu3140.pianotab.converter.model.Score;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.hatsu3140.pianotab.converter.TestScores.ONE_MEASURE;
import static org.junit.jupiter.api.Assertions.assertEquals;

class VoiceExtractorTest {

    @Test
    void picksHighestAsMelodyAndLowestAsBass() {
        // C メジャーの和音（C3, E4, G4）→ メロディ G4、ベース C3
        Score score = new Score(ONE_MEASURE, List.of(
                new Note(48, 0, 480),
                new Note(64, 0, 480),
                new Note(67, 0, 480)));

        Arrangement arrangement = new VoiceExtractor().extract(score);

        assertEquals(List.of(new Note(67, 0, 480)), arrangement.melody());
        assertEquals(List.of(new Note(48, 0, 480)), arrangement.bass());
    }

    @Test
    void singleNoteBecomesMelodyOnly() {
        Score score = new Score(ONE_MEASURE, List.of(new Note(60, 0, 480)));

        Arrangement arrangement = new VoiceExtractor().extract(score);

        assertEquals(1, arrangement.melody().size());
        assertEquals(0, arrangement.bass().size());
    }

    @Test
    void accompanimentUnderHeldMelodyGoesToBass() {
        // C5 を2分音符で伸ばしている間に、伴奏の C4, G4（8分音符）が鳴る
        // → G4 はメロディではなくベース側になる
        Score score = new Score(ONE_MEASURE, List.of(
                new Note(72, 0, 960),
                new Note(60, 0, 240),
                new Note(67, 240, 240)));

        Arrangement arrangement = new VoiceExtractor().extract(score);

        assertEquals(List.of(new Note(72, 0, 960)), arrangement.melody());
        assertEquals(List.of(new Note(60, 0, 240), new Note(67, 240, 240)), arrangement.bass());
    }

    @Test
    void usesVocalStaffForMelody() {
        // 歌（E5, D5）より右手（G5）が高くても、メロディは歌から取る。ベースは左手
        Score score = new Score(ONE_MEASURE,
                List.of(new Note(76, 0, 960), new Note(74, 960, 960)),
                List.of(new Note(79, 0, 1920)),
                List.of(new Note(48, 0, 1920)));

        Arrangement arrangement = new VoiceExtractor().extract(score);

        assertEquals(List.of(new Note(76, 0, 960), new Note(74, 960, 960)), arrangement.melody());
        assertEquals(List.of(new Note(48, 0, 1920)), arrangement.bass());
    }

    @Test
    void takesMelodyFromRightHandAndBassFromLeftHand() {
        // 左手が右手より高い音を弾いても、メロディは右手の一番上、ベースは左手の一番下
        Score score = new Score(ONE_MEASURE, List.of(),
                List.of(new Note(67, 0, 480), new Note(60, 0, 480)),
                List.of(new Note(72, 0, 480), new Note(48, 0, 480)));

        Arrangement arrangement = new VoiceExtractor().extract(score);

        assertEquals(List.of(new Note(67, 0, 480)), arrangement.melody());
        assertEquals(List.of(new Note(48, 0, 480)), arrangement.bass());
    }
}
