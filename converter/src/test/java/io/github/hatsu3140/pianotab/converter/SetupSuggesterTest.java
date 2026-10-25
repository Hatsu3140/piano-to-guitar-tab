package io.github.hatsu3140.pianotab.converter;

import io.github.hatsu3140.pianotab.converter.model.Note;
import io.github.hatsu3140.pianotab.converter.model.Score;
import io.github.hatsu3140.pianotab.converter.model.TabNote;
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
        // （CGDGBD にカポ5でも F・C・G になる）。どちらもベースを全部開放弦で弾けるので上位に来る
        Score score = new Score(TestScores.ONE_MEASURE, List.of(), List.of(
                new Note(72, 0, 480), new Note(77, 480, 480), new Note(72, 960, 480), new Note(77, 1440, 480)),
                List.of(new Note(43, 0, 480), new Note(48, 480, 480), new Note(41, 960, 480), new Note(43, 1440, 480)));

        List<SetupSuggestion> suggestions = new SetupSuggester().suggest(score, ADVANCED, 3);

        assertEquals(100, suggestions.get(0).openBassPercent());
        assertEquals("ベースの 100% を開放弦で弾けます", suggestions.get(0).reasons().get(0));
        assertTrue(suggestions.stream().anyMatch(suggestion ->
                suggestion.tuning().equals("STANDARD") && suggestion.capo() == 3));
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
    void countsOpenStringsForBassAndMelody() {
        // レギュラー・カポなしの低い3本は E・A・D。G と B は高い弦の開放弦なので数えない
        assertTrue(SetupSuggester.isOpenBassString(40, Tuning.STANDARD, 0));
        assertTrue(SetupSuggester.isOpenBassString(52, Tuning.STANDARD, 0)); // 1オクターブ上の E でもよい
        assertFalse(SetupSuggester.isOpenBassString(43, Tuning.STANDARD, 0));
        assertTrue(SetupSuggester.isOpenBassString(43, Tuning.STANDARD, 3));
        // 高い3本は E・B・G
        assertTrue(SetupSuggester.isOpenMelodyString(71, Tuning.STANDARD, 0));
        assertFalse(SetupSuggester.isOpenMelodyString(62, Tuning.STANDARD, 0));
    }

    @Test
    void measuresMelodyHandMovementWithoutOpenStrings() {
        // カポ2: 1弦5 → 開放弦（ネック上の2フレット）→ 1弦7 → 2弦4。開放弦は飛ばすので、移動は 2 と 3 の平均
        List<TabNote> melody = List.of(
                new TabNote(new Note(69, 0, 480), 1, 5), new TabNote(new Note(66, 480, 480), 1, 2),
                new TabNote(new Note(71, 960, 480), 1, 7), new TabNote(new Note(63, 1440, 480), 2, 4));

        assertEquals(2.5, SetupSuggester.averageMelodyMove(melody, 2), 1e-9);
    }
}
