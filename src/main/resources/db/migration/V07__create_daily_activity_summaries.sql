create table daily_activity_summaries (
    id bigint not null auto_increment comment '일별 활동 집계의 내부 식별자',
    member_activity_key_id bigint not null comment '집계가 속한 회원별 활동 키의 내부 식별자',
    activity_date date not null comment 'Asia/Seoul 기준 활동 날짜',
    steps decimal(30,20) not null comment '해당 활동일에 귀속된 걸음 수',
    distance_km decimal(30,20) not null comment '해당 활동일에 귀속된 이동 거리(km)',
    source_calories_kcal decimal(30,20) not null comment '원천 데이터에서 제공한 해당 활동일의 소모 칼로리(kcal)',
    estimated_calories_kcal decimal(30,20) not null comment '걸음 수 fallback으로 계산한 해당 활동일의 참고 칼로리(kcal)',
    primary key (id),
    constraint fk_daily_activity_summaries_member_activity_key
        foreign key (member_activity_key_id) references member_activity_keys (id),
    constraint uk_daily_activity_summaries_key_date
        unique (member_activity_key_id, activity_date)
) comment = '원본 걸음수 이벤트로부터 계산한 KST 일별 활동 조회용 집계';
