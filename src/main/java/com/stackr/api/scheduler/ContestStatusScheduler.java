package com.stackr.api.scheduler;

import com.stackr.api.entity.enums.ContestStatus;
import com.stackr.api.repository.ContestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContestStatusScheduler {

    private final ContestRepository contestRepository;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void updateStatuses() {
        var now = LocalDateTime.now();

        var toStart = contestRepository.findUpcomingToStart(now);
        toStart.forEach(c -> {
            c.setStatus(ContestStatus.RUNNING);
            log.info("Contest {} started", c.getId());
        });
        if (!toStart.isEmpty()) contestRepository.saveAll(toStart);

        var toFinish = contestRepository.findRunningToFinish(now);
        toFinish.forEach(c -> {
            c.setStatus(ContestStatus.FINISHED);
            log.info("Contest {} finished", c.getId());
        });
        if (!toFinish.isEmpty()) contestRepository.saveAll(toFinish);
    }
}
