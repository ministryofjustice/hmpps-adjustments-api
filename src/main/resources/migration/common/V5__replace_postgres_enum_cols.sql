ALTER TABLE adjustment DROP CONSTRAINT adjustment_from_date_check;

ALTER TABLE adjustment
DROP COLUMN source;

ALTER TABLE adjustment_history
DROP COLUMN change_type;

ALTER TABLE adjustment_history
DROP COLUMN change_source;

DROP TYPE source_systems;
DROP TYPE change_types;


ALTER TABLE adjustment ADD COLUMN
  source varchar(50) NOT NULL default 'NOMIS';

ALTER TABLE adjustment ADD CONSTRAINT adjustment_from_date_check check (
    NOT(source = 'DPS' AND from_date IS NULL)
);

ALTER TABLE adjustment_history ADD COLUMN
  change_type varchar(50) NOT NULL default 'CREATE';


ALTER TABLE adjustment_history ADD COLUMN
  change_source varchar(50) NOT NULL default 'NOMIS';
