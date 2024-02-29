ALTER TABLE adjustment_history ADD COLUMN prison_id varchar(10);

UPDATE adjustment_history ah SET ah.prison_id = a.prison_id
INNER JOIN adjustment a ON ah.adjustment_id = a.id;

ALTER TABLE adjustment DROP COLUMN prison_id;
