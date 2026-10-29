package com.example.testprojmesh.scheduler;

import com.example.testprojmesh.service.InterestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.interest.enabled", havingValue = "true", matchIfMissing = true)
public class InterestScheduler {

    private final InterestService interest;

    @Scheduled(fixedRateString = "${app.interest.interval:PT30S}")
    void run() {
        try {
            int updated = interest.accrue();
            log.info("Interest accrual updated {} accounts", updated);
        } catch (RuntimeException e) {
            log.error("Interest accrual failed; retrying at the next tick", e);
        }
    }
}
