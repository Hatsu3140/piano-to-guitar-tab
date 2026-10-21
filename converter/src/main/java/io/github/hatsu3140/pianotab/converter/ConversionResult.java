package io.github.hatsu3140.pianotab.converter;

import io.github.hatsu3140.pianotab.converter.model.TabScore;

/**
 * 変換結果。
 *
 * @param tabScore TAB 譜の中身
 * @param gp5      .gp5 ファイルの中身
 */
public record ConversionResult(TabScore tabScore, byte[] gp5) {
}
