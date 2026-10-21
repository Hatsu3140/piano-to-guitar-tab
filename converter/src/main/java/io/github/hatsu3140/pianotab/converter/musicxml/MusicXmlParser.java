package io.github.hatsu3140.pianotab.converter.musicxml;

import io.github.hatsu3140.pianotab.converter.model.Score;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 手順1: MusicXML ファイルを読み込んで {@link Score} にする。
 */
public class MusicXmlParser {

    /** 何も指定がないときに使う 4分音符あたりの tick 数 */
    public static final int DEFAULT_TICKS_PER_QUARTER = 480;

    public Score parse(InputStream musicXml) throws IOException {
        musicXml.readAllBytes();
        // TODO: 次の PR で実装する。<part> / <measure> / <note> を読み、
        //       <pitch>（step, alter, octave）を MIDI 番号に、<duration> を tick に変換する。
        return new Score(DEFAULT_TICKS_PER_QUARTER, List.of());
    }
}
