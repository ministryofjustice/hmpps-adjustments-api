CREATE TABLE unused_deductions
(
    id                     UUID                           NOT NULL constraint unused_deductions_pk PRIMARY KEY,
    days                   integer                        NOT NULL,
    legacy_data            JSONB                          NULL,
    person                 varchar(10)                    NOT NULL,
    status                 varchar(10)                    NOT NULL
);


ALTER TABLE adjustment RENAME COLUMN days_calculated TO effective_days;
UPDATE adjustment SET effective_days = days;

