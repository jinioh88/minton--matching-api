package org.app.mintonmatchapi.config;

import org.h2.tools.Server;

import java.util.Arrays;

/**
 * 로컬 프로파일에서 Spring 시작 전에 H2 TCP 서버를 먼저 실행합니다.
 * 파일 DB 잠금 문제를 해결하고, 앱과 H2 콘솔 동시 접속을 가능하게 합니다.
 */
public final class LocalH2ServerStarter {

    static final int H2_TCP_PORT = 9092;

    private static Server tcpServer;

    public static void startIfLocal(String[] args) {
        boolean isProd = Arrays.stream(args)
                .anyMatch(a -> a.contains("spring.profiles.active") && a.contains("prod"));
        if (isProd) {
            return;
        }
        try {
            tcpServer = Server.createTcpServer(
                    "-tcp",
                    "-tcpPort", String.valueOf(H2_TCP_PORT),
                    "-tcpAllowOthers",
                    "-ifNotExists"
            );
            tcpServer.start();
        } catch (Exception e) {
            // 이미 실행 중인 서버가 있으면 무시 (재시작 시)
        }
    }

    public static void stop() {
        if (tcpServer != null && tcpServer.isRunning(false)) {
            tcpServer.stop();
        }
    }
}
