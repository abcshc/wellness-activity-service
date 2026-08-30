-- 회원 식별 정보와 인증에 필요한 비밀번호 해시를 저장한다.
create table members (
    id bigint not null auto_increment,
    name varchar(100) not null,
    nickname varchar(30) not null,
    email varchar(254) not null,
    password_hash varchar(60) not null,
    primary key (id),
    -- 동시 회원가입 요청도 데이터베이스에서 최종적으로 중복 저장을 막는다.
    constraint uk_members_email unique (email)
);
