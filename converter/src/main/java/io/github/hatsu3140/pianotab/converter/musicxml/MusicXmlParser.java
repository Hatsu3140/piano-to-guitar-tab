package io.github.hatsu3140.pianotab.converter.musicxml;

import io.github.hatsu3140.pianotab.converter.model.Measure;
import io.github.hatsu3140.pianotab.converter.model.Note;
import io.github.hatsu3140.pianotab.converter.model.Score;
import io.github.hatsu3140.pianotab.converter.model.SongInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 手順1: MusicXML ファイルを読み込んで {@link Score} にする。
 *
 * <p>対応しているのは partwise 形式（MuseScore などが書き出す一般的な形式）。
 * 圧縮された .mxl ファイルも読める。装飾音（grace）は今は読み飛ばす。
 */
public class MusicXmlParser {

    /** 4分音符1つあたりの tick 数。曲ごとの divisions をこの値にそろえる */
    public static final int TICKS_PER_QUARTER = 480;

    private static final int DEFAULT_TEMPO = 120;

    /** .mxl を展開するときの上限（壊れたファイルや悪意あるファイル対策） */
    private static final long MAX_UNCOMPRESSED_BYTES = 50L * 1024 * 1024;

    /** C, D, E, F, G, A, B の C からの半音数 */
    private static final Map<String, Integer> STEP_SEMITONES = Map.of(
            "C", 0, "D", 2, "E", 4, "F", 5, "G", 7, "A", 9, "B", 11);

    public Score parse(InputStream musicXml) throws IOException, MusicXmlFormatException {
        byte[] data = musicXml.readAllBytes();
        if (isZip(data)) {
            data = extractFromMxl(data);
        }
        Element root = parseXml(data).getDocumentElement();
        if ("score-timewise".equals(root.getTagName())) {
            throw new MusicXmlFormatException("timewise 形式の MusicXML にはまだ対応していません。");
        }
        if (!"score-partwise".equals(root.getTagName())) {
            throw new MusicXmlFormatException("MusicXML ファイルではないようです。");
        }

        List<Element> parts = children(root, "part");
        if (parts.isEmpty()) {
            throw new MusicXmlFormatException("楽譜のパートが見つかりません。");
        }

        // 小節の位置は最初のパートで決め、2つ目以降のパートもそれに合わせる
        PartReader firstPart = new PartReader(null);
        firstPart.read(parts.get(0));
        if (firstPart.measures.isEmpty()) {
            throw new MusicXmlFormatException("小節が見つかりません。");
        }
        List<Note> notes = new ArrayList<>(firstPart.notes);
        for (Element part : parts.subList(1, parts.size())) {
            PartReader reader = new PartReader(firstPart.measures);
            reader.read(part);
            notes.addAll(reader.notes);
        }

        SongInfo info = new SongInfo(readTitle(root), readTempo(root), TICKS_PER_QUARTER, firstPart.measures);
        return new Score(info, notes);
    }

    /** 1つのパート（ピアノなら右手と左手の両方を含む）を読む */
    private static final class PartReader {
        /** 2つ目以降のパートで使う、最初のパートの小節位置 */
        private final List<Measure> referenceMeasures;
        private final List<Note> notes = new ArrayList<>();
        private final List<Measure> measures = new ArrayList<>();
        /** タイでつながっている途中の音（音の高さ → notes の位置） */
        private final Map<Integer, Integer> openTies = new HashMap<>();
        private double divisions = 1;
        private int beats = 4;
        private int beatType = 4;

        PartReader(List<Measure> referenceMeasures) {
            this.referenceMeasures = referenceMeasures;
        }

        void read(Element part) throws MusicXmlFormatException {
            long nextMeasureStart = 0;
            List<Element> measureElements = children(part, "measure");
            for (int index = 0; index < measureElements.size(); index++) {
                Element measure = measureElements.get(index);
                boolean hasReference = referenceMeasures != null && index < referenceMeasures.size();
                long measureStart = hasReference ? referenceMeasures.get(index).startTick() : nextMeasureStart;
                long cursor = measureStart;
                long chordStart = measureStart;
                long maxPosition = measureStart;

                for (Element element : children(measure)) {
                    switch (element.getTagName()) {
                        case "attributes" -> readAttributes(element);
                        case "backup" -> cursor -= toTicks(element);
                        case "forward" -> cursor += toTicks(element);
                        case "note" -> {
                            if (child(element, "grace") != null || child(element, "cue") != null) {
                                continue;
                            }
                            long duration = toTicks(element);
                            boolean isChord = child(element, "chord") != null;
                            if (!isChord) {
                                chordStart = cursor;
                                cursor += duration;
                            }
                            readNote(element, chordStart, duration);
                        }
                        default -> {
                        }
                    }
                    maxPosition = Math.max(maxPosition, cursor);
                }

                long length;
                if (hasReference) {
                    length = referenceMeasures.get(index).lengthTicks();
                } else {
                    long contentLength = maxPosition - measureStart;
                    long timeSignatureLength = (long) beats * TICKS_PER_QUARTER * 4 / beatType;
                    boolean isPickup = "yes".equals(measure.getAttribute("implicit"));
                    length = isPickup && contentLength > 0
                            ? contentLength
                            : Math.max(contentLength, timeSignatureLength);
                    measures.add(new Measure(measureStart, length, beats, beatType));
                }
                nextMeasureStart = measureStart + length;
            }
        }

        private void readAttributes(Element attributes) throws MusicXmlFormatException {
            String divisionsText = childText(attributes, "divisions");
            if (divisionsText != null) {
                divisions = parseNumber(divisionsText);
                if (divisions <= 0) {
                    throw new MusicXmlFormatException("divisions の値が正しくありません: " + divisionsText);
                }
            }
            Element time = child(attributes, "time");
            if (time != null && childText(time, "beats") != null && childText(time, "beat-type") != null) {
                // "3+2" のような複合拍子は合計する
                int sum = 0;
                for (String part : childText(time, "beats").split("\\+")) {
                    sum += (int) parseNumber(part.trim());
                }
                beats = sum;
                beatType = (int) parseNumber(childText(time, "beat-type"));
            }
        }

        private void readNote(Element note, long start, long duration) throws MusicXmlFormatException {
            Element pitchElement = child(note, "pitch");
            if (pitchElement == null || duration <= 0) {
                return; // 休符や打楽器の音
            }
            int pitch = toMidiPitch(pitchElement);

            boolean tieStart = false;
            boolean tieStop = false;
            for (Element tie : children(note, "tie")) {
                tieStart |= "start".equals(tie.getAttribute("type"));
                tieStop |= "stop".equals(tie.getAttribute("type"));
            }

            Integer tiedIndex = openTies.get(pitch);
            if (tieStop && tiedIndex != null) {
                // タイの続きは新しい音にせず、前の音を伸ばす
                Note previous = notes.get(tiedIndex);
                notes.set(tiedIndex, new Note(pitch, previous.startTick(), start + duration - previous.startTick()));
                if (!tieStart) {
                    openTies.remove(pitch);
                }
                return;
            }
            notes.add(new Note(pitch, start, duration));
            if (tieStart) {
                openTies.put(pitch, notes.size() - 1);
            } else {
                openTies.remove(pitch);
            }
        }

        private long toTicks(Element element) throws MusicXmlFormatException {
            String text = childText(element, "duration");
            if (text == null) {
                return 0;
            }
            return Math.round(parseNumber(text) * TICKS_PER_QUARTER / divisions);
        }
    }

    private static int toMidiPitch(Element pitch) throws MusicXmlFormatException {
        String step = childText(pitch, "step");
        String octave = childText(pitch, "octave");
        String alter = childText(pitch, "alter");
        if (step == null || octave == null || !STEP_SEMITONES.containsKey(step)) {
            throw new MusicXmlFormatException("音の高さ（pitch）が正しくありません。");
        }
        int semitones = STEP_SEMITONES.get(step) + (alter == null ? 0 : (int) Math.round(parseNumber(alter)));
        // MIDI ではオクターブ4のド（C4）が 60
        return ((int) parseNumber(octave) + 1) * 12 + semitones;
    }

    private static String readTitle(Element root) {
        Element work = child(root, "work");
        String title = work == null ? null : childText(work, "work-title");
        if (title == null || title.isEmpty()) {
            title = childText(root, "movement-title");
        }
        return title == null ? "" : title;
    }

    private static int readTempo(Element root) {
        NodeList sounds = root.getElementsByTagName("sound");
        for (int i = 0; i < sounds.getLength(); i++) {
            String tempo = ((Element) sounds.item(i)).getAttribute("tempo");
            if (!tempo.isEmpty()) {
                try {
                    long value = Math.round(Double.parseDouble(tempo));
                    if (value > 0) {
                        return (int) value;
                    }
                } catch (NumberFormatException ignored) {
                    // 読めないテンポは無視して次を探す
                }
            }
        }
        return DEFAULT_TEMPO;
    }

    private static Document parseXml(byte[] data) throws IOException, MusicXmlFormatException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // 外部ファイルやネットワークを読みに行かないようにする（MusicXML の DOCTYPE 宣言対策）
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException exception) {
                }

                @Override
                public void error(SAXParseException exception) throws SAXException {
                    throw exception;
                }

                @Override
                public void fatalError(SAXParseException exception) throws SAXException {
                    throw exception;
                }
            });
            return builder.parse(new ByteArrayInputStream(data));
        } catch (SAXException e) {
            throw new MusicXmlFormatException("XML として読み込めませんでした。", e);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean isZip(byte[] data) {
        return data.length >= 2 && data[0] == 'P' && data[1] == 'K';
    }

    /** .mxl（zip 圧縮された MusicXML）から楽譜本体を取り出す */
    private static byte[] extractFromMxl(byte[] data) throws IOException, MusicXmlFormatException {
        Map<String, byte[]> entries = new HashMap<>();
        long total = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(data))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = zip.read(buffer)) != -1) {
                    total += read;
                    if (total > MAX_UNCOMPRESSED_BYTES) {
                        throw new MusicXmlFormatException(".mxl ファイルの中身が大きすぎます。");
                    }
                    out.write(buffer, 0, read);
                }
                entries.put(entry.getName(), out.toByteArray());
            }
        }

        byte[] container = entries.get("META-INF/container.xml");
        if (container != null) {
            NodeList rootFiles = parseXml(container).getElementsByTagName("rootfile");
            if (rootFiles.getLength() > 0) {
                byte[] score = entries.get(((Element) rootFiles.item(0)).getAttribute("full-path"));
                if (score != null) {
                    return score;
                }
            }
        }
        for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
            String name = entry.getKey();
            if (!name.startsWith("META-INF/") && (name.endsWith(".xml") || name.endsWith(".musicxml"))) {
                return entry.getValue();
            }
        }
        throw new MusicXmlFormatException(".mxl ファイルの中に楽譜が見つかりません。");
    }

    private static double parseNumber(String text) throws MusicXmlFormatException {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            throw new MusicXmlFormatException("数値が正しくありません: " + text, e);
        }
    }

    private static List<Element> children(Element parent) {
        List<Element> result = new ArrayList<>();
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element) {
                result.add(element);
            }
        }
        return result;
    }

    private static List<Element> children(Element parent, String name) {
        return children(parent).stream().filter(e -> e.getTagName().equals(name)).toList();
    }

    private static Element child(Element parent, String name) {
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && element.getTagName().equals(name)) {
                return element;
            }
        }
        return null;
    }

    private static String childText(Element parent, String name) {
        Element element = child(parent, name);
        return element == null ? null : element.getTextContent().trim();
    }
}
