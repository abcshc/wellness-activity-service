-- 기존 인증 스키마에 데이터 의미를 확인할 수 있는 주석을 추가한다.
alter table members
    comment = '서비스 회원의 식별 정보와 로그인 비밀번호 해시';

alter table members
    modify column id bigint not null auto_increment comment '회원의 내부 식별자',
    modify column name varchar(100) not null comment '회원 이름',
    modify column nickname varchar(30) not null comment '서비스 표시 이름',
    modify column email varchar(254) not null comment '로그인에 사용하는 이메일 주소',
    modify column password_hash varchar(60) not null comment 'BCrypt로 해시한 비밀번호';

alter table refresh_tokens
    comment = '로그인 유지와 토큰 회전 이력을 위한 Refresh Token 메타데이터';

alter table refresh_tokens
    modify column id bigint not null auto_increment comment 'Refresh Token의 내부 식별자',
    modify column member_id bigint not null comment '토큰을 발급받은 회원의 내부 식별자',
    modify column token_hash char(64) not null comment 'SHA-256으로 해시한 Refresh Token',
    modify column family_id char(36) not null comment '로그인과 토큰 회전 계열을 구분하는 식별자',
    modify column status varchar(10) not null comment '토큰 상태: ACTIVE, ROTATED, REVOKED',
    modify column issued_at datetime(6) not null comment '토큰 발급 또는 회전 시각(UTC)',
    modify column expires_at datetime(6) not null comment '토큰 만료 시각(UTC)',
    modify column replaced_by_token_id bigint null comment '회전으로 발급된 후속 토큰의 내부 식별자';
