ALTER TABLE adjustment
ALTER COLUMN source TYPE varchar(50) using source::text;

ALTER TABLE adjustment_history
ALTER COLUMN change_type TYPE varchar(50) using change_type::text;

ALTER TABLE adjustment_history
ALTER COLUMN change_source TYPE varchar(50) using change_source::text;

DROP TYPE source_systems;
DROP TYPE change_types;