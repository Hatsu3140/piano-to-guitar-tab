package io.github.hatsu3140.pianotab.web;

/**
 * 変換に失敗したとき、API が画面に返す JSON。
 *
 * @param message 画面に表示するメッセージ
 */
public record ConvertResponse(String message) {
}
