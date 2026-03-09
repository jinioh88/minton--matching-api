# 배드민턴 매칭 API 문서

> 프론트엔드 개발자를 위한 API 명세서입니다. GitHub에서 직접 확인할 수 있습니다.

## 기본 정보

- **Base URL**: `http://localhost:8080/api`
- **Content-Type**: `application/json`

## 공통 응답 형식

### 성공 응답

```json
{
  "success": true,
  "message": null,
  "data": { ... }
}
```

### 실패 응답

```json
{
  "success": false,
  "message": "에러 메시지",
  "code": "ERROR_CODE"
}
```

## API 목록

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | /api/health | 서버 상태 확인 |

---

## 상세 API

### 헬스 체크

**GET** `/api/health`

서버 상태를 확인합니다.

**응답 예시**

```json
{
  "success": true,
  "data": {
    "status": "UP",
    "service": "minton-match-api"
  }
}
```
