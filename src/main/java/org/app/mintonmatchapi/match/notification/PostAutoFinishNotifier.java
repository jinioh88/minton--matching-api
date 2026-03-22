package org.app.mintonmatchapi.match.notification;

import java.util.List;

/**
 * 자동 종료 배치 이후 후기 작성 독려 알림 등을 붙일 때 구현한다 (Sprint 5 알림 연동 예정).
 */
public interface PostAutoFinishNotifier {

    void onMatchesAutoFinished(List<Long> matchIds);
}
