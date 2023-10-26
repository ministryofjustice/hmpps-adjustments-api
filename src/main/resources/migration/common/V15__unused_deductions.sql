ALTER TABLE adjustment ADD COLUMN effective_days integer NULL;
UPDATE adjustment SET effective_days = days_calculated;
ALTER TABLE adjustment ALTER COLUMN effective_days integer NOT NULL;

ALTER TABLE adjustment ALTER COLUMN days_calculated integer NULL;


