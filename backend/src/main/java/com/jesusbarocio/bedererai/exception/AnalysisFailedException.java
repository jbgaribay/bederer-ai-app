package com.jesusbarocio.bedererai.exception;

/**
 * Wraps failures from ffmpeg extraction or the Claude API call so the
 * controller layer has one exception type to translate into a 502/500,
 * instead of leaking IOException/InterruptedException details to the client.
 */
public class AnalysisFailedException extends RuntimeException {
    public AnalysisFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
