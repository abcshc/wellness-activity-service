# 테스트 전략

## 검증 흐름

건강활동 기능은 작은 규칙부터 실제 MySQL 저장까지, 범위를 조금씩 넓혀가며 확인합니다. 아래 순서를 보면 각 테스트가 무엇을 책임지는지 쉽게 알 수 있습니다.

1. 단위 테스트로 입력 정규화, 수치 처리, 자정 경계 집계 규칙을 검증한다.
2. Testcontainers MySQL 통합 테스트로 유니크 제약조건, 멱등 저장, 인증·권한을 검증한다.
3. 실제 API 전체 흐름에서 원본 입력 저장, 재전송, Daily·Monthly 집계 결과를 검증한다.
4. 요청 크기와 동시 업로드의 탐색 결과를 [건강활동 업로드 부하 탐색 결과](submission/load-test-results.md)에 기록한다.

## 테스트 계층

| 계층 | 목적 | 실행 명령 |
| --- | --- | --- |
| 단위 테스트 | 서비스 규칙과 입력 검증 규칙을 빠르게 확인 | `./gradlew test` |
| MySQL 통합 테스트 | Spring 컨텍스트, JPA 매핑, MockMvc API 계약, Flyway 마이그레이션을 실제 MySQL에서 확인 | `./gradlew integrationTest` |

기본 `test` 작업은 `src/test`의 단위 테스트만 실행합니다. `integrationTest` 작업은 `src/integrationTest`의 테스트를 실행하며, Testcontainers가 `mysql:8.4.8` 컨테이너를 시작하고 종료합니다. `build` 작업은 두 테스트 계층을 모두 실행합니다.

## 요청 크기별 활동 업로드 검증

`ActivityUploadRequestSizeIntegrationTest`는 “한 번에 큰 요청이 와도 정확히 저장되는가?”를 확인합니다. 실제 MySQL에서 100·500·1,000개 항목을 각각 업로드하고, SamsungHealth·Health Kit·HealthConnect의 시간 표현도 함께 확인합니다. 응답의 `createdCount`와 실제 저장 행 수가 요청 항목 수와 같아야 통과합니다.

### 이 테스트가 실행되는 방식

이 테스트는 한 테스트 메서드가 세 입력 크기를 **차례대로** 실행하는 파라미터화 테스트입니다. 여러 사용자가 동시에 접속하는 상황이나 초당 처리량을 재는 테스트는 아닙니다. 대신 큰 요청도 현재 API 계약대로 정확히 저장되는지를 확인합니다.

`MySqlTestContainerConfiguration`은 테스트 자체가 아니라, 통합 테스트가 사용할 MySQL 실행 환경을 마련해 주는 공용 설정입니다. 각 통합 테스트가 이 설정을 `@Import`하면 다음 순서로 동작합니다.

1. Testcontainers가 Docker에서 `mysql:8.4.8` 컨테이너를 시작한다.
2. `@ServiceConnection`이 컨테이너의 접속 정보를 Spring Boot DataSource에 연결한다.
3. Flyway가 실제 MySQL에 마이그레이션을 적용하고, Hibernate가 매핑을 검증한다.
4. `MockMvc`가 같은 JVM의 컨트롤러를 호출하고, 저장 결과를 실제 MySQL에서 확인한다.

즉, 이 테스트는 “애플리케이션과 실제 MySQL이 함께 잘 동작하는가?”를 확인하는 통합 테스트입니다. 다만 외부 HTTP 서버에 요청을 보내거나, 여러 사용자를 동시에 만드는 부하 테스트는 아닙니다.

## 배치 저장 결과 계약

`ActivityUploadBatchStorageContractIntegrationTest`는 청크 단위 batch 저장 구현에서도 바뀌면 안 되는 결과를 MySQL에서 고정합니다. 이 테스트는 성능을 측정하는 테스트가 아닙니다. 저장 구현을 조정해도 아래 결과가 유지되는지 확인하는 안전망입니다.

- 100·101·200·201·1,000건의 신규 항목은 모두 생성되고, `createdCount`와 실제 저장 행 수가 같습니다.
- 동일 묶음을 다시 보내면 새 행은 생성되지 않고, 모든 항목이 `ignoredCount`로 집계됩니다.
- 이미 저장된 항목과 새 항목이 섞여 있으면 새 항목만 생성되며, 최종 행 수가 중복 없이 증가합니다.

101·201건도 포함하는 이유는 현재 200건 청크의 경계 바로 다음 항목이 누락되거나 중복되지 않는지 확인하기 위해서입니다.

## MySQL 제약조건 검증

회원 이메일의 유니크 제약조건은 `uk_members_email`로 명명합니다. MySQL 호환성 통합 테스트는 실제 중복 저장 시 Hibernate 예외 원인에 전달되는 제약조건명이 `members.uk_members_email` 형식임을 확인합니다.

서비스는 테이블 접두사를 제외한 제약조건명이 `uk_members_email`인 경우에만 중복 이메일 오류로 변환합니다. 다른 데이터 무결성 오류는 중복 이메일 오류로 처리하지 않습니다.

## 실행 전제 조건

- Java 17
- Docker Desktop 또는 Testcontainers가 접근 가능한 Docker 호환 런타임

처음 `integrationTest`를 실행하면 MySQL 이미지를 내려받기 때문에 단위 테스트보다 시간이 더 걸릴 수 있습니다.

## 건강활동 전체 흐름

`ActivityWorkflowIntegrationTest`는 Testcontainers MySQL에서 다음 공개 API 흐름을 검증합니다.

1. 회원가입과 로그인으로 Access Token을 발급합니다.
2. KST 자정 경계·0초 구간·시작 시각 충돌·잘못된 항목이 포함된 활동 데이터를 업로드합니다.
3. 같은 입력을 재전송해 생성 없이 무시되는지 확인합니다.
4. Daily·Monthly 결과가 독립적으로 계산한 기대값과 일치하는지 확인합니다.
5. 다른 회원이 같은 `recordkey`를 조회하면 `403 Forbidden`인지 확인합니다.

테스트는 시작과 종료 시 데이터베이스를 정리하므로 실행 순서에 의존하지 않습니다.

## 인증 회원 상태 회귀 검증

`ActivitySummaryControllerIntegrationTest`는 보호 활동 조회에서 인증 경계를 함께 확인합니다. Access Token이 유효하더라도 토큰 주체인 회원이 존재하지 않으면 `401 Unauthorized`를 반환하며, 다른 회원의 `recordkey`는 `403 Forbidden`을 반환합니다. 두 경우를 분리해 검증해 토큰 유효성과 활동 데이터 소유권의 의미가 섞이지 않도록 합니다.
