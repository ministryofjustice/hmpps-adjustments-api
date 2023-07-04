CREATE TABLE additional_days_awarded
(
    id                     UUID                                  NOT NULL constraint additional_days_awarded_pk PRIMARY KEY,
    adjustment_id          UUID                                  NOT NULL references adjustment(id),
    adjudication_id        varchar(255)                          NOT NULL,
    consecutive            boolean                               NOT NULL
);