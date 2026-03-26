package org.app.mintonmatchapi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

/**
 * CORS·WebSocket(STOMP) 허용 origin 패턴. 쉼표로 구분해 환경변수에 넣을 수 있다.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "minton.web")
public class MintonWebProperties {

    /**
     * 예: {@code https://app.example.com} 또는 복수 시 {@code https://a.com,https://b.com}
     */
    private String corsAllowedOriginPatterns = "http://localhost:3000,http://127.0.0.1:3000";

    public List<String> corsAllowedOriginPatternList() {
        return Arrays.stream(corsAllowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public String[] corsAllowedOriginPatternArray() {
        List<String> list = corsAllowedOriginPatternList();
        return list.toArray(new String[0]);
    }
}
