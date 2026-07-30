package com.jesusbarocio.bedererai.entity;

import javax.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "swing_analyses")
public class SwingAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String shotType;

    @Column(nullable = false)
    private Double overallScore;

    @Column(length = 1000)
    private String topPriority;

    @Column(length = 1000)
    private String drillRecommendation;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "swingAnalysis", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("frameIndex ASC")
    private List<SwingCategory> categories = new ArrayList<>();

    public SwingAnalysis() {
    }

    public void addCategory(SwingCategory category) {
        category.setSwingAnalysis(this);
        categories.add(category);
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

    public List<SwingCategory> getCategories() {
        return categories;
    }

    public void setCategories(List<SwingCategory> categories) {
        this.categories = categories;
    }
}
