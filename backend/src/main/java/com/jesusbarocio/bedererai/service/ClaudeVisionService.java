package com.jesusbarocio.bedererai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jesusbarocio.bedererai.exception.AnalysisFailedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Java port of lib/ai.ts. The original called the Anthropic Node SDK; this
 * calls the same Messages API directly over HTTP with java.net.http.HttpClient
 * and Jackson, so there's no extra Anthropic-specific dependency to manage.
 * Same system/user prompt, same requested JSON schema, same model.
 */
@Service
public class ClaudeVisionService {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private static final String SYSTEM_PROMPT =
            "You are an expert tennis coach with 20 years of experience coaching players from "
            + "beginners to professionals. You specialize in biomechanical analysis of tennis "
            + "strokes and provide clear, actionable feedback.";

    private final String apiKey;
    private final String model;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public ClaudeVisionService(
            @Value("${app.claude.api-key}") String apiKey,
            @Value("${app.claude.model:claude-sonnet-4-20250514}") String model,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public AiAnalysisResult analyzeSwing(List<String> base64Frames, String shotType) {
        try {
            String requestBody = buildRequestBody(base64Frames, shotType);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(60))
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new AnalysisFailedException(
                        "Claude API returned status " + response.statusCode() + ": " + response.body(), null);
            }

            return parseResponse(response.body());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AnalysisFailedException("Failed to call Claude API", e);
        }
    }

    private String buildRequestBody(List<String> base64Frames, String shotType) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.put("max_tokens", 1500);
        root.put("temperature", 0.3);
        root.put("system", SYSTEM_PROMPT);

        ArrayNode content = objectMapper.createArrayNode();

        ObjectNode textBlock = objectMapper.createObjectNode();
        textBlock.put("type", "text");
        textBlock.put("text", buildUserPrompt(base64Frames.size(), shotType));
        content.add(textBlock);

        for (String frame : base64Frames) {
            ObjectNode imageBlock = objectMapper.createObjectNode();
            imageBlock.put("type", "image");

            ObjectNode source = objectMapper.createObjectNode();
            source.put("type", "base64");
            source.put("media_type", "image/jpeg");
            source.put("data", frame);

            imageBlock.set("source", source);
            content.add(imageBlock);
        }

        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", "user");
        message.set("content", content);

        ArrayNode messages = objectMapper.createArrayNode();
        messages.add(message);
        root.set("messages", messages);

        return objectMapper.writeValueAsString(root);
    }

    private String buildUserPrompt(int frameCount, String shotType) {
        return "I will provide you with " + frameCount + " frames from a video showing a tennis "
                + shotType + " swing. The frames show the progression from preparation through "
                + "follow-through.\n\n"
                + "Analyze this player's technique across these key categories:\n"
                + "1. Stance & Preparation\n"
                + "2. Backswing & Unit Turn\n"
                + "3. Contact Point\n"
                + "4. Follow-Through\n"
                + "5. Footwork & Balance\n\n"
                + "For each category, provide:\n"
                + "- A score out of 10\n"
                + "- A severity indicator: \"good\" (7-10), \"needs_work\" (4-6), or \"critical\" (1-3)\n"
                + "- A one-sentence observation about what you see\n"
                + "- One specific, actionable tip for improvement\n\n"
                + "Also provide:\n"
                + "- An overall score (average of categories)\n"
                + "- The single most important thing this player should work on (top_priority)\n"
                + "- A specific drill recommendation based on their weaknesses\n\n"
                + "Return your response as a JSON object with this exact structure:\n\n"
                + "{\n"
                + "  \"overall_score\": <number>,\n"
                + "  \"shot_type\": \"" + shotType + "\",\n"
                + "  \"categories\": [\n"
                + "    {\"name\": \"Stance & Preparation\", \"score\": <number>, \"severity\": <\"good\"|\"needs_work\"|\"critical\">, \"observation\": \"<string>\", \"tip\": \"<string>\"},\n"
                + "    {\"name\": \"Backswing & Unit Turn\", \"score\": <number>, \"severity\": <\"good\"|\"needs_work\"|\"critical\">, \"observation\": \"<string>\", \"tip\": \"<string>\"},\n"
                + "    {\"name\": \"Contact Point\", \"score\": <number>, \"severity\": <\"good\"|\"needs_work\"|\"critical\">, \"observation\": \"<string>\", \"tip\": \"<string>\"},\n"
                + "    {\"name\": \"Follow-Through\", \"score\": <number>, \"severity\": <\"good\"|\"needs_work\"|\"critical\">, \"observation\": \"<string>\", \"tip\": \"<string>\"},\n"
                + "    {\"name\": \"Footwork & Balance\", \"score\": <number>, \"severity\": <\"good\"|\"needs_work\"|\"critical\">, \"observation\": \"<string>\", \"tip\": \"<string>\"}\n"
                + "  ],\n"
                + "  \"top_priority\": \"<string>\",\n"
                + "  \"drill_recommendation\": \"<string>\"\n"
                + "}\n\n"
                + "Be specific and avoid generic advice like \"practice more\" or \"watch your form.\"";
    }

    private AiAnalysisResult parseResponse(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode contentArray = root.path("content");

        String rawText = null;
        for (JsonNode block : contentArray) {
            if ("text".equals(block.path("type").asText())) {
                rawText = block.path("text").asText();
                break;
            }
        }

        if (rawText == null) {
            throw new AnalysisFailedException("No text response from Claude", null);
        }

        String jsonText = rawText.trim();
        if (jsonText.startsWith("```json")) {
            jsonText = jsonText.replaceFirst("^```json\\s*", "").replaceFirst("```\\s*$", "");
        } else if (jsonText.startsWith("```")) {
            jsonText = jsonText.replaceFirst("^```\\s*", "").replaceFirst("```\\s*$", "");
        }

        return objectMapper.readValue(jsonText, AiAnalysisResult.class);
    }
}
