alter table step_records
    add column estimated_calories_kcal decimal(30,20) not null default 0
        comment '걸음수 fallback으로 계산한 참고용 칼로리(kcal)',
    add column calories_estimate_version varchar(30) null
        comment '칼로리 추정 규칙 버전';
