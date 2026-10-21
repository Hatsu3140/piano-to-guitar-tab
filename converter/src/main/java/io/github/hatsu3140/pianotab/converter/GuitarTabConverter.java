package io.github.hatsu3140.pianotab.converter;

import io.github.hatsu3140.pianotab.converter.arrange.BassSimplifier;
import io.github.hatsu3140.pianotab.converter.arrange.OctavePlacer;
import io.github.hatsu3140.pianotab.converter.arrange.VoiceExtractor;
import io.github.hatsu3140.pianotab.converter.fret.FretAssigner;
import io.github.hatsu3140.pianotab.converter.gp5.Gp5Writer;
import io.github.hatsu3140.pianotab.converter.model.Arrangement;
import io.github.hatsu3140.pianotab.converter.model.Note;
import io.github.hatsu3140.pianotab.converter.model.Score;
import io.github.hatsu3140.pianotab.converter.model.TabScore;
import io.github.hatsu3140.pianotab.converter.musicxml.MusicXmlFormatException;
import io.github.hatsu3140.pianotab.converter.musicxml.MusicXmlParser;

import java.io.IOException;
import java.io.InputStream;

/**
 * 変換の入口。各手順を順番に呼び出す。
 *
 * <pre>
 * MusicXML → [1. 読み込み] → [移調] → [2. メロディ/ベース抽出] → [難易度に合わせてベースを減らす]
 *          → [2.5 オクターブ調整] → [3. フレット割り当て] → [4. GP5 書き出し] → .gp5
 * </pre>
 */
public class GuitarTabConverter {

    private final MusicXmlParser parser = new MusicXmlParser();
    private final VoiceExtractor voiceExtractor = new VoiceExtractor();
    private final Gp5Writer gp5Writer = new Gp5Writer();

    /** 標準の設定（{@link ConversionOptions#DEFAULT}）で変換する */
    public ConversionResult convert(InputStream musicXml) throws IOException, MusicXmlFormatException {
        return convert(musicXml, ConversionOptions.DEFAULT);
    }

    public ConversionResult convert(InputStream musicXml, ConversionOptions options)
            throws IOException, MusicXmlFormatException {
        Difficulty difficulty = options.difficulty();
        Score score = transpose(parser.parse(musicXml), options.transpose());
        Arrangement arrangement = voiceExtractor.extract(score);
        arrangement = new BassSimplifier(difficulty.bassRhythm()).simplify(arrangement);
        arrangement = new OctavePlacer(options.tuning(), options.capo(), difficulty.maxFret()).place(arrangement);
        TabScore tabScore = new FretAssigner(options.tuning(), options.capo(), difficulty).assign(arrangement);
        byte[] gp5 = gp5Writer.write(tabScore);
        return new ConversionResult(tabScore, gp5);
    }

    private static Score transpose(Score score, int semitones) {
        if (semitones == 0) {
            return score;
        }
        return new Score(score.info(), score.notes().stream()
                .map(note -> new Note(note.pitch() + semitones, note.startTick(), note.durationTicks()))
                .toList());
    }
}
