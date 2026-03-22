package org.app.mintonmatchapi.match.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Component
@ConfigurationProperties(prefix = "match")
public class MatchProperties {

    /**
     * 경기 시작 시각 기준 이 시간(시간 단위)이 지난 CLOSED 매칭을 스케줄러가 FINISHED로 전환한다.
     */
    private int autoFinishAfterStartHours = 6;

    /**
     * 자동 종료 배치 크론 (초 분 시 일 월 요일). 기본: 매시 정각.
     */
    private String autoFinishCron = "0 0 * * * *";

    /**
     * 수동 종료 API에서 경기 시작 시각이 아직 오지 않은 매칭을 거절할지 여부.
     */
    private boolean requirePastStartForManualFinish = true;

    public void setAutoFinishAfterStartHours(int autoFinishAfterStartHours) {
        this.autoFinishAfterStartHours = autoFinishAfterStartHours;
    }

    public void setAutoFinishCron(String autoFinishCron) {
        this.autoFinishCron = autoFinishCron;
    }

    public void setRequirePastStartForManualFinish(boolean requirePastStartForManualFinish) {
        this.requirePastStartForManualFinish = requirePastStartForManualFinish;
    }
}
