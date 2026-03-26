package org.app.mintonmatchapi.notification.push;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@Getter
@Setter
@ConfigurationProperties(prefix = "minton.push.fcm")
public class FcmPushProperties {

    /**
     * true 이면 Firebase 초기화 및 FCM 발송을 시도한다. 로컬·테스트는 false 권장.
     */
    private boolean enabled = false;

    /**
     * 서비스 계정 JSON 파일 경로. 비우면 {@code GOOGLE_APPLICATION_CREDENTIALS} 등 애플리케이션 기본 자격 증명을 사용한다.
     */
    private String credentialsPath = "";

    public boolean hasCredentialsPath() {
        return StringUtils.hasText(credentialsPath);
    }
}
