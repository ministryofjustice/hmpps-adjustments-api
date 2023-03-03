CREATE TABLE adjustment_type
(
    id varchar(20) not null constraint adjustment_type_pk PRIMARY KEY,
    credit_or_debit boolean not null
);

CREATE TABLE adjustment
(
    id                     UUID                           NOT NULL constraint adjustment_pk PRIMARY KEY,
    adjustment_type_id     varchar(20)                    NOT NULL references adjustment_type(id),
    person                 varchar(10)                    NOT NULL,
    days                   integer                        NOT NULL,
    days_calculated        integer                        NOT NULL,
    legacy_data            JSONB                          NOT NULL,
    from_date              timestamp with time zone       NOT NULL,
    to_date                timestamp with time zone       NOT NULL,
    deleted                boolean                        NOT NULL
);
CREATE INDEX adjustment_person_reference ON adjustment(person);

CREATE TABLE adjustment_history
(
    id                     UUID                                  NOT NULL constraint adjustment_history_pk PRIMARY KEY,
    adjustment_id          UUID                                  NOT NULL references adjustment(id),
    change_at              timestamp with time zone              NOT NULL,
    change_by_username     varchar(255)                          NOT NULL,
    change_type            enum('CREATE', 'UPDATE', 'DELETE')    NOT NULL
);