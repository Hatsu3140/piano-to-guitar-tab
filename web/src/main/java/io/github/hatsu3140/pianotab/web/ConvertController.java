package io.github.hatsu3140.pianotab.web;

import io.github.hatsu3140.pianotab.converter.ConversionOptions;
import io.github.hatsu3140.pianotab.converter.ConversionResult;
import io.github.hatsu3140.pianotab.converter.Difficulty;
import io.github.hatsu3140.pianotab.converter.GuitarTabConverter;
import io.github.hatsu3140.pianotab.converter.SetupSuggester;
import io.github.hatsu3140.pianotab.converter.SetupSuggestion;
import io.github.hatsu3140.pianotab.converter.model.Tuning;
import io.github.hatsu3140.pianotab.converter.musicxml.MusicXmlFormatException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * 画面からアップロードされた MusicXML と設定を受け取り、.gp5 ファイルにして返す API。
 * 変換レポートの説明文は、ヘッダー X-Conversion-Report に（1行ずつ改行で区切り、URL エンコードして）入れる。
 *
 * <p>/api/suggest は、同じ曲に合うチューニングとカポの組み合わせを、良い順に JSON で返す。
 */
@RestController
public class ConvertController {

    private static final List<String> EXTENSIONS = List.of(".musicxml", ".xml", ".mxl");
    static final String REPORT_HEADER = "X-Conversion-Report";

    private final GuitarTabConverter converter;
    private final SetupSuggester suggester;

    public ConvertController(GuitarTabConverter converter) {
        this.converter = converter;
        this.suggester = new SetupSuggester(converter);
    }

    @PostMapping("/api/convert")
    public ResponseEntity<?> convert(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "difficulty", defaultValue = "INTERMEDIATE") String difficulty,
            @RequestParam(name = "tuning", defaultValue = "STANDARD") String tuning,
            @RequestParam(name = "capo", defaultValue = "0") int capo,
            @RequestParam(name = "transpose", defaultValue = "0") int transpose,
            @RequestParam(name = "barre", defaultValue = "true") boolean barre,
            @RequestParam(name = "strokes", defaultValue = "true") boolean strokes) throws IOException {
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        if (file.isEmpty()) {
            return badRequest("ファイルが空です。");
        }
        String extension = extensionOf(fileName);
        if (extension == null) {
            return badRequest("MusicXML ファイル（.musicxml / .xml / .mxl）を選んでください。");
        }

        ConversionOptions options;
        try {
            options = new ConversionOptions(parseDifficulty(difficulty), parseTuning(tuning), capo, transpose,
                    barre, strokes);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }

        ConversionResult result;
        try (InputStream in = file.getInputStream()) {
            result = converter.convert(in, options);
        } catch (MusicXmlFormatException e) {
            return badRequest("変換できませんでした: " + e.getMessage());
        }

        // 例: "song.musicxml" → "song.gp5"
        String gp5Name = fileName.substring(0, fileName.length() - extension.length()) + ".gp5";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(gp5Name, StandardCharsets.UTF_8).build().toString())
                .header(REPORT_HEADER, URLEncoder.encode(String.join("\n", result.report().messages()),
                        StandardCharsets.UTF_8).replace("+", "%20"))
                .body(result.gp5());
    }

    @PostMapping("/api/suggest")
    public ResponseEntity<?> suggest(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "difficulty", defaultValue = "INTERMEDIATE") String difficulty,
            @RequestParam(name = "transpose", defaultValue = "0") int transpose,
            @RequestParam(name = "barre", defaultValue = "true") boolean barre,
            @RequestParam(name = "strokes", defaultValue = "true") boolean strokes) throws IOException {
        if (file.isEmpty()) {
            return badRequest("ファイルが空です。");
        }
        if (extensionOf(file.getOriginalFilename() == null ? "" : file.getOriginalFilename()) == null) {
            return badRequest("MusicXML ファイル（.musicxml / .xml / .mxl）を選んでください。");
        }

        ConversionOptions base;
        try {
            base = new ConversionOptions(parseDifficulty(difficulty), Tuning.STANDARD, 0, transpose, barre, strokes);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }

        List<SetupSuggestion> suggestions;
        try (InputStream in = file.getInputStream()) {
            suggestions = suggester.suggest(in, base, SetupSuggester.DEFAULT_LIMIT);
        } catch (MusicXmlFormatException e) {
            return badRequest("読み込めませんでした: " + e.getMessage());
        }
        return ResponseEntity.ok(new SuggestResponse(suggestions.stream()
                .map(suggestion -> new SuggestResponse.Item(suggestion.tuning(), suggestion.capo(),
                        suggestion.reasons()))
                .toList()));
    }

    private static Difficulty parseDifficulty(String value) {
        try {
            return Difficulty.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("難易度の指定が正しくありません: " + value);
        }
    }

    private static Tuning parseTuning(String value) {
        Tuning tuning = Tuning.PRESETS.get(value);
        if (tuning == null) {
            throw new IllegalArgumentException("チューニングの指定が正しくありません: " + value);
        }
        return tuning;
    }

    private static ResponseEntity<ConvertResponse> badRequest(String message) {
        return ResponseEntity.badRequest().body(new ConvertResponse(message));
    }

    private static String extensionOf(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        return EXTENSIONS.stream().filter(lower::endsWith).findFirst().orElse(null);
    }
}
