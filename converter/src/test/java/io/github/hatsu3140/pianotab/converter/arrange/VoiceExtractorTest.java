package io.github.hatsu3140.pianotab.converter.arrange;

import io.github.hatsu3140.pianotab.converter.model.Arrangement;
import io.github.hatsu3140.pianotab.converter.model.Note;
import io.github.hatsu3140.pianotab.converter.model.Score;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VoiceExtractorTest {

    @Test
    void picksHighestAsMelodyAndLowestAsBass() {
        // C メジャーの和音（C3, E4, G4）→ メロディ G4、ベース C3
        Score score = new Score(480, List.of(
                new Note(48, 0, 480),
                new Note(64, 0, 480),
                new Note(67, 0, 480)));

        Arrangement arrangement = new VoiceExtractor().extract(score);

        assertEquals(List.of(new Note(67, 0, 480)), arrangement.melody());
        assertEquals(List.of(new Note(48, 0, 480)), arrangement.bass());
    }

    @Test
    void singleNoteBecomesMelodyOnly() {
        Score score = new Score(480, List.of(new Note(60, 0, 480)));

        Arrangement arrangement = new VoiceExtractor().extract(score);

        assertEquals(1, arrangement.melody().size());
        assertEquals(0, arrangement.bass().size());
    }
}
