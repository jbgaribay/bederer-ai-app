package com.jesusbarocio.bedererai.dto;

import com.jesusbarocio.bedererai.entity.Severity;

public class SwingCategoryDto {

    private String name;
    private Double score;
    private Severity severity;
    private String observation;
    private String tip;
    private Integer frameIndex;

    public SwingCategoryDto() {
    }

    public SwingCategoryDto(String name, Double score, Severity severity, String observation,
                             String tip, Integer frameIndex) {
        this.name = name;
        this.score = score;
        this.severity = severity;
        this.observation = observation;
        this.tip = tip;
        this.frameIndex = frameIndex;
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
}
