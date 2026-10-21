package io.github.hatsu3140.pianotab.web;

/**
 * 変換 API が画面に返す JSON。
 *
 * @param fileName  アップロードされたファイル名
 * @param noteCount TAB 譜にできた音の数
 * @param message   画面に表示するメッセージ
 */
public record ConvertResponse(String fileName, int noteCount, String message) {
}
