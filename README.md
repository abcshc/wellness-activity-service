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

JWT 서명 정보는 환경변수로 제공합니다. 아래 예시는 로컬 실행용이며, 생성한 값은 저장소에 포함하지 않습니다.

```bash
export APP_SECURITY_JWT_ISSUER=wellness-activity-service
export APP_SECURITY_JWT_SECRET="$(openssl rand -base64 32)"
```

로컬 실행은 Docker Compose의 MySQL을 사용합니다. MySQL 데이터는 named volume에 유지되며, 애플리케이션 시작 시 Flyway가 스키마를 적용하고 Hibernate는 매핑만 검증합니다.

```bash
docker compose up -d mysql
./gradlew bootRun
```

컨테이너는 다음 명령으로 중지합니다. 데이터까지 초기화하려면 `-v` 옵션을 추가합니다.

```bash
docker compose down
```

```bash
./gradlew test
```

Docker가 실행 중인 환경에서는 Testcontainers 기반 MySQL 통합 테스트를 실행할 수 있습니다.

```bash
./gradlew integrationTest
```

`./gradlew build`는 단위 테스트와 통합 테스트를 모두 실행합니다.

- [회원가입 기능](docs/features/member-registration.md)
- [공통 오류 처리](docs/common/error-handling.md)
- [JWT 인증 기반](docs/features/authentication.md)
- [현재 데이터 모델](docs/data-model.md)
- [데이터베이스 마이그레이션](docs/database-migration.md)
- [테스트 전략](docs/testing.md)
