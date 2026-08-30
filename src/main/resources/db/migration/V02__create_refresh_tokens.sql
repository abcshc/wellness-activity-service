-- 원문 Refresh Token 대신 SHA-256 해시와 회전 이력을 저장한다.
create table refresh_tokens (
    id bigint not null auto_increment,
    member_id bigint not null,
    token_hash char(64) not null,
    -- 회전으로 파생된 토큰을 함께 폐기하고 재사용을 감지하는 로그인 단위다.
    family_id char(36) not null,
    status varchar(10) not null,
    issued_at datetime(6) not null,
    expires_at datetime(6) not null,
    -- 회전 후 새로 발급한 토큰을 가리켜 이전 토큰의 이력을 보존한다.
    replaced_by_token_id bigint null,
    primary key (id),
    constraint uk_refresh_tokens_token_hash unique (token_hash),
    constraint fk_refresh_tokens_member
        foreign key (member_id) references members (id),
    constraint fk_refresh_tokens_replaced_by
        foreign key (replaced_by_token_id) references refresh_tokens (id),
    constraint ck_refresh_tokens_status
        check (status in ('ACTIVE', 'ROTATED', 'REVOKED')),
    index idx_refresh_tokens_member_status (member_id, status),
    index idx_refresh_tokens_family_status (family_id, status),
    index idx_refresh_tokens_status_expires_at (status, expires_at)
);
