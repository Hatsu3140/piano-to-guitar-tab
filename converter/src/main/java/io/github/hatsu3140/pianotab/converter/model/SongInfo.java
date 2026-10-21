package io.github.hatsu3140.pianotab.converter.model;

import java.util.List;

/**
 * 曲全体の情報（タイトル・テンポ・小節）。変換の最初から最後まで引き継がれる。
 *
 * @param title           曲名（なければ空文字）
 * @param tempo           テンポ（1分あたりの4分音符の数）
 * @param ticksPerQuarter 4分音符1つあたりの tick 数
 * @param measures        小節の一覧
 */
public record SongInfo(String title, int tempo, int ticksPerQuarter, List<Measure> measures) {
    public SongInfo {
        measures = List.copyOf(measures);
    }
}
