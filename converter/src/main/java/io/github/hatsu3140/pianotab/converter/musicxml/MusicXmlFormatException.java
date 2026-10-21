package io.github.hatsu3140.pianotab.converter.musicxml;

/**
 * MusicXML として読めないファイルだったときの例外。
 */
public class MusicXmlFormatException extends Exception {

    public MusicXmlFormatException(String message) {
        super(message);
    }

    public MusicXmlFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
