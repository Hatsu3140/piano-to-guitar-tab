package io.github.hatsu3140.pianotab.web;

import io.github.hatsu3140.pianotab.converter.ConversionResult;
import io.github.hatsu3140.pianotab.converter.GuitarTabConverter;
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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * 画面からアップロードされた MusicXML を受け取り、.gp5 ファイルにして返す API。
 */
@RestController
public class ConvertController {

    private static final List<String> EXTENSIONS = List.of(".musicxml", ".xml", ".mxl");

    private final GuitarTabConverter converter;

    public ConvertController(GuitarTabConverter converter) {
        this.converter = converter;
    }

    @PostMapping("/api/convert")
    public ResponseEntity<?> convert(@RequestParam("file") MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        if (file.isEmpty()) {
            return badRequest("ファイルが空です。");
        }
        String extension = extensionOf(fileName);
        if (extension == null) {
            return badRequest("MusicXML ファイル（.musicxml / .xml / .mxl）を選んでください。");
        }

        ConversionResult result;
        try (InputStream in = file.getInputStream()) {
            result = converter.convert(in);
        } catch (MusicXmlFormatException e) {
            return badRequest("変換できませんでした: " + e.getMessage());
        }

        // 例: "song.musicxml" → "song.gp5"
        String gp5Name = fileName.substring(0, fileName.length() - extension.length()) + ".gp5";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(gp5Name, StandardCharsets.UTF_8).build().toString())
                .body(result.gp5());
    }

    private static ResponseEntity<ConvertResponse> badRequest(String message) {
        return ResponseEntity.badRequest().body(new ConvertResponse(message));
    }

    private static String extensionOf(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        return EXTENSIONS.stream().filter(lower::endsWith).findFirst().orElse(null);
    }
}
