ALTER TABLE adjustment_history ADD COLUMN prison_id varchar(10);

UPDATE adjustment_history AS ah
SET prison_id = a.prison_id
FROM adjustment a
WHERE ah.adjustment_id = a.id;

ALTER TABLE adjustment DROP COLUMN prison_id;
