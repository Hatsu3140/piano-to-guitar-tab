package io.github.hatsu3140.pianotab.converter.model;

/**
 * 強弱記号（ここから先はこの強さで弾く、という指示）。
 *
 * @param startTick 記号の位置（tick 単位）
 * @param level     強さ
 */
public record Dynamic(long startTick, DynamicLevel level) {
}
