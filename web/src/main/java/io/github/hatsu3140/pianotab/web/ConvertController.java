package io.github.hatsu3140.pianotab.web;

import io.github.hatsu3140.pianotab.converter.ConversionResult;
import io.github.hatsu3140.pianotab.converter.GuitarTabConverter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * 画面からアップロードされた MusicXML を受け取り、変換する API。
 */
@RestController
public class ConvertController {

    private final GuitarTabConverter converter;

    public ConvertController(GuitarTabConverter converter) {
        this.converter = converter;
    }

    @PostMapping("/api/convert")
    public ResponseEntity<ConvertResponse> convert(@RequestParam("file") MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ConvertResponse(fileName, 0, "ファイルが空です。"));
        }
        if (!isMusicXml(fileName)) {
            return ResponseEntity.badRequest()
                    .body(new ConvertResponse(fileName, 0, "MusicXML ファイル（.musicxml または .xml）を選んでください。"));
        }

        ConversionResult result;
        try (InputStream in = file.getInputStream()) {
            result = converter.convert(in);
        }
        // TODO: 変換が完成したら、JSON ではなく .gp5 ファイルをダウンロードさせる
        return ResponseEntity.ok(new ConvertResponse(fileName, result.tabScore().notes().size(),
                "ファイルを受け取りました（変換機能は準備中です）。"));
    }

    private static boolean isMusicXml(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".musicxml") || lower.endsWith(".xml");
    }
}
