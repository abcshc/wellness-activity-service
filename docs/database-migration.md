# 데이터베이스 마이그레이션

이 서비스는 Flyway SQL 마이그레이션으로만 MySQL 스키마를 관리합니다. Hibernate는 `validate` 모드로 매핑만 검증하며 테이블을 생성하거나 변경하지 않습니다.

## 마이그레이션 규칙

- 파일 위치: `src/main/resources/db/migration/`
- 파일명: `V<두 자리 순차 번호>__<설명>.sql`
- 예시: `V01__create_members.sql`
- 한 번 적용한 파일은 수정하거나 삭제하지 않습니다. 변경이 필요하면 다음 번호의 새 파일을 추가합니다.
- `clean`은 비활성화되어 있습니다.

## 로컬 실행

Docker Compose로 MySQL을 시작한 뒤 애플리케이션을 실행합니다. 애플리케이션 시작 시 아직 적용되지 않은 마이그레이션을 자동 적용합니다.

```bash
docker compose up -d mysql
./gradlew bootRun
```

`APP_SECURITY_JWT_ISSUER`, `APP_SECURITY_JWT_SECRET` 등 애플리케이션 실행에 필요한 환경변수는 별도로 제공해야 합니다. 로컬 MySQL 연결 정보는 필요에 따라 `MYSQL_JDBC_URL`, `MYSQL_USERNAME`, `MYSQL_PASSWORD`, `MYSQL_PORT`로 변경할 수 있습니다.

## 배포 방식 계획

현재 저장소에는 특정 배포 플랫폼의 Migration Job 매니페스트나 비웹 실행 구성이 포함되어 있지 않습니다. 배포 자동화를 추가할 때는 다음 흐름을 적용할 계획입니다.

1. Migration Job에 MySQL 접속 정보와 애플리케이션 필수 환경변수를 Secret 또는 환경변수로 주입합니다.
2. Migration Job이 Flyway 마이그레이션을 단일 실행합니다.
3. Migration Job이 성공한 뒤 애플리케이션 인스턴스를 `SPRING_FLYWAY_ENABLED=false`로 시작합니다.

Migration Job은 동시에 하나만 실행해야 합니다. 실제 Job 실행 방식은 Spring Security 웹 구성을 비웹 실행과 분리한 뒤 검증하여 추가합니다.

## 검증

Testcontainers 기반 통합 테스트는 실제 MySQL에서 Flyway 마이그레이션과 JPA 매핑을 검증합니다.

```bash
./gradlew integrationTest
```
