package io.github.hatsu3140.pianotab.converter;

import io.github.hatsu3140.pianotab.converter.model.Tuning;

/**
 * 変換の設定。
 *
 * @param difficulty 難易度
 * @param tuning     チューニング
 * @param capo       カポの位置（0〜7。0 = カポなし）
 * @param transpose  移調する量（半音単位。+2 なら全音上げる）
 */
public record ConversionOptions(Difficulty difficulty, Tuning tuning, int capo, int transpose) {

    public static final int MAX_CAPO = 7;
    public static final int MAX_TRANSPOSE = 12;

    /** 何も指定しないときの設定（中級・レギュラーチューニング・カポなし・移調なし） */
    public static final ConversionOptions DEFAULT =
            new ConversionOptions(Difficulty.INTERMEDIATE, Tuning.STANDARD, 0, 0);

    public ConversionOptions {
        if (difficulty == null || tuning == null) {
            throw new IllegalArgumentException("難易度とチューニングを指定してください。");
        }
        if (capo < 0 || capo > MAX_CAPO) {
            throw new IllegalArgumentException("カポは0〜" + MAX_CAPO + "の範囲で指定してください。");
        }
        if (Math.abs(transpose) > MAX_TRANSPOSE) {
            throw new IllegalArgumentException("移調は-" + MAX_TRANSPOSE + "〜+" + MAX_TRANSPOSE + "の範囲で指定してください。");
        }
    }
}
