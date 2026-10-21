package io.github.hatsu3140.pianotab.converter;

import java.util.ArrayList;
import java.util.List;

/**
 * 変換レポート。元の楽譜からどれだけ音を減らしたか、和音やストロークをいくつ入れたか、
 * 弾けない所が残っていないかをまとめる。
 *
 * @param melodyNotes        メロディの音の数（元の楽譜から取り出した数）
 * @param droppedMelodyNotes どう押さえても弾けず、省いたメロディの音の数
 * @param bassNotes          ベースの音の数（難易度に合わせて減らしたあと）
 * @param droppedBassNotes   指が届かず省いたベースの音の数
 * @param chords             和音（内声を足した所）の数
 * @param strokes            ストロークの数
 * @param barres             セーハが必要な所の数
 * @param trimmedNotes       のばしたままだと次の音が押さえられないので、途中で止めた音の数
 * @param unplayablePlaces   弾けない押さえ方が残っている所の数（0 になるように作っている）
 */
public record ConversionReport(int melodyNotes, int droppedMelodyNotes, int bassNotes, int droppedBassNotes,
                               int chords, int strokes, int barres, int trimmedNotes,
                               int unplayablePlaces) {

    /** 画面に出す説明文 */
    public List<String> messages() {
        List<String> messages = new ArrayList<>();
        messages.add("メロディ " + (melodyNotes - droppedMelodyNotes) + " 音、ベース "
                + (bassNotes - droppedBassNotes) + " 音、和音 " + chords + " か所、ストローク " + strokes + " か所");
        if (droppedMelodyNotes > 0) {
            messages.add("ギターでは押さえられないメロディの音を " + droppedMelodyNotes + " 音省きました。");
        }
        if (droppedBassNotes > 0) {
            messages.add("メロディと同時に押さえられないベースの音を " + droppedBassNotes + " 音省きました。");
        }
        if (barres > 0) {
            messages.add("セーハを使う所が " + barres + " か所あります。");
        }
        if (trimmedNotes > 0) {
            messages.add("のばしたままだと次の音が押さえられない所で、音を " + trimmedNotes + " 個短くしました。");
        }
        if (unplayablePlaces > 0) {
            messages.add("押さえにくい所が " + unplayablePlaces + " か所残っています。TuxGuitar などで直してください。");
        }
        return messages;
    }
}
