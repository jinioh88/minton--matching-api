package org.app.mintonmatchapi.auth.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 선택적 인증(Optional Authentication)을 위한 어노테이션.
 * <p>
 * - 로그인된 경우: UserPrincipal 반환
 * - 비로그인: null 반환
 * <p>
 * 매칭 목록 검색 등 비로그인 접근이 가능하되, 로그인 시 추가 정보(관심 지역 기본값 등)를 적용할 때 사용.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface IfLogin {
}
