package io.github.hatsu3140.pianotab.converter;

import io.github.hatsu3140.pianotab.converter.model.Measure;
import io.github.hatsu3140.pianotab.converter.model.SongInfo;

import java.util.List;

/** テストで使う共通のデータ */
public final class TestScores {

    /** 4/4 拍子 1小節だけの曲 */
    public static final SongInfo ONE_MEASURE = new SongInfo("test", 120, 480, List.of(new Measure(0, 1920, 4, 4)));

    private TestScores() {
    }
}
