package com.jesusbarocio.bedererai.entity;

import javax.persistence.*;

@Entity
@Table(name = "swing_categories")
public class SwingCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Column(length = 1000)
    private String observation;

    @Column(length = 1000)
    private String tip;

    private Integer frameIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "swing_analysis_id", nullable = false)
    private SwingAnalysis swingAnalysis;

    public SwingCategory() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    public String getTip() {
        return tip;
    }

    public void setTip(String tip) {
        this.tip = tip;
    }

    public Integer getFrameIndex() {
        return frameIndex;
    }

    public void setFrameIndex(Integer frameIndex) {
        this.frameIndex = frameIndex;
    }

    public SwingAnalysis getSwingAnalysis() {
        return swingAnalysis;
    }

    public void setSwingAnalysis(SwingAnalysis swingAnalysis) {
        this.swingAnalysis = swingAnalysis;
    }
}
