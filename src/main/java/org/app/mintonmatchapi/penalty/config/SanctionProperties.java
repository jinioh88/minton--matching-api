package org.app.mintonmatchapi.penalty.config;

import lombok.Getter;
import lombok.Setter;
import org.app.mintonmatchapi.penalty.entity.PenaltyType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Component
@ConfigurationProperties(prefix = "sanction")
public class SanctionProperties {

    private StrikeThresholds strikeThresholds = new StrikeThresholds();
    private Durations durations = new Durations();
    private Map<String, Integer> penaltyWeights = new LinkedHashMap<>();

    public void setStrikeThresholds(StrikeThresholds strikeThresholds) {
        this.strikeThresholds = strikeThresholds != null ? strikeThresholds : new StrikeThresholds();
    }

    public void setDurations(Durations durations) {
        this.durations = durations != null ? durations : new Durations();
    }

    public void setPenaltyWeights(Map<String, Integer> penaltyWeights) {
        this.penaltyWeights = penaltyWeights != null ? penaltyWeights : new LinkedHashMap<>();
    }

    public int penaltyWeight(PenaltyType type) {
        return penaltyWeights.getOrDefault(type.name(), 1);
    }

    @Getter
    @Setter
    public static class StrikeThresholds {
        private int participationBan = 3;
        private int fullSuspension = 5;
        private int permanentBan = 10;
    }

    @Getter
    @Setter
    public static class Durations {
        private int participationBanDays = 3;
        private int suspensionDays = 7;
    }
}
