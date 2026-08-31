-- 활동 업로드의 멱등성과 recordkey 소유권을 데이터베이스에서 보장한다.
alter table member_activity_keys
    drop index idx_member_activity_keys_record_key,
    add constraint uk_member_activity_keys_record_key unique (record_key);

alter table step_records
    drop index idx_step_records_key_provider_period,
    add constraint uk_step_records_key_provider_period
        unique (member_activity_key_id, provider, started_at_utc, ended_at_utc);
