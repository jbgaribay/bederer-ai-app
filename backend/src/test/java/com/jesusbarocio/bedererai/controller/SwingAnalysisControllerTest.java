package com.jesusbarocio.bedererai.controller;

import com.jesusbarocio.bedererai.dto.SwingAnalysisResponse;
import com.jesusbarocio.bedererai.entity.Severity;
import com.jesusbarocio.bedererai.dto.SwingCategoryDto;
import com.jesusbarocio.bedererai.service.SwingAnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SwingAnalysisController.class)
class SwingAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SwingAnalysisService swingAnalysisService;

    private SwingAnalysisResponse sampleResponse() {
        SwingCategoryDto category = new SwingCategoryDto(
                "Stance & Preparation", 8.0, Severity.GOOD, "Solid base", "Bend knees more", 0);

        return new SwingAnalysisResponse(
                1L, "forehand", 7.5, "Follow through more", "Shadow swings",
                Instant.parse("2026-07-29T12:00:00Z"), List.of(category));
    }

    @Test
    void getHistory_isPubliclyAccessible() throws Exception {
        when(swingAnalysisService.getHistory()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/swings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].shotType").value("forehand"))
                .andExpect(jsonPath("$[0].categories[0].name").value("Stance & Preparation"));
    }

    @Test
    void analyze_withoutAuth_isRejected() throws Exception {
        MockMultipartFile video = new MockMultipartFile("video", "swing.mp4", "video/mp4", "fake".getBytes());

        mockMvc.perform(multipart("/api/swings").file(video).param("shotType", "forehand"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "demo", roles = {"DEMO"})
    void analyze_withAuth_returnsCreatedAnalysis() throws Exception {
        MockMultipartFile video = new MockMultipartFile("video", "swing.mp4", "video/mp4", "fake".getBytes());

        when(swingAnalysisService.analyze(any(), anyString())).thenReturn(sampleResponse());

        mockMvc.perform(multipart("/api/swings").file(video).param("shotType", "forehand"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.overallScore").value(7.5));
    }

    @Test
    @WithMockUser(username = "demo", roles = {"DEMO"})
    void delete_withAuth_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/swings/1"))
                .andExpect(status().isNoContent());
    }
}
