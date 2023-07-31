ALTER TABLE adjustment
ALTER COLUMN source TYPE varchar(50);

ALTER TABLE adjustment_history
ALTER COLUMN change_type TYPE varchar(50);

ALTER TABLE adjustment_history
ALTER COLUMN change_source TYPE varchar(50);

DROP TYPE source_systems;
DROP TYPE change_types;