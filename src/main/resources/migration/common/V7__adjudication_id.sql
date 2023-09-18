ALTER TABLE additional_days_awarded ALTER COLUMN adjudication_id TYPE BIGINT USING adjudication_id::BIGINT;
