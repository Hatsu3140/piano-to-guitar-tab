package io.github.hatsu3140.pianotab.converter.model;

/**
 * TAB 譜上の1音（どの弦の何フレットを押さえるか）。
 *
 * @param note   元の音符
 * @param string 弦番号（1 = いちばん細い弦）
 * @param fret   フレット番号（0 = 開放弦）
 */
public record TabNote(Note note, int string, int fret) {
}
