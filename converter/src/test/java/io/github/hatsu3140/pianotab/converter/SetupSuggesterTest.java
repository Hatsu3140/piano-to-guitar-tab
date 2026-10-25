package io.github.hatsu3140.pianotab.converter;

import io.github.hatsu3140.pianotab.converter.model.Note;
import io.github.hatsu3140.pianotab.converter.model.Score;
import io.github.hatsu3140.pianotab.converter.model.Tuning;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SetupSuggesterTest {

    private static final ConversionOptions ADVANCED =
            new ConversionOptions(Difficulty.ADVANCED, Tuning.STANDARD, 0, 0);

    @Test
    void prefersCapoThatMakesBassOpenStrings() {
        // ベースが G・C・F・G。レギュラーにカポ3なら、6・5・4弦の開放弦が G・C・F になる
        Score score = new Score(TestScores.ONE_MEASURE, List.of(), List.of(
                new Note(72, 0, 480), new Note(76, 480, 480), new Note(79, 960, 480), new Note(76, 1440, 480)),
                List.of(new Note(43, 0, 480), new Note(48, 480, 480), new Note(41, 960, 480), new Note(43, 1440, 480)));

        List<SetupSuggestion> suggestions = new SetupSuggester().suggest(score, ADVANCED, 3);

        SetupSuggestion best = suggestions.get(0);
        assertEquals("STANDARD", best.tuning());
        assertEquals(3, best.capo());
        assertEquals(100, best.openBassPercent());
        assertEquals("ベースの 100% を開放弦で弾けます", best.reasons().get(0));
    }

    @Test
    void returnsBestFirst() throws Exception {
        List<SetupSuggestion> suggestions;
        try (InputStream in = getClass().getResourceAsStream("/two-hands.musicxml")) {
            suggestions = new SetupSuggester().suggest(in, ADVANCED, SetupSuggester.DEFAULT_LIMIT);
        }

        assertEquals(SetupSuggester.DEFAULT_LIMIT, suggestions.size());
        for (int i = 1; i < suggestions.size(); i++) {
            assertTrue(suggestions.get(i - 1).score() >= suggestions.get(i).score());
        }
        for (SetupSuggestion suggestion : suggestions) {
            assertTrue(Tuning.PRESETS.containsKey(suggestion.tuning()));
        }
    }

    @Test
    void countsOnlyTheThreeBassStrings() {
        // レギュラー・カポなしの低い3本は E・A・D。G と B は高い弦の開放弦なので数えない
        assertTrue(SetupSuggester.isOpenBassString(40, Tuning.STANDARD, 0));
        assertTrue(SetupSuggester.isOpenBassString(52, Tuning.STANDARD, 0)); // 1オクターブ上の E でもよい
        assertFalse(SetupSuggester.isOpenBassString(43, Tuning.STANDARD, 0));
        assertTrue(SetupSuggester.isOpenBassString(43, Tuning.STANDARD, 3));
    }
}
