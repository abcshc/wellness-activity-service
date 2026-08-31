-- recordkey는 provider와 독립적인 회원별 활동 키로 관리한다.
create table member_activity_keys (
    id bigint not null auto_increment comment '회원별 활동 키의 내부 식별자',
    member_id bigint not null comment '활동 키를 소유한 회원의 내부 식별자',
    record_key varchar(255) not null comment '외부 입력에서 전달되는 사용자 구분 키',
    primary key (id),
    constraint fk_member_activity_keys_member
        foreign key (member_id) references members (id),
    index idx_member_activity_keys_member_id (member_id),
    index idx_member_activity_keys_record_key (record_key)
) comment = '회원과 외부 건강활동 recordkey의 연결 정보';

-- 원천 걸음수 이벤트를 그대로 저장한다. 중복 후보 조회와 기간 조회는 인덱스를 사용한다.
create table step_records (
	 id bigint not null auto_increment comment '원본 걸음수 이벤트의 내부 식별자',
	 member_activity_key_id bigint not null comment '걸음수 이벤트가 속한 회원별 활동 키',
    provider varchar(20) not null comment '활동 데이터 수집 원천',
    started_at_utc datetime(6) not null comment '활동 시작 시각(UTC)',
    ended_at_utc datetime(6) not null comment '활동 종료 시각(UTC)',
    steps decimal(30,20) not null comment '원천 걸음 수',
    distance_km decimal(30,20) not null comment '원천 이동 거리(km)',
    calories_kcal decimal(30,20) not null comment '원천 소모 칼로리(kcal)',
    primary key (id),
	constraint fk_step_records_member_activity_key
        foreign key (member_activity_key_id) references member_activity_keys (id),
	index idx_step_records_key_provider_period
        (member_activity_key_id, provider, started_at_utc, ended_at_utc),
	index idx_step_records_key_started_at_utc
        (member_activity_key_id, started_at_utc)
) comment = '외부 건강 플랫폼에서 전달된 걸음수 원본 이벤트';
