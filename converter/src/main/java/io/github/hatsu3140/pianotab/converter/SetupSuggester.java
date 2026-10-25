package io.github.hatsu3140.pianotab.converter;

import io.github.hatsu3140.pianotab.converter.model.Score;
import io.github.hatsu3140.pianotab.converter.model.TabNote;
import io.github.hatsu3140.pianotab.converter.model.Tuning;
import io.github.hatsu3140.pianotab.converter.musicxml.MusicXmlFormatException;
import io.github.hatsu3140.pianotab.converter.musicxml.MusicXmlParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 曲に合うチューニングとカポの組み合わせを提案する。
 *
 * <p>選べるチューニング（{@link Tuning#PRESETS}）とカポ0〜7のすべての組み合わせで実際に変換してみて、
 * 次の点数が高い順に並べる。
 * <ul>
 *   <li>ベースを開放弦で弾ける割合（一番重い。開放弦のベースなら、そのあいだ左手の指がほかの音に使える）</li>
 *   <li>メロディを省かずに残せた割合</li>
 *   <li>ベースを省かずに残せた割合</li>
 *   <li>途中で短くした音の少なさ</li>
 *   <li>チューニングを変える手間（レギュラーが一番楽、6弦だけ変えるドロップDが次）</li>
 *   <li>カポの高さ（6フレットより上は、使えるネックが短くなり音も細くなるので少し減点）</li>
 * </ul>
 * 難易度・移調・セーハ・ストロークは、利用者が選んだものをそのまま使う。
 */
public class SetupSuggester {

    /** 画面に出す候補の数 */
    public static final int DEFAULT_LIMIT = 3;

    private static final double OPEN_BASS_WEIGHT = 50;
    private static final double MELODY_KEPT_WEIGHT = 30;
    private static final double BASS_KEPT_WEIGHT = 30;
    private static final double TRIMMED_WEIGHT = 20;
    /** チューニングを変える手間（点数から引く） */
    private static final Map<Tuning, Double> RETUNE_COST = Map.of(Tuning.DROP_D, 1.0);
    private static final double OTHER_TUNING_COST = 3.0;
    /** カポがこのフレットより上なら減点する */
    private static final int HIGH_CAPO_FROM = 5;
    private static final double HIGH_CAPO_COST = 1.0;
    /** ベースに使う弦の数（太い方から） */
    private static final int BASS_STRINGS = 3;

    private final GuitarTabConverter converter;

    public SetupSuggester() {
        this(new GuitarTabConverter());
    }

    public SetupSuggester(GuitarTabConverter converter) {
        this.converter = converter;
    }

    /** MusicXML を読み込んで提案する */
    public List<SetupSuggestion> suggest(InputStream musicXml, ConversionOptions base, int limit)
            throws IOException, MusicXmlFormatException {
        return suggest(new MusicXmlParser().parse(musicXml), base, limit);
    }

    /**
     * @param base チューニングとカポ以外の設定（難易度・移調など）。チューニングとカポは無視する
     * @return 点数の高い順に limit 個
     */
    public List<SetupSuggestion> suggest(Score score, ConversionOptions base, int limit) {
        List<ConversionOptions> candidates = new ArrayList<>();
        for (Tuning tuning : Tuning.PRESETS.values()) {
            for (int capo = 0; capo <= ConversionOptions.MAX_CAPO; capo++) {
                candidates.add(new ConversionOptions(base.difficulty(), tuning, capo, base.transpose(),
                        base.barre(), base.strokes()));
            }
        }
        // 48通りを順に変換すると数秒かかるので、CPU のコアを使って同時に進める
        return candidates.parallelStream()
                .map(options -> evaluate(score, options))
                .sorted(Comparator.comparingDouble(SetupSuggestion::score).reversed()
                        .thenComparingInt(suggestion -> order(suggestion)))
                .limit(limit)
                .toList();
    }

    SetupSuggestion evaluate(Score score, ConversionOptions options) {
        ConversionResult result = converter.convert(score, options);
        ConversionReport report = result.report();
        List<TabNote> bass = result.tabScore().bass();
        int capo = options.capo();
        // 実際に開放弦で弾いているかではなく、低い3本の弦の開放弦と同じ音名か（オクターブや押さえ方はあとで変えられる）で数える
        Tuning tuning = options.tuning();
        long openBass = bass.stream().filter(note -> isOpenBassString(note.note().pitch(), tuning, capo)).count();

        double openBassRatio = ratio(openBass, bass.size());
        double melodyKept = 1 - ratio(report.droppedMelodyNotes(), report.melodyNotes());
        double bassKept = 1 - ratio(report.droppedBassNotes(), report.bassNotes());
        int keptNotes = result.tabScore().melody().size() + bass.size();
        double trimmed = ratio(report.trimmedNotes(), keptNotes);
        double total = openBassRatio * OPEN_BASS_WEIGHT + melodyKept * MELODY_KEPT_WEIGHT
                + bassKept * BASS_KEPT_WEIGHT - trimmed * TRIMMED_WEIGHT - retuneCost(options.tuning())
                - Math.max(0, capo - HIGH_CAPO_FROM) * HIGH_CAPO_COST;
        return new SetupSuggestion(keyOf(options.tuning()), capo, total,
                (int) Math.round(openBassRatio * 100), report.droppedMelodyNotes(), report.droppedBassNotes(),
                report.trimmedNotes());
    }

    /** ベースに使う低い3本の弦のどれかの開放弦と同じ音名か */
    static boolean isOpenBassString(int pitch, Tuning tuning, int capo) {
        for (int string = tuning.stringCount() - BASS_STRINGS + 1; string <= tuning.stringCount(); string++) {
            if (Math.floorMod(tuning.openPitch(string) + capo - pitch, 12) == 0) {
                return true;
            }
        }
        return false;
    }

    private static double retuneCost(Tuning tuning) {
        if (tuning.equals(Tuning.STANDARD)) {
            return 0;
        }
        return RETUNE_COST.getOrDefault(tuning, OTHER_TUNING_COST);
    }

    private static double ratio(long part, long whole) {
        return whole == 0 ? 0 : (double) part / whole;
    }

    private static String keyOf(Tuning tuning) {
        return Tuning.PRESETS.entrySet().stream()
                .filter(entry -> entry.getValue().equals(tuning))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow();
    }

    /** 点数が同じときは、一覧で先にあるチューニング・低いカポを先にする（結果が毎回同じになるように） */
    private static int order(SetupSuggestion suggestion) {
        int tuningIndex = new ArrayList<>(Tuning.PRESETS.keySet()).indexOf(suggestion.tuning());
        return tuningIndex * (ConversionOptions.MAX_CAPO + 1) + suggestion.capo();
    }
}
