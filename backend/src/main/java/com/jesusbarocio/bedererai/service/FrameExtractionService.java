package com.jesusbarocio.bedererai.service;

import com.jesusbarocio.bedererai.exception.AnalysisFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Java port of lib/ffmpeg.ts. The original Next.js app used the fluent-ffmpeg
 * npm package, which is itself just a wrapper that shells out to the ffmpeg
 * CLI binary. This does the same thing directly via ProcessBuilder: probe the
 * video's duration with ffprobe, then grab N frames at evenly spaced
 * timestamps with ffmpeg, one -frames:v 1 seek per frame.
 *
 * Requires the ffmpeg and ffprobe binaries to be present on PATH (installed
 * in the Docker image; see backend/Dockerfile).
 */
@Service
public class FrameExtractionService {

    private static final Logger log = LoggerFactory.getLogger(FrameExtractionService.class);

    private final String ffmpegBinary;
    private final String ffprobeBinary;
    private final long timeoutSeconds;

    public FrameExtractionService(
            @Value("${app.ffmpeg.binary:ffmpeg}") String ffmpegBinary,
            @Value("${app.ffmpeg.ffprobe-binary:ffprobe}") String ffprobeBinary,
            @Value("${app.ffmpeg.timeout-seconds:30}") long timeoutSeconds) {
        this.ffmpegBinary = ffmpegBinary;
        this.ffprobeBinary = ffprobeBinary;
        this.timeoutSeconds = timeoutSeconds;
    }

    public List<String> extractFrames(Path videoPath, int numberOfFrames) {
        Path frameDir = null;
        try {
            double durationSeconds = probeDuration(videoPath);
            frameDir = Files.createTempDirectory("bederer-frames-");

            List<String> base64Frames = new ArrayList<>();
            for (int i = 0; i < numberOfFrames; i++) {
                // Evenly spaced timestamps, skipping the very first/last instant
                // so we don't grab a black or half-formed frame.
                double fraction = (i + 1.0) / (numberOfFrames + 1.0);
                double timestampSeconds = durationSeconds * fraction;

                Path framePath = frameDir.resolve(String.format(Locale.US, "frame-%d.jpg", i));
                extractSingleFrame(videoPath, timestampSeconds, framePath);

                byte[] bytes = Files.readAllBytes(framePath);
                base64Frames.add(Base64.getEncoder().encodeToString(bytes));
            }

            return base64Frames;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AnalysisFailedException("Failed to extract frames from video", e);
        } finally {
            cleanupDirectory(frameDir);
        }
    }

    private double probeDuration(Path videoPath) throws IOException, InterruptedException {
        List<String> command = List.of(
                ffprobeBinary,
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "csv=p=0",
                videoPath.toAbsolutePath().toString()
        );

        String output = runProcess(command).trim();
        try {
            return Double.parseDouble(output);
        } catch (NumberFormatException e) {
            throw new AnalysisFailedException("Could not determine video duration", e);
        }
    }

    private void extractSingleFrame(Path videoPath, double timestampSeconds, Path outputPath)
            throws IOException, InterruptedException {
        List<String> command = List.of(
                ffmpegBinary,
                "-y",
                "-ss", String.format(Locale.US, "%.3f", timestampSeconds),
                "-i", videoPath.toAbsolutePath().toString(),
                "-frames:v", "1",
                "-q:v", "2",
                outputPath.toAbsolutePath().toString()
        );
        runProcess(command);
    }

    private String runProcess(List<String> command) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();

        // Drain stdout/stderr on a background thread instead of blocking the
        // main thread on readAllBytes() before checking the timeout below.
        // readAllBytes() only returns once the process closes its output
        // stream (i.e. once it exits) - calling it first meant the timeout
        // check below never actually got a chance to fire on a slow/stuck
        // process, since we'd already be blocked waiting for output. This
        // matters a lot on a resource-constrained host (e.g. Render's free
        // tier), where ffmpeg can legitimately take much longer than it does
        // locally.
        StringBuilder output = new StringBuilder();
        Thread outputReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                }
            } catch (IOException ignored) {
                // Stream closes/breaks when the process exits or is destroyed.
            }
        });
        outputReader.setDaemon(true);
        outputReader.start();

        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new AnalysisFailedException("ffmpeg/ffprobe timed out after " + timeoutSeconds + "s", null);
        }

        outputReader.join(2000);

        if (process.exitValue() != 0) {
            log.error("Command failed ({}): {}", String.join(" ", command), output);
            throw new AnalysisFailedException("ffmpeg/ffprobe exited with code " + process.exitValue(), null);
        }
        return output.toString();
    }

    private void cleanupDirectory(Path dir) {
        if (dir == null) return;
        try {
            if (Files.exists(dir)) {
                Files.walk(dir)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException e) {
                                log.warn("Failed to delete temp file {}", p, e);
                            }
                        });
            }
        } catch (IOException e) {
            log.warn("Failed to clean up frame directory {}", dir, e);
        }
    }
}
