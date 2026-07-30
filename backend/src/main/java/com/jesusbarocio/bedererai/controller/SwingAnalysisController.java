package com.jesusbarocio.bedererai.controller;

import com.jesusbarocio.bedererai.dto.SwingAnalysisResponse;
import com.jesusbarocio.bedererai.service.SwingAnalysisService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/swings")
public class SwingAnalysisController {

    private final SwingAnalysisService swingAnalysisService;

    public SwingAnalysisController(SwingAnalysisService swingAnalysisService) {
        this.swingAnalysisService = swingAnalysisService;
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public SwingAnalysisResponse analyze(
            @RequestParam("video") MultipartFile video,
            @RequestParam(value = "shotType", defaultValue = "forehand") String shotType) {
        return swingAnalysisService.analyze(video, shotType);
    }

    @GetMapping
    public List<SwingAnalysisResponse> getHistory() {
        return swingAnalysisService.getHistory();
    }

    @GetMapping("/{id}")
    public SwingAnalysisResponse getById(@PathVariable Long id) {
        return swingAnalysisService.getById(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        swingAnalysisService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
