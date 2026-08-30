# JWT 인증 기반

> 상태: 일부 구현

## 목적

회원가입 이후의 보호 API에서 Bearer JWT로 인증된 회원을 식별할 수 있도록 Spring Security 기반 인증 경계를 구성합니다.

## 현재 구현 범위

- Spring Security의 Stateless 보안 필터 체인
- HS256으로 서명한 JWT Access Token 발급과 검증
- JWT의 서명, 만료 시각, 발행자 검증
- 인증 실패 시 공통 오류 응답
- 회원가입 API의 비인증 접근 허용

로그인, Refresh Token 발급·회전, 로그아웃, 건강활동 API는 아직 구현하지 않았습니다.

## 접근 정책

| 경로 | 현재 정책 |
| --- | --- |
| `POST /api/v1/members` | 비인증 허용 |
| `POST /api/v1/auth/login` | 비인증 허용. API 구현 전 |
| `POST /api/v1/auth/refresh` | 비인증 허용. API 구현 전 |
| `POST /api/v1/auth/logout` | 비인증 허용. API 구현 전 |
| 그 외 경로 | Bearer JWT 인증 필요 |

유효하지 않거나 없는 Bearer Token으로 보호 경로에 접근하면 다음 형식의 `401 Unauthorized` 응답을 반환합니다.

```json
{
  "status": 401,
  "code": "UNAUTHENTICATED",
  "message": "인증이 필요합니다.",
  "path": "/api/v1/activities"
}
```

## JWT 설정

애플리케이션 실행 환경에는 다음 환경변수가 필요합니다.

| 환경변수 | 설명 |
| --- | --- |
| `APP_SECURITY_JWT_ISSUER` | JWT 발행자를 식별하는 URI 값 |
| `APP_SECURITY_JWT_SECRET` | Base64 인코딩한 256비트 이상 HS256 서명 키 |

서명 키 원문은 저장소, 설정 파일, 로그에 포함하지 않습니다. 테스트는 별도의 테스트 전용 설정값을 사용합니다.

## 검증

- 회원 ID를 `sub` Claim으로 담아 Access Token을 발급합니다.
- JWT 서명, 만료 시각, 발행자를 검증합니다.
- 유효한 Bearer Token만 보호 경로의 인증 필터를 통과하는지 MockMvc로 검증합니다.
