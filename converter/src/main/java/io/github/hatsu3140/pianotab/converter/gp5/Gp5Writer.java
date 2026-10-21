package io.github.hatsu3140.pianotab.converter.gp5;

import io.github.hatsu3140.pianotab.converter.model.Measure;
import io.github.hatsu3140.pianotab.converter.model.SongInfo;
import io.github.hatsu3140.pianotab.converter.model.TabNote;
import io.github.hatsu3140.pianotab.converter.model.TabScore;
import io.github.hatsu3140.pianotab.converter.model.Tuning;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 手順4: TAB 譜を Guitar Pro 5 形式（.gp5）のバイト列にする。
 *
 * <p>ギター1トラックで、声部1にメロディ、声部2にベースを入れる。
 * リズムは16分音符単位にそろえる（3連符などは近い位置に寄せられる）。
 * TODO: 3連符への対応。
 *
 * <p>カポを付けたときは、フレットはカポの分を足したネック上の実際の番号で書き、
 * トラック名と曲の最初の拍に「Capo N」と書く（Guitar Pro のカポ設定は使わない）。
 *
 * <p>ファイル形式は TuxGuitar や PyGuitarPro の実装を参考にしている。
 */
public class Gp5Writer {

    private static final String VERSION = "FICHIER GUITAR PRO v5.00";
    /** MIDI の音色番号（24 = ナイロン弦ギター） */
    private static final int NYLON_GUITAR_PROGRAM = 24;
    private static final int FRET_COUNT = 24;

    public byte[] write(TabScore tabScore) {
        SongInfo info = tabScore.info();
        LittleEndianWriter out = new LittleEndianWriter();

        out.byteSizeString(VERSION, 30);
        writeInfo(out, info.title());
        writeLyrics(out);
        writePageSetup(out);
        out.intByteSizeString("Moderate");
        out.writeInt(info.tempo());
        out.writeByte(0); // 調号
        out.writeInt(0); // オクターブ
        writeMidiChannels(out);
        for (int i = 0; i < 19; i++) {
            out.writeShort(-1); // コーダやセーニョなどの記号（使わない）
        }
        out.writeInt(0); // マスターリバーブ

        List<Measure> measures = info.measures();
        out.writeInt(measures.size());
        out.writeInt(1); // トラック数
        writeMeasureHeaders(out, measures, info.ticksPerQuarter());
        String capoText = tabScore.capo() > 0 ? "Capo " + tabScore.capo() : null;
        writeTrack(out, tabScore.tuning(), capoText == null ? "Guitar" : "Guitar (" + capoText + ")");
        out.skip(2);

        Rhythm rhythm = new Rhythm(info.ticksPerQuarter());
        List<List<Beat>> melodyBeats = rhythm.toBeats(tabScore.melody(), measures);
        List<List<Beat>> bassBeats = rhythm.toBeats(tabScore.bass(), measures);
        for (int i = 0; i < measures.size(); i++) {
            writeVoice(out, melodyBeats.get(i), false, i == 0 ? capoText : null);
            writeVoice(out, bassBeats.get(i), true, null);
            out.writeByte(0); // 改行なし
        }
        return out.toByteArray();
    }

    private static void writeInfo(LittleEndianWriter out, String title) {
        out.intByteSizeString(title); // 曲名
        for (int i = 0; i < 8; i++) {
            out.intByteSizeString(""); // 副題・アーティスト・アルバム・作詞・作曲・著作権・採譜者・説明
        }
        out.writeInt(0); // メモの行数
    }

    private static void writeLyrics(LittleEndianWriter out) {
        out.writeInt(0); // 歌詞を表示するトラック
        for (int i = 0; i < 5; i++) {
            out.writeInt(0); // 開始小節
            out.writeInt(0); // 歌詞（空）
        }
    }

    private static void writePageSetup(LittleEndianWriter out) {
        out.writeInt(210); // 用紙の幅（mm, A4）
        out.writeInt(297); // 用紙の高さ
        out.writeInt(10); // 余白 左
        out.writeInt(10); // 余白 右
        out.writeInt(15); // 余白 上
        out.writeInt(10); // 余白 下
        out.writeInt(100); // 表示倍率（%）
        out.writeShort(0x1ff); // ヘッダー・フッターの表示項目
        out.intByteSizeString("%TITLE%");
        out.intByteSizeString("%SUBTITLE%");
        out.intByteSizeString("%ARTIST%");
        out.intByteSizeString("%ALBUM%");
        out.intByteSizeString("Words by %WORDS%");
        out.intByteSizeString("Music by %MUSIC%");
        out.intByteSizeString("Words & Music by %WORDSMUSIC%");
        out.intByteSizeString("Copyright %COPYRIGHT%");
        out.intByteSizeString("All Rights Reserved - International Copyright Secured");
        out.intByteSizeString("Page %N%/%P%");
    }

    private static void writeMidiChannels(LittleEndianWriter out) {
        for (int i = 0; i < 64; i++) {
            out.writeInt(NYLON_GUITAR_PROGRAM);
            out.writeByte(13); // 音量（0〜16）
            out.writeByte(8); // 左右の位置（8 = 中央）
            out.writeByte(0); // コーラス
            out.writeByte(0); // リバーブ
            out.writeByte(0); // フェイザー
            out.writeByte(0); // トレモロ
            out.skip(2);
        }
    }

    private static void writeMeasureHeaders(LittleEndianWriter out, List<Measure> measures, int ticksPerQuarter) {
        Measure previous = null;
        for (Measure measure : measures) {
            if (previous != null) {
                out.skip(1);
            }
            boolean timeSignatureChanged = previous == null
                    || previous.beats() != measure.beats()
                    || previous.beatType() != measure.beatType();
            int flags = timeSignatureChanged ? 0x03 : 0x00;
            out.writeByte(flags);
            if (timeSignatureChanged) {
                out.writeByte(measure.beats());
                out.writeByte(measure.beatType());
                for (int beam : beamGroups(measure)) {
                    out.writeByte(beam);
                }
            }
            out.skip(1);
            out.writeByte(0); // ハネ（シャッフル）なし
            previous = measure;
        }
    }

    /** 8分音符の連桁（はた）のまとめ方。表示だけに使われる */
    private static int[] beamGroups(Measure measure) {
        int[] groups = {2, 2, 2, 2};
        if (measure.beatType() <= 8) {
            int eighths = 8 / measure.beatType() * measure.beats();
            Arrays.fill(groups, eighths / 4);
            groups[0] += eighths % 4;
        }
        return groups;
    }

    private static void writeTrack(LittleEndianWriter out, Tuning tuning, String name) {
        out.skip(1);
        out.writeByte(0x08); // 表示するトラック
        out.byteSizeString(name, 40);
        out.writeInt(tuning.stringCount());
        for (int i = 0; i < 7; i++) {
            out.writeInt(i < tuning.stringCount() ? tuning.openPitch(i + 1) : 0);
        }
        out.writeInt(1); // MIDI ポート
        out.writeInt(1); // MIDI チャンネル
        out.writeInt(2); // エフェクト用 MIDI チャンネル
        out.writeInt(FRET_COUNT);
        out.writeInt(0); // カポ（フレット番号にカポの分を含めているので 0）
        out.writeByte(255); // トラックの色（赤）
        out.writeByte(0);
        out.writeByte(0);
        out.skip(1);
        out.writeShort(0x03); // TAB 譜と五線譜を表示
        out.writeByte(0); // 自動アクセント
        out.writeByte(0); // MIDI バンク
        out.writeByte(0); // ヒューマナイズ
        out.writeInt(0); // 音部記号の移調
        out.writeInt(0);
        out.writeInt(100);
        out.skip(12);
        out.writeInt(-1); // RSE（Guitar Pro の音源）は使わない
        out.writeInt(0);
        out.writeInt(-1);
        out.writeShort(-1);
        out.skip(1);
    }

    /** @param firstBeatText 最初の拍に付ける文字（なければ null） */
    private static void writeVoice(LittleEndianWriter out, List<Beat> beats, boolean isSecondVoice,
                                   String firstBeatText) {
        boolean hasNotes = beats.stream().anyMatch(beat -> beat.note() != null);
        if (isSecondVoice && !hasNotes) {
            // 音のない第2声部は休符も書かず「空」にする
            out.writeInt(1);
            out.writeByte(0x40);
            out.writeByte(0x00); // 空の拍
            out.writeByte(0); // 4分音符
            out.writeByte(0); // 音なし
            out.writeShort(0);
            return;
        }
        out.writeInt(beats.size());
        for (int i = 0; i < beats.size(); i++) {
            writeBeat(out, beats.get(i), i == 0 ? firstBeatText : null);
        }
    }

    private static void writeBeat(LittleEndianWriter out, Beat beat, String text) {
        int flags = beat.duration().dotted() ? 0x01 : 0x00;
        if (text != null) {
            flags |= 0x04;
        }
        if (beat.note() == null) {
            flags |= 0x40;
        }
        out.writeByte(flags);
        if (beat.note() == null) {
            out.writeByte(0x02); // 休符
        }
        out.writeByte(beat.duration().value());
        if (text != null) {
            out.intByteSizeString(text);
        }
        if (beat.note() == null) {
            out.writeByte(0);
        } else {
            TabNote note = beat.note();
            out.writeByte(1 << (7 - note.string()));
            out.writeByte(0x20); // 音の種類とフレットを書く
            out.writeByte(beat.tied() ? 2 : 1); // 2 = タイで前の音から続く, 1 = 普通の音
            out.writeByte(note.fret());
            out.writeByte(0);
        }
        out.writeShort(0);
    }

    /** Guitar Pro の音符の長さ。value: -2 = 全音符, -1 = 2分, 0 = 4分, 1 = 8分, 2 = 16分 */
    record Duration(int value, boolean dotted) {
    }

    /** 1拍分（1つの音または休符） */
    record Beat(Duration duration, TabNote note, boolean tied) {
    }

    /** 音の並びを、小節ごとの Guitar Pro の拍の並びに変換する */
    static final class Rhythm {
        private final long grid;
        private final long[] lengths;
        private final Duration[] durations;

        Rhythm(int ticksPerQuarter) {
            long quarter = ticksPerQuarter;
            this.grid = quarter / 4; // 16分音符
            this.lengths = new long[] {
                    quarter * 4, quarter * 3, quarter * 2, quarter * 3 / 2,
                    quarter, quarter * 3 / 4, quarter / 2, quarter / 4};
            this.durations = new Duration[] {
                    new Duration(-2, false), new Duration(-1, true), new Duration(-1, false), new Duration(0, true),
                    new Duration(0, false), new Duration(1, true), new Duration(1, false), new Duration(2, false)};
        }

        List<List<Beat>> toBeats(List<TabNote> notes, List<Measure> measures) {
            List<Event> events = toEvents(notes);
            List<List<Beat>> result = new ArrayList<>();
            int eventIndex = 0;
            for (Measure measure : measures) {
                long measureStart = quantize(measure.startTick());
                long measureEnd = quantize(measure.startTick() + measure.lengthTicks());
                while (eventIndex < events.size() && events.get(eventIndex).end() <= measureStart) {
                    eventIndex++;
                }

                List<Beat> beats = new ArrayList<>();
                long cursor = measureStart;
                for (int i = eventIndex; i < events.size() && events.get(i).start() < measureEnd; i++) {
                    Event event = events.get(i);
                    long start = Math.max(event.start(), measureStart);
                    long end = Math.min(event.end(), measureEnd);
                    addBeats(beats, start - cursor, null, false);
                    // 前の小節から続いている音はタイにする
                    addBeats(beats, end - start, event.note(), event.start() < measureStart);
                    cursor = end;
                }
                addBeats(beats, measureEnd - cursor, null, false);
                result.add(beats);
            }
            return result;
        }

        /** 音の開始・終了を16分音符単位にそろえ、重なりをなくす */
        private List<Event> toEvents(List<TabNote> notes) {
            List<TabNote> sorted = new ArrayList<>(notes);
            sorted.sort(Comparator.comparingLong(note -> note.note().startTick()));
            List<Event> events = new ArrayList<>();
            for (TabNote note : sorted) {
                long start = quantize(note.note().startTick());
                long end = Math.max(start + grid, quantize(note.note().startTick() + note.note().durationTicks()));
                if (!events.isEmpty()) {
                    Event previous = events.get(events.size() - 1);
                    if (previous.start() == start) {
                        continue; // 同じ位置に2つ目の音は置けないので捨てる
                    }
                    if (previous.end() > start) {
                        events.set(events.size() - 1, new Event(previous.start(), start, previous.note()));
                    }
                }
                events.add(new Event(start, end, note));
            }
            return events;
        }

        /** 長さ length を、書ける音符の長さの組み合わせに分けて追加する */
        private void addBeats(List<Beat> beats, long length, TabNote note, boolean tiedFromPrevious) {
            boolean tied = tiedFromPrevious;
            long remaining = length;
            for (int i = 0; i < lengths.length && remaining >= grid; ) {
                if (lengths[i] <= remaining) {
                    beats.add(new Beat(durations[i], note, note != null && tied));
                    remaining -= lengths[i];
                    tied = true;
                } else {
                    i++;
                }
            }
        }

        private long quantize(long tick) {
            return Math.round((double) tick / grid) * grid;
        }

        private record Event(long start, long end, TabNote note) {
        }
    }

    /** Guitar Pro 形式で使う、リトルエンディアン（下位バイトが先）の書き込み */
    private static final class LittleEndianWriter {
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        void writeByte(int value) {
            bytes.write(value);
        }

        void writeShort(int value) {
            bytes.write(value);
            bytes.write(value >> 8);
        }

        void writeInt(int value) {
            for (int i = 0; i < 4; i++) {
                bytes.write(value >> (8 * i));
            }
        }

        void skip(int count) {
            for (int i = 0; i < count; i++) {
                bytes.write(0);
            }
        }

        /** 長さ1バイト + 文字列（size バイトになるまで 0 で埋める） */
        void byteSizeString(String text, int size) {
            byte[] encoded = encode(text, size);
            writeByte(encoded.length);
            bytes.writeBytes(encoded);
            skip(size - encoded.length);
        }

        /** 長さ+1（4バイト） + 長さ（1バイト） + 文字列 */
        void intByteSizeString(String text) {
            byte[] encoded = encode(text, 255);
            writeInt(encoded.length + 1);
            writeByte(encoded.length);
            bytes.writeBytes(encoded);
        }

        private static byte[] encode(String text, int maxBytes) {
            byte[] encoded = text.getBytes(StandardCharsets.UTF_8);
            if (encoded.length <= maxBytes) {
                return encoded;
            }
            // 文字の途中で切らないように、収まる文字数まで減らす
            String shortened = text;
            while (encoded.length > maxBytes) {
                shortened = shortened.substring(0, shortened.offsetByCodePoints(0, shortened.codePointCount(0, shortened.length()) - 1));
                encoded = shortened.getBytes(StandardCharsets.UTF_8);
            }
            return encoded;
        }

        byte[] toByteArray() {
            return bytes.toByteArray();
        }
    }
}
