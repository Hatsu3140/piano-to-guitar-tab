package io.github.hatsu3140.pianotab.converter.gp5;

import io.github.hatsu3140.pianotab.converter.model.DynamicLevel;
import io.github.hatsu3140.pianotab.converter.model.Measure;
import io.github.hatsu3140.pianotab.converter.model.SongInfo;
import io.github.hatsu3140.pianotab.converter.model.Stroke;
import io.github.hatsu3140.pianotab.converter.model.TabNote;
import io.github.hatsu3140.pianotab.converter.model.TabScore;
import io.github.hatsu3140.pianotab.converter.model.Tuning;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 手順4: TAB 譜を Guitar Pro 5 形式（.gp5）のバイト列にする。
 *
 * <p>ギター1トラックで、声部1にメロディ（と和音の内声）、声部2にベースを入れる。
 * ストロークを付ける和音には、拍のエフェクトとしてストロークを書く。
 * リズムは16分音符単位か、8分音符の3連符単位にそろえる（5連符などは近い位置に寄せる）。
 * 強弱記号があれば、音の強さとして書く。
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
    /** ストロークの速さ（Guitar Pro の値。4 = 16分音符, 3 = 32分音符） */
    private static final int STROKE_SIXTEENTH = 4;
    private static final int STROKE_THIRTY_SECOND = 3;

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
        List<TabNote> upperVoice = new ArrayList<>(tabScore.melody());
        upperVoice.addAll(tabScore.inner());
        Map<Long, Stroke> strokes = new HashMap<>();
        tabScore.strokes().forEach(stroke -> strokes.put(stroke.startTick(), stroke));
        List<List<Beat>> melodyBeats = rhythm.toBeats(upperVoice, measures, strokes);
        List<List<Beat>> bassBeats = rhythm.toBeats(tabScore.bass(), measures, Map.of());
        for (int i = 0; i < measures.size(); i++) {
            writeVoice(out, melodyBeats.get(i), false, i == 0 ? capoText : null, info);
            writeVoice(out, bassBeats.get(i), true, null, info);
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
        for (Measure original : measures) {
            Measure measure = fitTimeSignature(original, ticksPerQuarter);
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

    /**
     * 拍子記号より中身が長い小節（古い楽譜で 4/4 と書いて8拍ある小節など）は、
     * 中身に合わせた拍子にする（8拍なら 8/4）。そうしないと Guitar Pro で小節からはみ出す。
     */
    static Measure fitTimeSignature(Measure measure, int ticksPerQuarter) {
        long beatLength = (long) ticksPerQuarter * 4 / measure.beatType();
        long signatureLength = beatLength * measure.beats();
        if (measure.lengthTicks() <= signatureLength || measure.lengthTicks() % beatLength != 0) {
            return measure;
        }
        int beats = (int) (measure.lengthTicks() / beatLength);
        if (beats > 127) {
            return measure;
        }
        return new Measure(measure.startTick(), measure.lengthTicks(), beats, measure.beatType());
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
                                   String firstBeatText, SongInfo info) {
        boolean hasNotes = beats.stream().anyMatch(beat -> !beat.notes().isEmpty());
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
            writeBeat(out, beats.get(i), i == 0 ? firstBeatText : null, info);
        }
    }

    private static void writeBeat(LittleEndianWriter out, Beat beat, String text, SongInfo info) {
        boolean rest = beat.notes().isEmpty();
        Stroke stroke = beat.tied() ? null : beat.stroke();
        int flags = beat.duration().dotted() ? 0x01 : 0x00;
        if (text != null) {
            flags |= 0x04;
        }
        if (stroke != null) {
            flags |= 0x08;
        }
        if (beat.duration().triplet()) {
            flags |= 0x20;
        }
        if (rest) {
            flags |= 0x40;
        }
        out.writeByte(flags);
        if (rest) {
            out.writeByte(0x02); // 休符
        }
        out.writeByte(beat.duration().value());
        if (beat.duration().triplet()) {
            out.writeInt(3); // 3連符
        }
        if (text != null) {
            out.intByteSizeString(text);
        }
        if (stroke != null) {
            writeStroke(out, stroke);
        }
        int strings = 0;
        for (TabNote note : beat.notes()) {
            strings |= 1 << (7 - note.string());
        }
        out.writeByte(strings);
        // 弦の番号順（1弦から）に書く
        List<TabNote> notes = new ArrayList<>(beat.notes());
        notes.sort(Comparator.comparingInt(TabNote::string));
        for (TabNote note : notes) {
            // 強さ。Guitar Pro では 1 = ppp 〜 8 = fff で、書かなければ f になる
            DynamicLevel level = beat.tied() ? null : info.dynamicAt(note.note().startTick());
            boolean writeDynamic = level != null && level != DynamicLevel.F;
            out.writeByte(0x20 | (writeDynamic ? 0x10 : 0)); // 音の種類とフレット（と強さ）を書く
            out.writeByte(beat.tied() ? 2 : 1); // 2 = タイで前の音から続く, 1 = 普通の音
            if (writeDynamic) {
                out.writeByte(level.ordinal() + 1);
            }
            out.writeByte(note.fret());
            out.writeByte(0);
        }
        out.writeShort(0);
    }

    /**
     * ストローク（拍のエフェクト）。向きは TuxGuitar の「ダウン」（低い弦から高い弦へ。譜面では↑の矢印）で、
     * 普通は16分音符、軽いストロークは32分音符の速さで鳴らす。
     * バイトの順番は「アップの速さ」「ダウンの速さ」（TuxGuitar の GP5 読み込みと同じ）
     */
    private static void writeStroke(LittleEndianWriter out, Stroke stroke) {
        out.writeByte(0x40); // ストロークあり
        out.writeByte(0x00);
        out.writeByte(0); // アップ（使わない）
        out.writeByte(stroke.soft() ? STROKE_THIRTY_SECOND : STROKE_SIXTEENTH); // ダウン
    }

    /**
     * Guitar Pro の音符の長さ。value: -2 = 全音符, -1 = 2分, 0 = 4分, 1 = 8分, 2 = 16分。
     * triplet が true なら3連符（3つで value の音符2つ分の長さ）
     */
    record Duration(int value, boolean dotted, boolean triplet) {
        Duration(int value, boolean dotted) {
            this(value, dotted, false);
        }
    }

    /** 1拍分（音・和音、または休符）。notes が空なら休符 */
    record Beat(Duration duration, List<TabNote> notes, boolean tied, Stroke stroke) {
        Beat {
            notes = List.copyOf(notes);
        }

        /** 1音（null なら休符）だけで、ストロークのない拍 */
        Beat(Duration duration, TabNote note, boolean tied) {
            this(duration, note == null ? List.of() : List.of(note), tied, null);
        }

        /** 最初の音（休符なら null） */
        TabNote note() {
            return notes.isEmpty() ? null : notes.get(0);
        }
    }

    /**
     * 音の並びを、小節ごとの Guitar Pro の拍の並びに変換する。
     *
     * <p>リズムは、4分音符1つ分の区間ごとに「16分音符単位」か「3連符単位」のどちらかにそろえる。
     * 区間の中の音の位置が3連符の位置（4分音符の 1/3, 2/3）に近ければ3連符にする。
     */
    static final class Rhythm {
        private final long quarter;
        private final long straightGrid;
        private final long tripletGrid;
        private final long[] straightLengths;
        private final Duration[] straightDurations;
        private final long[] tripletLengths;
        private final Duration[] tripletDurations;

        Rhythm(int ticksPerQuarter) {
            this.quarter = ticksPerQuarter;
            this.straightGrid = quarter / 4; // 16分音符
            this.tripletGrid = quarter / 3; // 8分音符の3連符
            this.straightLengths = new long[] {
                    quarter * 4, quarter * 3, quarter * 2, quarter * 3 / 2,
                    quarter, quarter * 3 / 4, quarter / 2, quarter / 4};
            this.straightDurations = new Duration[] {
                    new Duration(-2, false), new Duration(-1, true), new Duration(-1, false), new Duration(0, true),
                    new Duration(0, false), new Duration(1, true), new Duration(1, false), new Duration(2, false)};
            this.tripletLengths = new long[] {quarter * 2 / 3, quarter / 3};
            this.tripletDurations = new Duration[] {new Duration(0, false, true), new Duration(1, false, true)};
        }

        List<List<Beat>> toBeats(List<TabNote> notes, List<Measure> measures) {
            return toBeats(notes, measures, Map.of());
        }

        /** @param strokes 鳴り始めの位置（元の tick）ごとのストローク */
        List<List<Beat>> toBeats(List<TabNote> notes, List<Measure> measures, Map<Long, Stroke> strokes) {
            Grid grid = new Grid(notes, measures);
            List<Event> events = toEvents(notes, grid, strokes);
            List<List<Beat>> result = new ArrayList<>();
            int eventIndex = 0;
            for (Measure measure : measures) {
                long measureStart = grid.measureStart(measure);
                long measureEnd = grid.measureEnd(measure);
                while (eventIndex < events.size() && events.get(eventIndex).end() <= measureStart) {
                    eventIndex++;
                }

                List<Beat> beats = new ArrayList<>();
                long cursor = measureStart;
                for (int i = eventIndex; i < events.size() && events.get(i).start() < measureEnd; i++) {
                    Event event = events.get(i);
                    long start = Math.max(event.start(), measureStart);
                    long end = Math.min(event.end(), measureEnd);
                    addBeats(beats, cursor, start, List.of(), false, null, grid);
                    // 前の小節から続いている音はタイにする
                    addBeats(beats, start, end, event.notes(), event.start() < measureStart, event.stroke(), grid);
                    cursor = end;
                }
                addBeats(beats, cursor, measureEnd, List.of(), false, null, grid);
                result.add(beats);
            }
            return result;
        }

        /** 音の開始・終了をそろえ、重なりをなくす。同じ位置で鳴り始める音は和音にまとめる */
        private List<Event> toEvents(List<TabNote> notes, Grid grid, Map<Long, Stroke> strokes) {
            List<TabNote> sorted = new ArrayList<>(notes);
            sorted.sort(Comparator.comparingLong(note -> note.note().startTick()));
            List<Event> events = new ArrayList<>();
            for (TabNote note : sorted) {
                long start = grid.quantize(note.note().startTick());
                long end = grid.quantize(note.note().startTick() + note.note().durationTicks());
                if (end <= start) {
                    end = grid.nextPoint(start);
                }
                Stroke stroke = strokes.get(note.note().startTick());
                if (!events.isEmpty()) {
                    Event previous = events.get(events.size() - 1);
                    if (previous.start() == start) {
                        // 和音にまとめる（同じ弦の2音目は置けないので捨てる）。長さは最初の音（メロディ）に合わせる
                        if (previous.notes().stream().noneMatch(other -> other.string() == note.string())) {
                            List<TabNote> chord = new ArrayList<>(previous.notes());
                            chord.add(note);
                            events.set(events.size() - 1, new Event(previous.start(), previous.end(), chord,
                                    previous.stroke() != null ? previous.stroke() : stroke));
                        }
                        continue;
                    }
                    if (previous.end() > start) {
                        events.set(events.size() - 1,
                                new Event(previous.start(), start, previous.notes(), previous.stroke()));
                    }
                }
                events.add(new Event(start, end, List.of(note), stroke));
            }
            return events;
        }

        /**
         * from から to までを、書ける音符の長さの組み合わせに分けて追加する。
         * 3連符の区間は3連符の長さで、それ以外は普通の長さで分ける。
         */
        private void addBeats(List<Beat> beats, long from, long to, List<TabNote> notes, boolean tiedFromPrevious,
                              Stroke stroke, Grid grid) {
            boolean tied = tiedFromPrevious;
            long runStart = from;
            boolean runIsTriplet = false;
            long position = from;
            while (position < to) {
                Grid.Window window = grid.windowAt(position);
                long pieceEnd = Math.min(to, window.end());
                // 区間を丸ごと使うなら普通の4分音符でよいので、3連符にするのは区間の一部だけのとき
                boolean triplet = window.triplet() && !(position == window.start() && pieceEnd == window.end());
                if (position > runStart && triplet != runIsTriplet) {
                    tied = addRun(beats, position - runStart, runIsTriplet, notes, tied, stroke);
                    runStart = position;
                }
                runIsTriplet = triplet;
                position = pieceEnd;
            }
            if (to > runStart) {
                addRun(beats, to - runStart, runIsTriplet, notes, tied, stroke);
            }
        }

        /** 同じ種類（普通 / 3連符）の長さ length を音符に分けて追加する。追加したら true（次はタイ）を返す */
        private boolean addRun(List<Beat> beats, long length, boolean triplet, List<TabNote> notes, boolean tied,
                               Stroke stroke) {
            long[] lengths = triplet ? tripletLengths : straightLengths;
            Duration[] durations = triplet ? tripletDurations : straightDurations;
            long smallest = lengths[lengths.length - 1];
            long remaining = length;
            for (int i = 0; i < lengths.length && remaining >= smallest; ) {
                if (lengths[i] <= remaining) {
                    beats.add(new Beat(durations[i], notes, !notes.isEmpty() && tied, stroke));
                    remaining -= lengths[i];
                    tied = true;
                } else {
                    i++;
                }
            }
            return tied;
        }

        private record Event(long start, long end, List<TabNote> notes, Stroke stroke) {
        }

        /** 4分音符1つ分ずつの区間と、それぞれを3連符にするかどうか */
        private final class Grid {
            private final TreeMap<Long, Window> windows = new TreeMap<>();

            record Window(long start, long end, boolean triplet) {
            }

            Grid(List<TabNote> notes, List<Measure> measures) {
                for (Measure measure : measures) {
                    long measureEnd = measureEnd(measure);
                    for (long start = measureStart(measure); start < measureEnd; start += quarter) {
                        long end = Math.min(start + quarter, measureEnd);
                        windows.put(start, new Window(start, end, false));
                    }
                }
                // 区間ごとに、音の位置が16分音符と3連符のどちらに近いかを比べる
                Map<Long, long[]> errors = new HashMap<>();
                for (TabNote note : notes) {
                    long start = note.note().startTick();
                    for (long tick : new long[] {start, start + note.note().durationTicks()}) {
                        Map.Entry<Long, Window> entry = windows.floorEntry(tick);
                        if (entry == null || tick >= entry.getValue().end()) {
                            continue;
                        }
                        long offset = tick - entry.getKey();
                        long[] error = errors.computeIfAbsent(entry.getKey(), key -> new long[2]);
                        error[0] += distanceToGrid(offset, straightGrid);
                        error[1] += distanceToGrid(offset, tripletGrid);
                    }
                }
                errors.forEach((start, error) -> {
                    Window window = windows.get(start);
                    if (error[1] < error[0] && window.end() - window.start() == quarter) {
                        windows.put(start, new Window(window.start(), window.end(), true));
                    }
                });
            }

            long measureStart(Measure measure) {
                return Math.round((double) measure.startTick() / straightGrid) * straightGrid;
            }

            long measureEnd(Measure measure) {
                return Math.round((double) (measure.startTick() + measure.lengthTicks()) / straightGrid) * straightGrid;
            }

            Window windowAt(long tick) {
                Map.Entry<Long, Window> entry = windows.floorEntry(tick);
                if (entry == null || tick >= entry.getValue().end()) {
                    // 小節の外（最後の小節より後など）は普通の区間として扱う
                    long start = Math.floorDiv(tick, quarter) * quarter;
                    return new Window(start, start + quarter, false);
                }
                return entry.getValue();
            }

            /** 区間に合わせて、一番近いリズムの位置にそろえる */
            long quantize(long tick) {
                Window window = windowAt(tick);
                long grid = window.triplet() ? tripletGrid : straightGrid;
                long snapped = window.start() + Math.round((double) (tick - window.start()) / grid) * grid;
                return Math.min(snapped, window.end());
            }

            /** tick の次のリズムの位置 */
            long nextPoint(long tick) {
                Window window = windowAt(tick);
                long grid = window.triplet() ? tripletGrid : straightGrid;
                return Math.min(tick + grid, window.end());
            }

            private static long distanceToGrid(long offset, long grid) {
                long remainder = offset % grid;
                return Math.min(remainder, grid - remainder);
            }
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
