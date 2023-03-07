CREATE TYPE source_systems AS ENUM ('NOMIS', 'DPS');
CREATE TYPE change_types AS ENUM ('CREATE', 'UPDATE', 'DELETE');

CREATE TABLE adjustment
(
    id                     UUID                           NOT NULL constraint adjustment_pk PRIMARY KEY,
    adjustment_type        varchar(50)                    NOT NULL,
    person                 varchar(10)                    NOT NULL,
    days                   integer                        NULL,
    days_calculated        integer                        NOT NULL,
    legacy_data            JSONB                          NULL,
    from_date              timestamp with time zone       NULL,
    to_date                timestamp with time zone       NULL,
    deleted                boolean                        NOT NULL,
    source                 source_systems                 NOT NULL
);
CREATE INDEX adjustment_person_reference ON adjustment(person);

ALTER TABLE adjustment ADD CONSTRAINT adjustment_from_date_check check (
    NOT(source = 'DPS' AND from_date IS NULL)
);

CREATE TABLE adjustment_history
(
    id                     UUID                                  NOT NULL constraint adjustment_history_pk PRIMARY KEY,
    adjustment_id          UUID                                  NOT NULL references adjustment(id),
    change_at              timestamp with time zone              NOT NULL,
    change_by_username     varchar(255)                          NOT NULL,
    change                 JSONB                                 NULL,
    change_type            change_types                          NOT NULL
);