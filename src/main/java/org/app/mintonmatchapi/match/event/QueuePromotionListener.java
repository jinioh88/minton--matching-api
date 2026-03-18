package org.app.mintonmatchapi.match.event;

import org.app.mintonmatchapi.match.service.QueuePromotionService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class QueuePromotionListener {

    private final QueuePromotionService queuePromotionService;

    public QueuePromotionListener(QueuePromotionService queuePromotionService) {
        this.queuePromotionService = queuePromotionService;
    }

    @Async
    @EventListener
    public void onParticipantCancelled(ParticipantCancelledEvent event) {
        if (!event.requiresQueuePromotion()) {
            return;
        }
        queuePromotionService.promoteOnCancelled(event.getMatchId());
    }
}
