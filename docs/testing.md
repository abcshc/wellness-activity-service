# 테스트 전략

## 테스트 계층

| 계층 | 목적 | 실행 명령 |
| --- | --- | --- |
| 단위 테스트 | 서비스 규칙과 입력 검증 규칙을 빠르게 확인 | `./gradlew test` |
| MySQL 통합 테스트 | Spring 컨텍스트, JPA 매핑, MockMvc API 계약, Flyway 마이그레이션을 실제 MySQL에서 확인 | `./gradlew integrationTest` |

기본 `test` 작업은 `src/test`의 단위 테스트만 실행합니다. `integrationTest` 작업은 `src/integrationTest`의 테스트를 실행하며, Testcontainers가 `mysql:8.4.8` 컨테이너를 시작하고 종료합니다. `build` 작업은 두 테스트 계층을 모두 실행합니다.

## MySQL 제약조건 검증

회원 이메일의 유니크 제약조건은 `uk_members_email`로 명명합니다. MySQL 호환성 통합 테스트는 실제 중복 저장 시 Hibernate 예외 원인에 전달되는 제약조건명이 `members.uk_members_email` 형식임을 확인합니다.

서비스는 테이블 접두사를 제외한 제약조건명이 `uk_members_email`인 경우에만 중복 이메일 오류로 변환합니다. 다른 데이터 무결성 오류는 중복 이메일 오류로 처리하지 않습니다.

## 실행 전제 조건

- Java 17
- Docker Desktop 또는 Testcontainers가 접근 가능한 Docker 호환 런타임

처음 `integrationTest`를 실행하면 MySQL 이미지를 내려받기 때문에 단위 테스트보다 시간이 더 걸릴 수 있습니다.
