package io.github.hatsu3140.pianotab.converter;

import io.github.hatsu3140.pianotab.converter.model.TabScore;

/**
 * 変換結果。
 *
 * @param tabScore TAB 譜の中身
 * @param gp5      .gp5 ファイルの中身
 * @param report   変換レポート
 */
public record ConversionResult(TabScore tabScore, byte[] gp5, ConversionReport report) {
}
