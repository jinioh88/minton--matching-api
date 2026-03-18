package org.app.mintonmatchapi.match.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Component
@ConfigurationProperties(prefix = "queue")
public class QueueProperties {

    private int offerTimeoutMinutes = 15;
    private int emergencyThresholdHours = 2;

    public void setOfferTimeoutMinutes(int offerTimeoutMinutes) {
        this.offerTimeoutMinutes = offerTimeoutMinutes;
    }

    public void setEmergencyThresholdHours(int emergencyThresholdHours) {
        this.emergencyThresholdHours = emergencyThresholdHours;
    }
}
