package io.github.hatsu3140.pianotab.converter.fret;

import io.github.hatsu3140.pianotab.converter.Difficulty;
import io.github.hatsu3140.pianotab.converter.model.Arrangement;
import io.github.hatsu3140.pianotab.converter.model.Note;
import io.github.hatsu3140.pianotab.converter.model.TabNote;
import io.github.hatsu3140.pianotab.converter.model.TabScore;
import io.github.hatsu3140.pianotab.converter.model.Tuning;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.hatsu3140.pianotab.converter.TestScores.ONE_MEASURE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class FretAssignerTest {

    private final FretAssigner assigner = new FretAssigner(Tuning.STANDARD);

    @Test
    void fastPassagesMakeMovingHarder() {
        assertEquals(1.0, FretAssigner.speedFactor(960, 480));
        assertEquals(1.0, FretAssigner.speedFactor(480, 480));
        assertEquals(2.0, FretAssigner.speedFactor(240, 480));
        assertEquals(4.0, FretAssigner.speedFactor(60, 480));
    }

    @Test
    void openStringNotesUseFretZero() {
        // E4 は 1弦の開放、E2 は 6弦の開放
        Arrangement arrangement = new Arrangement(ONE_MEASURE,
                List.of(new Note(64, 0, 480)),
                List.of(new Note(40, 0, 480)));

        TabScore tab = assigner.assign(arrangement);

        assertEquals(List.of(new TabNote(new Note(64, 0, 480), 1, 0)), tab.melody());
        assertEquals(List.of(new TabNote(new Note(40, 0, 480), 6, 0)), tab.bass());
    }

    @Test
    void simultaneousNotesUseDifferentStrings() {
        Arrangement arrangement = new Arrangement(ONE_MEASURE,
                List.of(new Note(60, 0, 480)),
                List.of(new Note(59, 0, 480)));

        TabScore tab = assigner.assign(arrangement);

        assertEquals(2, tab.notes().size());
        assertNotEquals(tab.melody().get(0).string(), tab.bass().get(0).string());
    }

    @Test
    void movesNotesOutsideGuitarRangeByOctaves() {
        // C2 はギターより低いので C3 に、C7 は高すぎるので（19フレットの B5 まで）C5 に移す
        Arrangement arrangement = new Arrangement(ONE_MEASURE,
                List.of(new Note(96, 0, 480)),
                List.of(new Note(36, 0, 480)));

        TabScore tab = assigner.assign(arrangement);

        assertEquals(72, tab.melody().get(0).note().pitch());
        assertEquals(48, tab.bass().get(0).note().pitch());
    }

    @Test
    void choosesPositionsWithinReach() {
        // C3 と C5 を同時に弾く。C3 を5弦3フレット、C5 を1弦8フレットにすると指が届かないので、
        // C3 を6弦8フレットにして同じポジションで弾く
        Arrangement arrangement = new Arrangement(ONE_MEASURE,
                List.of(new Note(72, 0, 480)),
                List.of(new Note(48, 0, 480)));

        TabScore tab = assigner.assign(arrangement);

        assertEquals(new TabNote(new Note(72, 0, 480), 1, 8), tab.melody().get(0));
        assertEquals(new TabNote(new Note(48, 0, 480), 6, 8), tab.bass().get(0));
    }

    @Test
    void dropsBassWhenItCannotBeReached() {
        // B5 は1弦19フレットでしか出せず、F2 は6弦1フレットでしか出せない → 同時には弾けないのでベースを捨てる
        Arrangement arrangement = new Arrangement(ONE_MEASURE,
                List.of(new Note(83, 0, 480)),
                List.of(new Note(41, 0, 480)));

        TabScore tab = assigner.assign(arrangement);

        assertEquals(1, tab.melody().size());
        assertEquals(0, tab.bass().size());
    }

    @Test
    void cutsHeldNoteWhenNextNoteCannotBeReached() {
        // F2（6弦1フレット）を伸ばしている間に B5（1弦19フレット）が鳴る → F2 はそこで止める
        Arrangement arrangement = new Arrangement(ONE_MEASURE,
                List.of(new Note(83, 480, 480)),
                List.of(new Note(41, 0, 1920)));

        TabScore tab = assigner.assign(arrangement);

        assertEquals(new TabNote(new Note(41, 0, 480), 6, 1), tab.bass().get(0));
        assertEquals(1, tab.melody().size());
    }

    @Test
    void doesNotReuseStringOfHeldNote() {
        // E4（1弦開放）を伸ばしている間に G4 が鳴る → G4 は1弦ではなく2弦8フレット
        Arrangement arrangement = new Arrangement(ONE_MEASURE,
                List.of(new Note(64, 0, 960)),
                List.of(new Note(67, 480, 480)));

        TabScore tab = assigner.assign(arrangement);

        assertEquals(new TabNote(new Note(67, 480, 480), 2, 8), tab.bass().get(0));
    }

    @Test
    void addsCapoToOutputFrets() {
        // カポ3なら G4 は1弦の「カポの位置の開放」→ 出力は1弦3フレット
        FretAssigner withCapo = new FretAssigner(Tuning.STANDARD, 3, Difficulty.INTERMEDIATE);
        Arrangement arrangement = new Arrangement(ONE_MEASURE, List.of(new Note(67, 0, 480)), List.of());

        TabScore tab = withCapo.assign(arrangement);

        assertEquals(new TabNote(new Note(67, 0, 480), 1, 3), tab.melody().get(0));
        assertEquals(3, tab.capo());
    }

    @Test
    void keepsWithinFretRangeOfDifficulty() {
        // 入門は5フレットまで。E5（1弦12フレット）は1オクターブ下の E4（1弦開放）にする
        FretAssigner introductory = new FretAssigner(Tuning.STANDARD, 0, Difficulty.INTRODUCTORY);
        Arrangement arrangement = new Arrangement(ONE_MEASURE, List.of(new Note(76, 0, 480)), List.of());

        TabScore tab = introductory.assign(arrangement);

        assertEquals(new TabNote(new Note(64, 0, 480), 1, 0), tab.melody().get(0));
    }

    @Test
    void movesUnreachablePitchToAnotherOctave() {
        // ドロップD・5フレットまでだと G#2 は押さえられない（6弦は D2〜G2、5弦は A2〜）→ G#3 にする
        FretAssigner dropD = new FretAssigner(Tuning.DROP_D, 0, Difficulty.BEGINNER);
        Arrangement arrangement = new Arrangement(ONE_MEASURE, List.of(new Note(44, 0, 480)), List.of());

        TabScore tab = dropD.assign(arrangement);

        assertEquals(56, tab.melody().get(0).note().pitch());
    }
}
