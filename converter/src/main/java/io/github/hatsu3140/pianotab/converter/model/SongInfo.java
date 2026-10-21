package io.github.hatsu3140.pianotab.converter.model;

import java.util.List;

/**
 * 曲全体の情報（タイトル・テンポ・小節・強弱記号）。変換の最初から最後まで引き継がれる。
 *
 * @param title           曲名（なければ空文字）
 * @param tempo           テンポ（1分あたりの4分音符の数）
 * @param ticksPerQuarter 4分音符1つあたりの tick 数
 * @param measures        小節の一覧
 * @param dynamics        強弱記号の一覧（位置の順）
 */
public record SongInfo(String title, int tempo, int ticksPerQuarter, List<Measure> measures,
                       List<Dynamic> dynamics) {
    public SongInfo {
        measures = List.copyOf(measures);
        dynamics = List.copyOf(dynamics);
    }

    /** 強弱記号のない曲 */
    public SongInfo(String title, int tempo, int ticksPerQuarter, List<Measure> measures) {
        this(title, tempo, ticksPerQuarter, measures, List.of());
    }

    /** tick の位置での強さ。それより前に強弱記号がなければ null */
    public DynamicLevel dynamicAt(long tick) {
        DynamicLevel level = null;
        for (Dynamic dynamic : dynamics) {
            if (dynamic.startTick() > tick) {
                break;
            }
            level = dynamic.level();
        }
        return level;
    }
}
