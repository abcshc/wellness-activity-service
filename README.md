# wellness-activity-service

건강활동 데이터를 수집·저장하고, 사용자별 일별·월별 활동 요약을 제공하는 백엔드 서비스입니다.

## 목표

외부 건강 플랫폼에서 전달되는 활동 데이터를 표준 형식으로 정규화하여 저장하고, 사용자가 자신의 건강활동을 일별·월별로 조회할 수 있도록 합니다.

## 현재 구현 범위

- `POST /api/v1/members` 회원가입
- BCrypt 기반 비밀번호 해시 저장
- 이메일 중복 및 요청 필드 검증
- 공통 오류 응답
- JWT Bearer Token 인증 기반

로그인, Refresh Token, 건강활동 데이터 수집·조회 기능은 아직 구현하지 않았습니다.

## 기술 방향

- Java 17
- Spring Boot
- Spring Data JPA
- MySQL
- 기능 중심 패키지 구조
- 외부 데이터 형식의 시간·수치 표현 정규화

## 실행과 테스트

```bash
./gradlew test build
```

Docker가 실행 중인 환경에서는 실제 MySQL 호환성 테스트도 실행할 수 있습니다.

```bash
./gradlew mysqlTest
```

- [회원가입 기능](docs/features/member-registration.md)
- [공통 오류 처리](docs/common/error-handling.md)
- [JWT 인증 기반](docs/features/authentication.md)
- [현재 데이터 모델](docs/data-model.md)
- [테스트 전략](docs/testing.md)
