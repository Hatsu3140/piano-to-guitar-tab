package io.github.hatsu3140.pianotab.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ConvertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void servesUploadPage() throws Exception {
        mockMvc.perform(get("/index.html")).andExpect(status().isOk());
    }

    @Test
    void acceptsMusicXmlFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "song.musicxml", "application/xml",
                "<score-partwise/>".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/convert").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("song.musicxml"));
    }

    @Test
    void rejectsOtherFileTypes() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "song.pdf", "application/pdf",
                new byte[] {1, 2, 3});

        mockMvc.perform(multipart("/api/convert").file(file))
                .andExpect(status().isBadRequest());
    }
}
