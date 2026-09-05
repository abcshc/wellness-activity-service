# wellness-activity-service

건강활동 데이터를 수집·저장하고, 사용자별 일별·월별 활동 요약을 제공하는 백엔드 서비스입니다.

## 목표

외부 건강 플랫폼에서 전달되는 활동 데이터를 표준 형식으로 정규화하여 저장하고, 사용자가 자신의 건강활동을 일별·월별로 조회할 수 있도록 합니다.

## 현재 구현 범위

- `POST /api/v1/members` 회원가입
- BCrypt 기반 비밀번호 해시 저장
- 이메일 중복 및 요청 필드 검증
- 공통 오류 응답
- JWT Bearer Token 인증
- 이메일·비밀번호 로그인, Refresh Token 회전, 로그아웃
- 활동 데이터 업로드와 항목별 부분 성공 응답
- `recordkey` 소유권 확인과 원본 이벤트 멱등 저장
- 원본 저장과 같은 트랜잭션으로 갱신하는 KST 일별 집계
- 일별 집계를 읽는 Daily·Monthly 활동 요약 조회

## 주요 설계 선택

- 원본 활동 이벤트는 재집계와 검증의 기준으로 보관하고, Daily·Monthly 조회는 KST 일별 집계를 사용합니다.
- 활동 이벤트의 재전송은 데이터베이스 유니크 제약조건으로 식별하며, 실제 신규 원본만 일별 집계에 반영합니다.
- 원본 이벤트 저장과 일별 집계 갱신은 하나의 트랜잭션으로 처리합니다.
- 현재 동기 업로드 범위에는 Redis·Kafka를 도입하지 않았습니다. 대량 이관·비동기 재집계 요구가 생기면 별도 작업 흐름으로 확장합니다.

## 설계 개선과 검증

기능을 먼저 구현한 뒤, 원본 데이터가 누적되는 상황에서 조회 비용과 재전송 정합성을 다시 검토했습니다. 개선은 원본을 버리는 방식이 아니라, 원본을 기준 데이터로 유지하면서 조회용 일별 집계를 추가하는 방향으로 진행했습니다.

| 주제 | 초기 방식 | 개선한 방식 | 확인한 내용 |
| --- | --- | --- | --- |
| Daily·Monthly 조회 | 조회 시 원본 `step_records`를 읽고 기간별로 집계했습니다. | 저장 시 KST 일별 기여값을 계산해 `daily_activity_summaries`에 누적하고, 조회는 이 집계를 읽습니다. | 최대 조회 범위와 대용량 원본 fixture에서 Daily·Monthly 응답 계약과 결과 정합성을 검증했습니다. |
| 재전송과 동시 업로드 | 원본 이벤트 중복 저장 방지는 필요했지만, 중복이 집계에 다시 반영되지 않아야 했습니다. | 원본 이벤트 유니크 제약조건과 JDBC batch의 항목별 저장 결과를 사용해 실제 신규 원본만 집계에 반영합니다. | 재전송, 200건 청크 경계, 같은 활동 키·날짜의 동시 업로드를 Testcontainers MySQL 통합 테스트로 검증했습니다. |
| 원본·집계 일관성 | 원본 저장과 조회 결과 사이의 실패 경계를 명확히 관리해야 했습니다. | 원본 저장과 일별 집계 갱신을 하나의 트랜잭션으로 처리하고, 일별 집계는 원자적 UPSERT로 누적합니다. | 저장 중 예외가 발생하면 원본과 일별 집계가 함께 rollback되는지 통합 테스트로 검증했습니다. |

현재 1,000건 이하의 동기화 요청을 지원하며, 요청 크기·재전송·동시 저장의 검증 범위는 [건강활동 업로드 검증 방법](docs/submission/load-test-results.md)에서 확인할 수 있습니다. 이 검증은 특정 장비의 처리량을 보장하는 성능 수치가 아니라, 현재 설계의 입력 한계와 정합성 조건을 재현하는 데 목적이 있습니다.

활동 데이터의 입력 정규화, 시간대·집계 기준, API 계약은 [건강활동 데이터 기능 명세](docs/features/activity-data.md)에서 확인할 수 있습니다.

비식별 검증 입력을 실제 API로 처리한 1차 결과는 [건강활동 입력 검증 결과](docs/submission/activity-results.md)에서 확인할 수 있습니다. 이 결과는 원본 저장·재전송 멱등성·KST 일별 집계·Daily·Monthly 조회 정합성을 포함합니다. 요청 크기와 동시성 검증의 범위와 실행 방법은 [건강활동 업로드 검증 방법](docs/submission/load-test-results.md)에서 확인할 수 있습니다.

## 기술 방향

- Java 17
- Spring Boot
- Spring Data JPA
- MySQL
- 기능 중심 패키지 구조
- 외부 데이터 형식의 시간·수치 표현 정규화

## 프로젝트 구조

기능별 패키지 안에 API, 서비스, 영속성 계층을 함께 둡니다. 기능을 따라가며 HTTP 계약부터 비즈니스 규칙·저장소까지 확인할 수 있도록 구성했습니다.

```text
src/main/java/.../wellnessactivity
├── member/       회원가입, 비밀번호 정책, 회원 영속성
├── auth/         로그인, JWT 인증, Refresh Token 회전·로그아웃
├── activity/     활동 입력 정규화, 원본 저장, KST 집계·조회
└── common/       공통 오류 응답, 예외, 웹 검증

src/main/resources/db/migration/  Flyway MySQL 스키마 이력
src/test/                         단위 테스트
src/integrationTest/              Testcontainers MySQL 통합 테스트
docs/                             기능 명세, ERD, 마이그레이션·검증 문서
```

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

애플리케이션과 분리해 Flyway 마이그레이션만 먼저 적용하려면 다음 명령을 사용합니다. 이후 애플리케이션은 `SPRING_FLYWAY_ENABLED=false`로 실행할 수 있습니다.

```bash
docker compose --profile migration run --rm migration
SPRING_FLYWAY_ENABLED=false ./gradlew bootRun
```

컨테이너는 다음 명령으로 중지합니다. 데이터까지 초기화하려면 `-v` 옵션을 추가합니다.

```bash
docker compose down
```

단위 테스트는 다음 명령으로 실행합니다.

```bash
./gradlew test
```

Docker가 실행 중인 환경에서는 Testcontainers 기반 MySQL 통합 테스트를 실행할 수 있습니다.

```bash
./gradlew integrationTest
```

전체 검증은 다음 명령으로 실행합니다. `build`는 단위 테스트와 통합 테스트를 모두 실행합니다.

```bash
./gradlew build
```

- [회원가입 기능](docs/features/member-registration.md)
- [공통 오류 처리](docs/common/error-handling.md)
- [JWT 인증 기반](docs/features/authentication.md)
- [건강활동 데이터 기능 명세](docs/features/activity-data.md)
- [현재 데이터 모델](docs/data-model.md)
- [데이터베이스 마이그레이션](docs/database-migration.md)
- [테스트 전략](docs/testing.md)
- [건강활동 업로드 검증 방법](docs/submission/load-test-results.md)
