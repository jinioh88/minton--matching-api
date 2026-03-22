package org.app.mintonmatchapi.match.scheduler;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.app.mintonmatchapi.match.service.MatchService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MatchAutoFinishScheduler {

    private final MatchService matchService;

    public MatchAutoFinishScheduler(MatchService matchService) {
        this.matchService = matchService;
    }

    @Scheduled(cron = "${match.auto-finish-cron:0 0 * * * *}")
    @SchedulerLock(name = "autoFinishMatches", lockAtMostFor = "9m", lockAtLeastFor = "30s")
    public void runAutoFinish() {
        matchService.autoFinishMatches();
    }
}
