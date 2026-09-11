ALTER TABLE members ADD UNIQUE KEY uk_members_nickname (nickname);

ALTER TABLE stores ADD KEY idx_stores_name (name);

ALTER TABLE congestion_votes DROP INDEX idx_congestion_store_voted_at;
ALTER TABLE congestion_votes ADD KEY idx_congestion_store_voted_at_level (store_id, voted_at, level);

ALTER TABLE live_talk_messages ADD KEY idx_talk_deleted_created (deleted_at, created_at);
