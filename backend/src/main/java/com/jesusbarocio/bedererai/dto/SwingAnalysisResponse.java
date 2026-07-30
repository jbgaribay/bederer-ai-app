package com.jesusbarocio.bedererai.dto;

import java.time.Instant;
import java.util.List;

/**
 * Note on `frames`: like the original app, reference frame images are only
 * ever included in the response returned immediately after POST /api/swings
 * (freshly extracted, never persisted). GET /api/swings and GET /api/swings/{id}
 * return this same DTO shape but with frames left null - history doesn't
 * re-serve images, matching the original localStorage-based history which
 * never stored frames either.
 */
public class SwingAnalysisResponse {

    private Long id;
    private String shotType;
    private Double overallScore;
    private String topPriority;
    private String drillRecommendation;
    private Instant createdAt;
    private List<SwingCategoryDto> categories;
    private List<String> frames;

    public SwingAnalysisResponse() {
    }

    public SwingAnalysisResponse(Long id, String shotType, Double overallScore, String topPriority,
                                  String drillRecommendation, Instant createdAt, List<SwingCategoryDto> categories) {
        this(id, shotType, overallScore, topPriority, drillRecommendation, createdAt, categories, null);
    }

    public SwingAnalysisResponse(Long id, String shotType, Double overallScore, String topPriority,
                                  String drillRecommendation, Instant createdAt, List<SwingCategoryDto> categories,
                                  List<String> frames) {
        this.id = id;
        this.shotType = shotType;
        this.overallScore = overallScore;
        this.topPriority = topPriority;
        this.drillRecommendation = drillRecommendation;
        this.createdAt = createdAt;
        this.categories = categories;
        this.frames = frames;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getShotType() {
        return shotType;
    }

    public void setShotType(String shotType) {
        this.shotType = shotType;
    }

    public Double getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(Double overallScore) {
        this.overallScore = overallScore;
    }

    public String getTopPriority() {
        return topPriority;
    }

    public void setTopPriority(String topPriority) {
        this.topPriority = topPriority;
    }

    public String getDrillRecommendation() {
        return drillRecommendation;
    }

    public void setDrillRecommendation(String drillRecommendation) {
        this.drillRecommendation = drillRecommendation;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<SwingCategoryDto> getCategories() {
        return categories;
    }

    public void setCategories(List<SwingCategoryDto> categories) {
        this.categories = categories;
    }

    public List<String> getFrames() {
        return frames;
    }

    public void setFrames(List<String> frames) {
        this.frames = frames;
    }
}
