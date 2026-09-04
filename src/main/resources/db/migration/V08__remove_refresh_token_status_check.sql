alter table refresh_tokens
    drop check ck_refresh_tokens_status,
    modify column status varchar(10) not null comment 'Refresh Token 상태';
