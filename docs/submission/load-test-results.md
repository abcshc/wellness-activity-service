# 건강활동 업로드 검증 방법

## 목적

활동 업로드는 한 요청에 여러 원본 이벤트를 받을 수 있으므로, 저장 결과의 정확성과 동시 재전송 상황의 정합성을 함께 확인합니다. 이 문서는 특정 장비의 응답 시간이나 운영 처리량을 주장하지 않습니다. 대신 현재 API가 지원하는 입력 범위와 이를 재현하는 자동화 검증을 설명합니다.

## 지원하는 입력 범위

- 일반 동기화 요청은 활동 항목을 최대 **1,000개**까지 받습니다.
- 입력 항목은 최대 200개씩 batch 저장합니다.
- 같은 활동 이벤트를 다시 보내도 유니크 제약조건과 `INSERT IGNORE`로 원본과 일별 집계가 다시 증가하지 않습니다.
- 대량의 과거 데이터도 1,000개 이하 묶음으로 시간순 전송하고, 실패한 묶음은 같은 입력으로 재시도할 수 있습니다.

## 무엇을 나누어 검증하는가

| 검증 | 방법 | 확인하는 내용 |
| --- | --- | --- |
| 요청 크기 | Testcontainers MySQL 통합 테스트 | 100·500·1,000개 항목의 저장 건수와 API 응답 계약 |
| 청크 경계 | Testcontainers MySQL 통합 테스트 | 200건 청크의 앞뒤인 101·201건에서도 누락·중복이 없는지 |
| 재전송 | Testcontainers MySQL 통합 테스트 | 같은 입력이 원본 이벤트와 일별 집계에 한 번만 반영되는지 |
| 동시 업로드 | Testcontainers MySQL 통합 테스트 | 같은 활동 키·날짜의 동시 저장이 하나의 일별 집계 행에 원자적으로 합산되는지 |
| API 전체 흐름 | MockMvc + Testcontainers MySQL | 원본 저장부터 KST Daily·Monthly 조회까지 결과가 일관되는지 |

이 저장소의 자동화 통합 테스트는 특정 동시 사용자 수나 TPS를 보장하는 성능 시험이 아니라, 동시 요청에도 저장 결과가 정확한지를 검증합니다.

## 관련 자동화 검증

- 요청 크기·청크 경계·재전송: `ActivityUploadRequestSizeIntegrationTest`, `ActivityUploadBatchStorageContractIntegrationTest`
- JDBC batch의 신규·중복 항목 구분: `StepRecordBatchRepositoryIntegrationTest`
- 원본 저장과 일별 집계의 transaction·rollback: `ActivityUploadServiceIntegrationTest`, `ActivityUploadTransactionIntegrationTest`
- 동시 업로드와 일별 집계 정합성: `ActivityUploadConcurrencyIntegrationTest`
- 원본 입력·재전송·KST Daily·Monthly 조회 흐름: `ActivityWorkflowIntegrationTest`

Docker가 실행 중인 환경에서 다음 명령으로 검증을 재현할 수 있습니다.

```bash
./gradlew integrationTest
```

각 테스트의 책임과 Testcontainers MySQL 실행 방식은 [테스트 전략](../testing.md)에서 확인할 수 있습니다.
