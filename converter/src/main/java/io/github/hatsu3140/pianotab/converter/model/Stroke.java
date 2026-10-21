package io.github.hatsu3140.pianotab.converter.model;

/**
 * ストローク（弦をなぞるように和音を鳴らす）。
 *
 * @param startTick 和音の位置
 * @param soft      true なら静かな所の、軽く素早いストローク
 */
public record Stroke(long startTick, boolean soft) {
}
