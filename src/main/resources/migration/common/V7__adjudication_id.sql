ALTER TABLE additional_days_awarded ALTER COLUMN adjudication_id TYPE INTEGER USING adjudication_id::integer;
