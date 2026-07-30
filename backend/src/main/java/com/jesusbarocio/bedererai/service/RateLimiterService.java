package com.jesusbarocio.bedererai.service;

import com.jesusbarocio.bedererai.exception.RateLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Guards the one endpoint that costs real money (video analysis: ffmpeg CPU
 * time + a Claude API call). A simple in-memory daily counter is enough for
 * a single-instance demo deployment; a multi-instance production deployment
 * would move this to a shared store (Redis) instead.
 */
@Service
public class RateLimiterService {

    private final int maxAnalysesPerDay;

    private LocalDate windowDate = LocalDate.now(ZoneOffset.UTC);
    private int countToday = 0;

    public RateLimiterService(@Value("${app.rate-limit.max-analyses-per-day:20}") int maxAnalysesPerDay) {
        this.maxAnalysesPerDay = maxAnalysesPerDay;
    }

    public synchronized void checkAndIncrement() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if (!today.equals(windowDate)) {
            windowDate = today;
            countToday = 0;
        }

        if (countToday >= maxAnalysesPerDay) {
            throw new RateLimitExceededException(
                    "Daily analysis limit reached (" + maxAnalysesPerDay + "/day). Please try again tomorrow.");
        }

        countToday++;
    }
}
