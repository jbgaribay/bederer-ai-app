package com.jesusbarocio.bedererai.service;

import com.jesusbarocio.bedererai.dto.SwingAnalysisResponse;
import com.jesusbarocio.bedererai.entity.SwingAnalysis;
import com.jesusbarocio.bedererai.exception.InvalidUploadException;
import com.jesusbarocio.bedererai.exception.RateLimitExceededException;
import com.jesusbarocio.bedererai.repository.SwingAnalysisRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SwingAnalysisServiceTest {

    @Mock
    private FrameExtractionService frameExtractionService;

    @Mock
    private ClaudeVisionService claudeVisionService;

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private SwingAnalysisRepository repository;

    @InjectMocks
    private SwingAnalysisService swingAnalysisService;

    private AiAnalysisResult sampleAiResult() {
        AiCategoryResult category = new AiCategoryResult();
        category.setName("Stance & Preparation");
        category.setScore(8.0);
        category.setSeverity("good");
        category.setObservation("Solid base");
        category.setTip("Keep knees bent a touch more");

        AiAnalysisResult result = new AiAnalysisResult();
        result.setOverallScore(7.5);
        result.setShotType("forehand");
        result.setCategories(List.of(category));
        result.setTopPriority("Follow through more");
        result.setDrillRecommendation("Shadow swings, 3 sets of 10");
        return result;
    }

    @Test
    void analyze_rejectsNonVideoUploads() {
        MockMultipartFile file = new MockMultipartFile("video", "notes.txt", "text/plain", "hi".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> swingAnalysisService.analyze(file, "forehand"))
                .isInstanceOf(InvalidUploadException.class);

        verifyNoInteractions(frameExtractionService, claudeVisionService, rateLimiterService, repository);
    }

    @Test
    void analyze_respectsRateLimit() {
        MockMultipartFile file = new MockMultipartFile("video", "swing.mp4", "video/mp4", "fake-bytes".getBytes(StandardCharsets.UTF_8));
        doThrow(new RateLimitExceededException("limit reached")).when(rateLimiterService).checkAndIncrement();

        assertThatThrownBy(() -> swingAnalysisService.analyze(file, "forehand"))
                .isInstanceOf(RateLimitExceededException.class);

        verifyNoInteractions(frameExtractionService, claudeVisionService, repository);
    }

    @Test
    void analyze_happyPath_extractsAnalyzesAndPersists() {
        MockMultipartFile file = new MockMultipartFile("video", "swing.mp4", "video/mp4", "fake-bytes".getBytes(StandardCharsets.UTF_8));

        when(frameExtractionService.extractFrames(any(), anyInt())).thenReturn(List.of("base64frame"));
        when(claudeVisionService.analyzeSwing(any(), eq("forehand"))).thenReturn(sampleAiResult());
        when(repository.save(any(SwingAnalysis.class))).thenAnswer(invocation -> {
            SwingAnalysis saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        SwingAnalysisResponse response = swingAnalysisService.analyze(file, "forehand");

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getShotType()).isEqualTo("forehand");
        assertThat(response.getCategories()).hasSize(1);
        assertThat(response.getCategories().get(0).getName()).isEqualTo("Stance & Preparation");

        verify(rateLimiterService).checkAndIncrement();
        verify(frameExtractionService).extractFrames(any(), eq(6));
    }
}
