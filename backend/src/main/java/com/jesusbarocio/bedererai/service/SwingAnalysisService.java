package com.jesusbarocio.bedererai.service;

import com.jesusbarocio.bedererai.dto.SwingAnalysisResponse;
import com.jesusbarocio.bedererai.dto.SwingCategoryDto;
import com.jesusbarocio.bedererai.entity.Severity;
import com.jesusbarocio.bedererai.entity.SwingAnalysis;
import com.jesusbarocio.bedererai.entity.SwingCategory;
import com.jesusbarocio.bedererai.exception.AnalysisFailedException;
import com.jesusbarocio.bedererai.exception.InvalidUploadException;
import com.jesusbarocio.bedererai.exception.ResourceNotFoundException;
import com.jesusbarocio.bedererai.repository.SwingAnalysisRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Orchestrates the same flow as the original app/api/analyze/route.ts:
 * save the upload -> extract frames -> call Claude -> persist -> return DTO,
 * then clean up the temp video file either way.
 */
@Service
public class SwingAnalysisService {

    private static final int FRAME_COUNT = 6;
    private static final int HISTORY_LIMIT = 10;

    private final FrameExtractionService frameExtractionService;
    private final ClaudeVisionService claudeVisionService;
    private final RateLimiterService rateLimiterService;
    private final SwingAnalysisRepository repository;
    private final long maxUploadBytes;

    public SwingAnalysisService(
            FrameExtractionService frameExtractionService,
            ClaudeVisionService claudeVisionService,
            RateLimiterService rateLimiterService,
            SwingAnalysisRepository repository,
            @Value("${app.upload.max-file-size-mb:500}") long maxUploadSizeMb) {
        this.frameExtractionService = frameExtractionService;
        this.claudeVisionService = claudeVisionService;
        this.rateLimiterService = rateLimiterService;
        this.repository = repository;
        this.maxUploadBytes = maxUploadSizeMb * 1024 * 1024;
    }

    public SwingAnalysisResponse analyze(MultipartFile video, String shotType) {
        validateUpload(video);
        rateLimiterService.checkAndIncrement();

        Path tempVideo = null;
        try {
            tempVideo = Files.createTempFile("bederer-upload-", ".mp4");
            video.transferTo(tempVideo);

            List<String> frames = frameExtractionService.extractFrames(tempVideo, FRAME_COUNT);
            AiAnalysisResult aiResult = claudeVisionService.analyzeSwing(frames, shotType);

            SwingAnalysis saved = repository.save(toEntity(aiResult));
            SwingAnalysisResponse response = toResponse(saved);
            // Frames are only ever attached to this immediate response, never persisted -
            // matches the original app, which never stored frames in swing history either.
            response.setFrames(frames);
            return response;
        } catch (IOException e) {
            throw new AnalysisFailedException("Failed to process uploaded video", e);
        } finally {
            deleteQuietly(tempVideo);
        }
    }

    public List<SwingAnalysisResponse> getHistory() {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, HISTORY_LIMIT)).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public SwingAnalysisResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    public void delete(Long id) {
        repository.delete(findEntity(id));
    }

    private SwingAnalysis findEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Swing analysis not found with id " + id));
    }

    private void validateUpload(MultipartFile video) {
        if (video == null || video.isEmpty()) {
            throw new InvalidUploadException("No video file provided");
        }

        String contentType = video.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.US).startsWith("video/")) {
            throw new InvalidUploadException(
                    "That doesn't look like a video file. Please upload an MP4, MOV, or similar video format.");
        }

        if (video.getSize() > maxUploadBytes) {
            throw new InvalidUploadException(
                    "File is too large. Please upload a video under " + (maxUploadBytes / 1024 / 1024) + "MB.");
        }
    }

    private SwingAnalysis toEntity(AiAnalysisResult aiResult) {
        SwingAnalysis analysis = new SwingAnalysis();
        analysis.setShotType(aiResult.getShotType());
        analysis.setOverallScore(aiResult.getOverallScore());
        analysis.setTopPriority(aiResult.getTopPriority());
        analysis.setDrillRecommendation(aiResult.getDrillRecommendation());

        // Same category -> frame index mapping as the original route handler.
        List<String> orderedNames = List.of(
                "Stance & Preparation", "Backswing & Unit Turn", "Contact Point",
                "Follow-Through", "Footwork & Balance");

        List<AiCategoryResult> categories = aiResult.getCategories();
        for (int i = 0; i < categories.size(); i++) {
            AiCategoryResult c = categories.get(i);
            SwingCategory category = new SwingCategory();
            category.setName(c.getName());
            category.setScore(c.getScore());
            category.setSeverity(parseSeverity(c.getSeverity()));
            category.setObservation(c.getObservation());
            category.setTip(c.getTip());

            int mappedIndex = orderedNames.indexOf(c.getName());
            category.setFrameIndex(mappedIndex >= 0 ? mappedIndex : i);

            analysis.addCategory(category);
        }

        return analysis;
    }

    private Severity parseSeverity(String raw) {
        if (raw == null) return Severity.NEEDS_WORK;
        switch (raw.toLowerCase(Locale.US)) {
            case "good": return Severity.GOOD;
            case "critical": return Severity.CRITICAL;
            default: return Severity.NEEDS_WORK;
        }
    }

    private SwingAnalysisResponse toResponse(SwingAnalysis analysis) {
        List<SwingCategoryDto> categoryDtos = analysis.getCategories().stream()
                .map(c -> new SwingCategoryDto(
                        c.getName(), c.getScore(), c.getSeverity(), c.getObservation(), c.getTip(), c.getFrameIndex()))
                .collect(Collectors.toList());

        return new SwingAnalysisResponse(
                analysis.getId(),
                analysis.getShotType(),
                analysis.getOverallScore(),
                analysis.getTopPriority(),
                analysis.getDrillRecommendation(),
                analysis.getCreatedAt(),
                categoryDtos);
    }

    private void deleteQuietly(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // best-effort cleanup, matches cleanupVideo()'s swallow-and-log behavior
        }
    }
}
