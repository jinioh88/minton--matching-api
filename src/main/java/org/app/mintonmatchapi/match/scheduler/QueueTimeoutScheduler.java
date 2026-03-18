package org.app.mintonmatchapi.match.scheduler;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.app.mintonmatchapi.match.service.QueuePromotionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class QueueTimeoutScheduler {

    private final QueuePromotionService queuePromotionService;

    public QueueTimeoutScheduler(QueuePromotionService queuePromotionService) {
        this.queuePromotionService = queuePromotionService;
    }

    @Scheduled(fixedRate = 60000) // 1분마다
    @SchedulerLock(name = "processExpiredReservations", lockAtMostFor = "90s", lockAtLeastFor = "30s")
    public void processExpiredReservations() {
        queuePromotionService.processExpiredReservations();
    }
}
