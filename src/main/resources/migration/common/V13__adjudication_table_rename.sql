ALTER TABLE additional_days_awarded
RENAME TO adjudication_charges;

ALTER TABLE adjudication_charges
DROP COLUMN consecutive;

ALTER TABLE adjudication_charges
DROP COLUMN id;
