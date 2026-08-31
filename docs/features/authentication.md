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
- 이메일·비밀번호 로그인과 Access Token·Refresh Token 발급
- Refresh Token의 해시 저장, 회전, 로그아웃

건강활동 API는 아직 구현하지 않았습니다.

## 접근 정책

| 경로 | 현재 정책 |
| --- | --- |
| `POST /api/v1/members` | 비인증 허용 |
| `POST /api/v1/auth/login` | 비인증 허용 |
| `POST /api/v1/auth/refresh` | 비인증 허용 |
| `POST /api/v1/auth/logout` | 비인증 허용 |
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

## 인증 API 사용법

### 로그인

`POST /api/v1/auth/login`

```json
{
  "email": "member@example.com",
  "password": "password"
}
```

성공하면 다음 형식으로 Access Token, Refresh Token, Access Token 만료 시각을 반환합니다. Access Token은 15분 동안 유효합니다.

```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "accessTokenExpiresAt": "2026-01-01T00:15:00Z"
}
```

보호 API에는 다음 형식으로 Access Token을 전달합니다.

```http
Authorization: Bearer {accessToken}
```

## Refresh Token 회전

- Refresh Token 원문은 저장하지 않고 SHA-256 해시만 MySQL에 저장합니다.
- 로그인과 갱신 시 새 Refresh Token은 발급·회전 시점부터 14일 동안 유효합니다.
- 갱신에 성공하면 기존 토큰은 `ROTATED` 상태로 보존하고, 새 토큰을 같은 계열로 연결합니다.
- 회전·폐기된 토큰의 재사용은 탈취 가능성으로 간주합니다. 해당 계열의 활성 토큰을 폐기하고, 외부에는 `401 AUTH_INVALID_REFRESH_TOKEN`만 반환합니다.
- 동일 Refresh Token에 대한 갱신 요청은 클라이언트에서 직렬화해야 합니다. 동시에 전송된 후속 요청은 재사용으로 감지되어 토큰 계열이 폐기될 수 있습니다.

### 토큰 갱신 API

`POST /api/v1/auth/refresh`

```json
{
  "refreshToken": "..."
}
```

성공하면 로그인과 같은 응답 형식으로 새 Access Token, 새 Refresh Token, Access Token 만료 시각을 반환합니다. Refresh Token은 URL, 로그, 오류 응답에 포함하지 않습니다.

## 로그아웃

`POST /api/v1/auth/logout`

```json
{
  "refreshToken": "..."
}
```

요청 본문의 `refreshToken`으로 해당 토큰 계열의 활성 Refresh Token을 폐기하고 `204 No Content`를 반환합니다. 존재하지 않거나 이미 만료·폐기된 토큰도 동일하게 `204`를 반환합니다.

Access Token은 Stateless JWT이므로 로그아웃 후에도 만료 시각까지 최대 15분 동안 유효할 수 있습니다.

## 인증 오류

| 오류 코드 | HTTP 상태 | 설명 |
| --- | --- | --- |
| `AUTH_INVALID_CREDENTIALS` | `401 Unauthorized` | 이메일 또는 비밀번호가 올바르지 않습니다. |
| `AUTH_INVALID_REFRESH_TOKEN` | `401 Unauthorized` | Refresh Token이 유효하지 않습니다. 만료·위조·회전·폐기 여부는 구분하지 않습니다. |

## 검증

- 회원 ID를 `sub` Claim으로 담아 Access Token을 발급합니다.
- JWT 서명, 만료 시각, 발행자를 검증합니다.
- 유효한 Bearer Token만 보호 경로의 인증 필터를 통과하는지 MockMvc로 검증합니다.
- Testcontainers MySQL에서 Refresh Token 회전, 재사용 감지, 동시 갱신 시 하나만 성공하는 동작을 검증합니다.
- 로그아웃 뒤 Refresh Token 갱신이 거부되고, 반복 로그아웃이 항상 `204`를 반환하는지 검증합니다.
