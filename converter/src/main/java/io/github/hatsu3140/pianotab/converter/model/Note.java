package io.github.hatsu3140.pianotab.converter.model;

/**
 * 1つの音符。
 *
 * @param pitch         MIDI ノート番号（例: 60 = 真ん中のド C4）
 * @param startTick     曲の先頭から数えた開始位置（tick 単位）
 * @param durationTicks 長さ（tick 単位）
 */
public record Note(int pitch, long startTick, long durationTicks) {
}
