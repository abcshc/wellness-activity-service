# 건강활동 데이터

> 상태: 입력 정규화·업로드·일별·월별 조회 API 구현

## 목적

외부 건강 플랫폼에서 전달된 걸음 수 활동 데이터를 저장하고, 인증된 회원이 자신의 일별·월별 활동 요약을 조회할 수 있도록 합니다.

## 입력 범위

제공된 JSON의 `recordkey`, `data.source`, `data.entries` 구조와 각 활동 항목의 `period`, `steps`, `calories`, `distance` 값을 수용합니다. 입력 JSON의 필드나 값을 변경하거나 시간대 필드를 추가로 요구하지 않습니다.

제공 JSON의 최상위 `type: "steps"`는 걸음수 전용 endpoint·테이블에서 이미 표현되는 중복 정보입니다. 기존 입력과의 호환을 위해 수용하되, 필수값 검증·저장·중복 판정에는 사용하지 않습니다.

`source.name`은 서버 내부의 제한된 provider 값으로 정규화합니다.

| 입력 값 | provider |
| --- | --- |
| `SamsungHealth` | `SAMSUNG_HEALTH` |
| `Health Kit` | `APPLE_HEALTH` |
| `HealthConnect` | `HEALTH_CONNECT` |

## 입력 검증

- `recordkey`, provider, 활동 시작·종료 시각, `steps`, `calories`, `distance`와 각 단위는 필수입니다.
- `distance`는 `km`, `calories`는 `kcal`만 수용하며, 측정값은 음수일 수 없습니다.
- 활동 종료 시각은 시작 시각과 같거나 이후여야 합니다. 제공 데이터에 시작·종료 시각이 같은 활동 구간이 있으므로 같은 시각은 허용합니다.
- 측정값은 `BigDecimal`으로 해석하고, 데이터베이스에는 `DECIMAL(30,20)`으로 저장합니다. 입력·저장·집계에서는 소수점을 반올림하거나 절삭하지 않으며, 표시 단위의 반올림은 API 소비자에서 결정합니다.
- 최상위 `recordkey`, `data.source`처럼 묶음 전체의 해석에 필요한 값이 잘못되면 요청 전체를 거부합니다. 개별 활동 항목의 검증 오류는 해당 항목만 제외하고 나머지 유효 항목을 저장합니다.
- 일반 동기화 요청의 `data.entries`는 최대 1,000개입니다. 이를 초과하면 `ACTIVITY_ENTRIES_TOO_MANY` 오류로 요청 전체를 거부합니다.
- 항목 오류에는 원본 배열의 0부터 시작하는 인덱스, 필드 경로, 오류 코드와 메시지를 보존합니다. 현재는 한 항목에서 처음 발견한 오류를 반환합니다.

## 업로드 크기와 과거 이관

일반 동기화 endpoint는 한 요청에 최대 1,000개 항목을 수용합니다. 이 범위는 200건 청크 저장과 MySQL 통합 테스트, 1,000건 동시 업로드 탐색으로 검증했습니다.

과거 전체 데이터를 보낼 때도 별도 이관 API는 사용하지 않습니다. 클라이언트가 시간순으로 1,000건 이하 묶음을 순차 전송하고, 실패한 묶음은 같은 입력으로 재시도합니다. 유니크 제약조건과 `INSERT IGNORE`가 재전송된 원본 이벤트를 무시하므로, 체크포인트 없이도 안전하게 이어서 전송할 수 있습니다.

## 시간 정규화와 집계 기준

- 오프셋을 포함한 일시는 포함된 오프셋으로 해석합니다.
- 오프셋이 없는 일시는 UTC로 해석합니다. 이는 Samsung 원본 SDK의 시간대 모델을 단정하는 것이 아니라, `+0000` 표기를 사용하는 제공 JSON의 일관된 해석 규칙입니다.
- 두 형식 모두 UTC `Instant`로 정규화해 저장합니다.
- Daily·Monthly의 업무상 날짜 경계는 국내 서비스 기준인 `Asia/Seoul`입니다.
- 서버는 한국 날짜 범위를 UTC 반열린 구간(`[start, end)`)으로 변환해 조회합니다. 예를 들어 한국 시간 `2024-11-15`는 UTC `[2024-11-14T15:00:00Z, 2024-11-15T15:00:00Z)`입니다.

이 정책은 저장·중복 판정에는 절대 시각을 사용하고, 사용자에게 제공하는 활동 요약에는 한국의 업무 날짜를 적용하기 위함입니다.

## 집계 방식

일별·월별 집계 서비스는 원본 활동 이벤트를 직접 합산해 계산합니다. 현재 예상 데이터량에서는 일별 집계 결과를 별도 테이블에 저장하거나 Redis 캐시를 유지하지 않습니다.

`steps`, `distance`, `calories` 원본값은 변형하지 않고 보존합니다. 칼로리의 사용자 제공 집계값은 원천값과 구분해 관리하며, 추정값이 원천값을 덮어쓰지 않습니다.

### 활동 칼로리 fallback

원천 `calories`가 양수인 활동은 원천값만 사용합니다. 원천값이 `0`이고 걸음 수가 양수인 일상 걸음 활동은 사용자에게 참고용 활동량을 제공하기 위해 다음 단순 fallback을 적용합니다.

```text
estimatedCaloriesKcal = steps × 0.04
caloriesKcal = sourceCaloriesKcal + estimatedCaloriesKcal
```

원본 `calories`는 `calories_kcal`, fallback 값은 `estimated_calories_kcal`, 적용 규칙은 `calories_estimate_version`으로 원본 이벤트에 함께 저장합니다. API는 내부 구성값을 노출하지 않고 두 값을 합산한 `caloriesKcal`만 반환합니다.

`0.04 kcal/걸음`은 개인별 에너지 소비량이 아닌 보수적인 단일 참고 계수입니다. 통근 보행 연구에서 휴식 에너지를 제외한 걸음당 에너지는 여성 약 `0.0394 kcal`, 남성 약 `0.0532 kcal`로 보고되었습니다. 제공된 SamsungHealth 검증 입력을 원천값으로 역산한 결과도 약 `0.0353`~`0.0401 kcal/걸음`입니다. [통근 보행 연구](https://pmc.ncbi.nlm.nih.gov/articles/PMC9635924/)

현재 입력은 일반 활동의 걸음 수를 10분 단위로 집계한 데이터이므로, 체중·실제 연속 보행 시간·속도·경사 같은 복잡한 조건은 fallback에 포함하지 않습니다. 따라서 계단·등산·달리기·운동 세션을 구분하거나, 의료·영양 처방 또는 개인별 정확한 에너지 소비량을 산출하는 기능으로 사용하지 않습니다. 실제 연동에서는 HealthKit의 [`activeEnergyBurned`](https://developer.apple.com/documentation/healthkit/hkquantitytypeidentifier/activeenergyburned)나 Health Connect의 [`ActiveCaloriesBurnedRecord`](https://developer.android.com/reference/androidx/health/connect/client/records/ActiveCaloriesBurnedRecord)처럼 원천이 제공하는 활동 에너지를 우선합니다.

서버는 데이터 스트림과 UTC 시간 범위를 조건으로 사용하므로, 날짜별·월별 조회에서 활동 시각 열에 시간대 변환 함수를 적용하지 않습니다. 실제 조회량에서 병목이 확인될 때만 파생 집계 또는 캐시를 검토합니다.

한국 시간의 일자 또는 월 경계를 넘는 활동 구간은 겹치는 시간 비율대로 `steps`, `calories`, `distance`를 나눠 각 결과 기간에 반영합니다. 시작·종료 시각이 같은 구간은 나눌 수 없으므로 시작일에 전량 반영합니다. 배분 계산은 원본을 변경하지 않고 조회 시 수행하며, 전체 구간을 조회하면 각 배분값의 합계가 원본 측정값과 같도록 마지막 구간에 소수점 잔여값을 반영합니다.

## 사용자 구분 키와 중복 수집

`recordkey`는 과제 입력에서 사전 제공되는 사용자 구분 키입니다. 서버는 이를 발행·재발급·복구하지 않고 저장·집계·조회 기준으로 사용합니다. provider는 활동이 수집된 원천을 나타내는 속성입니다. 원천별 테이블을 만들지 않고 공통 활동 모델에 provider를 보존합니다.

- 사용자 연결: `recordkey` 하나는 하나의 회원에게만 연결합니다. 회원이 여러 `recordkey`를 가질 수 있는지와 키 수명주기는 현재 제한하지 않습니다.
- `recordkey`와 provider의 발급·종속 관계는 과제에서 정의하지 않습니다. provider를 `recordkey`의 일부로 가정하지 않습니다.
- 활동 이벤트: 같은 `recordkey`, provider 안에서 정규화된 시작·종료 시각이 모두 같으면 같은 이벤트로 봅니다.
- 동일 JSON의 재업로드와 같은 이벤트의 재전송은 성공으로 처리하며, 이미 저장된 이벤트는 그대로 유지하고 일·월 집계에 다시 반영하지 않습니다.
- 같은 이벤트 식별값으로 다른 측정값이 들어와도 현재 범위에서는 먼저 저장된 원본을 유지하고, 후속 입력은 재전송으로 간주해 반영하지 않습니다. 원천 이벤트의 갱신·버전 정보가 제공될 때만 별도 정정 정책을 검토합니다.
- 서로 다른 provider에서 같은 시간대에 수집된 활동은 원본 이벤트 ID 없이 같은 활동인지 안전하게 판단할 수 없으므로, 현재 범위에서는 자동 병합하지 않습니다.

구현에서는 `recordkey`를 회원별 활동 키로 연결합니다. 활동 이벤트의 중복 저장은 다음 데이터베이스 유니크 제약조건으로 막고, 동일 이벤트의 최초 원본 유지 정책은 업로드 서비스에서 처리합니다.

```text
UNIQUE (member_activity_key_id, provider, started_at_utc, ended_at_utc)
```

`member_activity_key_id`는 `recordkey`를 참조하는 내부 식별자입니다. provider·`recordkey`·기간을 이어 붙인 문자열 키나 별도 해시 키는 저장하지 않습니다. 구조화된 값은 각각의 컬럼으로 유지하고, 코드에서는 필요할 때만 하나의 식별 값으로 묶어 사용합니다.

`recordkey`와 원본 이벤트 식별값은 데이터베이스 유니크 제약조건으로 보호합니다. 업로드 서비스는 최대 200개 항목씩 MySQL multi-row `INSERT IGNORE`를 실행해 신규 저장과 동시 재전송을 같은 흐름으로 처리하며, 이미 존재하는 이벤트는 오류 없이 무시합니다. 이 방식은 여러 애플리케이션 인스턴스에서도 동일하게 동작합니다.

청크 크기는 SQL 파라미터 수와 트랜잭션 시간을 제한하기 위한 구현 상수입니다. 각 값은 바인드 파라미터로 전달하며, 입력값을 SQL 문자열에 직접 연결하지 않습니다. 청크 크기는 성능 재측정 결과에 따라 조정할 수 있지만, `createdCount`·`ignoredCount`와 유니크 제약조건 기반 멱등성 계약은 유지합니다.

업로드 시 마지막으로 처리한 시각만 저장하는 체크포인트는 중복 방지 기준으로 사용하지 않습니다. 모바일 데이터는 지연 도착하거나 같은 구간이 재전송될 수 있기 때문입니다. 서버는 유니크 제약조건과 `INSERT IGNORE`로 재전송을 무시합니다.

제공된 SamsungHealth 입력에는 자정의 시작·종료 시각이 같은 구간이 15건 존재합니다. 이 값은 시간 구간 데이터로서는 품질 이슈이지만, 원본에 전날 귀속 근거나 원천 이벤트 ID가 없으므로 삭제·이동하지 않습니다. 원본을 저장하고, 일·월 집계에서는 시작 시각이 속한 한국 날짜에 전량 반영합니다.

## 권한

- 활동 업로드와 조회는 JWT 인증이 필요합니다.
- `recordkey`는 최초 업로드 시 인증된 회원과 연결해 접근 권한을 확인합니다.
- 이미 다른 회원에게 연결된 `recordkey`는 사용할 수 없습니다.
- 회원은 자신에게 연결된 `recordkey`의 활동만 조회할 수 있습니다.

## 제공 기능

| 기능 | 설명 |
| --- | --- |
| 활동 데이터 업로드 | 입력 JSON의 활동 항목을 검증·정규화·저장합니다. |
| 일별 조회 | `recordkey` 기준으로 한국 시간 날짜의 `steps`, `calories`, `distance`를 합산합니다. |
| 월별 조회 | `recordkey` 기준으로 한국 시간 월의 활동을 합산합니다. |

## 조회 계약

- `GET /api/v1/activities/steps/daily`와 `GET /api/v1/activities/steps/monthly`는 JWT 인증이 필요합니다.
- Daily 조회는 `from`·`to`의 한국 날짜(`YYYY-MM-DD`) 범위를 양 끝 포함으로 받습니다.
- Monthly 조회는 `from`·`to`의 한국 연월(`YYYY-MM`) 범위를 양 끝 포함으로 받습니다.
- `recordkey`, `from`, `to`는 모두 필수이며, Daily는 최대 366일, Monthly는 최대 24개월까지 조회할 수 있습니다.
- 요청 범위의 모든 날짜·월을 순서대로 반환하며, 활동이 없는 항목의 `steps`, `calories`, `distance`는 `0`입니다.
- 각 결과에는 조회 기준인 `recordkey`를 포함합니다.
- `caloriesKcal`은 원천값과 저장된 추정값을 합산한 사용자 표시용 활동 칼로리입니다. 원천·추정 구성값과 적용 규칙 버전은 데이터베이스에만 보관합니다.
- 존재하지 않거나 인증 회원이 소유하지 않은 `recordkey`는 같은 `403 Forbidden` 오류로 처리합니다.

### Daily 예시

```http
GET /api/v1/activities/steps/daily?recordkey=record-key-001&from=2024-11-14&to=2024-11-15
Authorization: Bearer <access-token>
```

```json
[
  {
    "recordkey": "record-key-001",
    "date": "2024-11-14",
    "steps": 0,
    "distanceKm": 0,
    "caloriesKcal": 0
  }
]
```

원천 칼로리가 `0`인 100걸음의 응답은 다음과 같습니다.

```json
{
  "caloriesKcal": 4
}
```

Monthly 응답은 `date` 대신 `month`(`YYYY-MM`)를 사용하며, 나머지 필드는 동일합니다.

## 자정 경계와 재업로드 예시

이 예시는 활동 구간이 자정을 넘을 때 일별 값이 어떻게 나뉘는지, 같은 데이터를 다시 보내도 합계가 늘어나지 않는지를 보여줍니다. 실제 사용자 데이터 대신 테스트 전용 데이터를 사용합니다.

`recordkey=record-key-001`에 다음 활동을 업로드합니다.

| 활동 | 기간(KST) | 값 | 처리 방식 |
| --- | --- | --- | --- |
| A | 2024-11-14 23:30 ~ 2024-11-15 00:30 | 100 steps, 2km, 10kcal | 한 시간 중 날짜별로 30분씩이므로 두 날짜에 절반씩 나눕니다. |
| B | 2024-11-15 00:00 ~ 00:00 | 12 steps, 0.12km, 1.2kcal | 기간이 0초이므로 시작 날짜인 15일에 전부 넣습니다. |
| C | 2024-11-14 23:30 ~ 23:40 | 20 steps, 0.2km, 1kcal | A와 시작 시각은 같지만 종료 시각이 달라 별도의 활동으로 저장합니다. |
| 잘못된 항목 | 종료 시각이 시작 시각보다 이릅니다 | - | 이 항목만 저장하지 않고, 나머지 활동은 정상 처리합니다. |

Daily 조회 결과는 다음과 같습니다.

| 날짜(KST) | steps | distanceKm | caloriesKcal |
| --- | ---: | ---: | ---: |
| 2024-11-14 | 70 | 1.2 | 6 |
| 2024-11-15 | 62 | 1.12 | 6.2 |

예를 들어 14일의 `70 steps`는 A의 절반인 50과 C의 20을 더한 값입니다. 15일의 `62 steps`는 A의 나머지 50과 B의 12를 더한 값입니다.

두 날짜를 합친 2024년 11월 Monthly 결과는 `steps=132`, `distanceKm=2.32`, `caloriesKcal=12.2`입니다. 동일 JSON을 다시 업로드하면 이미 저장된 세 활동은 무시되므로 Daily·Monthly 결과가 변하지 않습니다.

이 동작은 [건강활동 전체 흐름 통합 테스트](../../src/integrationTest/java/io/github/abcshc/wellnessactivity/activity/ActivityWorkflowIntegrationTest.java)에서 검증합니다.

## 업로드 결과 계약

### `POST /api/v1/activities/steps`

`Authorization: Bearer <access-token>`이 필요합니다. 인증 회원은 요청 본문이 아니라 Access Token의 주체로 결정됩니다.

```http
POST /api/v1/activities/steps
Authorization: Bearer <access-token>
Content-Type: application/json
```

```json
{
  "recordkey": "record-key-001",
  "data": {
    "source": {"name": "SamsungHealth"},
    "entries": [
      {
        "period": {"from": "2024-11-14T23:30:00+0900", "to": "2024-11-15T00:30:00+0900"},
        "steps": 100,
        "distance": {"unit": "km", "value": 2},
        "calories": {"unit": "kcal", "value": 10}
      }
    ]
  }
}
```

최상위 값이 유효한 업로드는 `200 OK`로 처리하고, 항목별 처리 결과를 함께 반환합니다.

```json
{
  "totalCount": 100,
  "createdCount": 92,
  "ignoredCount": 5,
  "invalidCount": 3,
  "invalidEntries": [
    {
      "index": 17,
      "field": "period.to",
      "code": "ACTIVITY_INVALID_PERIOD",
      "message": "종료 시각은 시작 시각보다 이를 수 없습니다."
    }
  ]
}
```

- `createdCount`: 새로 저장된 활동 수
- `ignoredCount`: 이미 저장된 활동이라 원본을 유지한 채 무시한 수
- `invalidCount`: 개별 항목 검증에 실패해 저장하지 않은 수
- `invalidEntries`: 실패 항목의 0부터 시작하는 입력 순번과 오류 정보

묶음 전체를 해석할 수 없는 최상위 입력 오류와 JSON 본문 해석 오류는 `400 Bad Request` 공통 오류 응답으로 처리합니다. 인증되지 않은 요청은 `401 Unauthorized`, 다른 회원에게 이미 연결된 `recordkey`는 `403 Forbidden` 공통 오류 응답으로 처리합니다.
