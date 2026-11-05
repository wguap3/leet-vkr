package com.stackr.api.scheduler;

import com.stackr.api.service.SubmissionProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** PENDING rows survive process restarts; no in-memory queue or commit race. */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.judge.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class SubmissionQueueWorker {
    private final SubmissionProcessor processor;

    @Scheduled(fixedDelayString = "${app.judge.poll-delay-ms:1000}")
    public void poll() {
        try { processor.processNext(); }
        catch (Exception e) { log.error("Submission queue iteration failed", e); }
    }
}
