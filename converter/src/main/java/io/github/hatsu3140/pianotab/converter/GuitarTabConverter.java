package io.github.hatsu3140.pianotab.converter;

import io.github.hatsu3140.pianotab.converter.arrange.OctavePlacer;
import io.github.hatsu3140.pianotab.converter.arrange.VoiceExtractor;
import io.github.hatsu3140.pianotab.converter.fret.FretAssigner;
import io.github.hatsu3140.pianotab.converter.gp5.Gp5Writer;
import io.github.hatsu3140.pianotab.converter.model.Arrangement;
import io.github.hatsu3140.pianotab.converter.model.Score;
import io.github.hatsu3140.pianotab.converter.model.TabScore;
import io.github.hatsu3140.pianotab.converter.model.Tuning;
import io.github.hatsu3140.pianotab.converter.musicxml.MusicXmlFormatException;
import io.github.hatsu3140.pianotab.converter.musicxml.MusicXmlParser;

import java.io.IOException;
import java.io.InputStream;

/**
 * 変換の入口。各手順を順番に呼び出す。
 *
 * <pre>
 * MusicXML → [1. 読み込み] → [2. メロディ/ベース抽出] → [2.5 オクターブ調整]
 *          → [3. フレット割り当て] → [4. GP5 書き出し] → .gp5
 * </pre>
 */
public class GuitarTabConverter {

    private final MusicXmlParser parser = new MusicXmlParser();
    private final VoiceExtractor voiceExtractor = new VoiceExtractor();
    private final OctavePlacer octavePlacer = new OctavePlacer();
    private final FretAssigner fretAssigner;
    private final Gp5Writer gp5Writer = new Gp5Writer();

    public GuitarTabConverter() {
        this(Tuning.STANDARD);
    }

    public GuitarTabConverter(Tuning tuning) {
        this.fretAssigner = new FretAssigner(tuning);
    }

    public ConversionResult convert(InputStream musicXml) throws IOException, MusicXmlFormatException {
        Score score = parser.parse(musicXml);
        Arrangement arrangement = octavePlacer.place(voiceExtractor.extract(score));
        TabScore tabScore = fretAssigner.assign(arrangement);
        byte[] gp5 = gp5Writer.write(tabScore);
        return new ConversionResult(tabScore, gp5);
    }
}
