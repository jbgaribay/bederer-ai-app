package com.jesusbarocio.bedererai.service;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Mirrors types/analysis.ts's SwingAnalysis shape (minus the `frames` field,
 * which we keep separate since it's the raw base64 images, not part of the
 * AI's JSON response).
 */
public class AiAnalysisResult {

    @JsonProperty("overall_score")
    private Double overallScore;

    @JsonProperty("shot_type")
    private String shotType;

    private List<AiCategoryResult> categories;

    @JsonProperty("top_priority")
    private String topPriority;

    @JsonProperty("drill_recommendation")
    private String drillRecommendation;

    public Double getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(Double overallScore) {
        this.overallScore = overallScore;
    }

    public String getShotType() {
        return shotType;
    }

    public void setShotType(String shotType) {
        this.shotType = shotType;
    }

    public List<AiCategoryResult> getCategories() {
        return categories;
    }

    public void setCategories(List<AiCategoryResult> categories) {
        this.categories = categories;
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
}
