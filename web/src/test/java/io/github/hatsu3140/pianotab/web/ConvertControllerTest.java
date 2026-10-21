package io.github.hatsu3140.pianotab.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ConvertControllerTest {

    private static final String MUSIC_XML = """
            <score-partwise><part id="P1"><measure number="1">
              <attributes><divisions>1</divisions></attributes>
              <note><pitch><step>E</step><octave>4</octave></pitch><duration>4</duration></note>
            </measure></part></score-partwise>
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void servesUploadPage() throws Exception {
        mockMvc.perform(get("/index.html")).andExpect(status().isOk());
    }

    @Test
    void returnsGp5File() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "song.musicxml", "application/xml",
                MUSIC_XML.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/convert").file(file))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("song.gp5")));
    }

    @Test
    void rejectsBrokenMusicXml() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "song.musicxml", "application/xml",
                "<score-partwise>".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/convert").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void rejectsOtherFileTypes() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "song.pdf", "application/pdf",
                new byte[] {1, 2, 3});

        mockMvc.perform(multipart("/api/convert").file(file))
                .andExpect(status().isBadRequest());
    }
}
