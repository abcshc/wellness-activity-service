# 애플리케이션과 분리해 SQL 마이그레이션만 실행하는 전용 이미지다.
# Spring Boot 의존성과 애플리케이션 설정을 포함하지 않는다.
FROM flyway/flyway:12.4.0

COPY src/main/resources/db/migration/ /flyway/sql/

# 기본 sql 디렉터리 스캔 변경에 영향받지 않도록 마이그레이션 위치를 명시한다.
ENV FLYWAY_LOCATIONS=filesystem:/flyway/sql
