CREATE TABLE prospective_ada_rejection
(
    id                     UUID                           NOT NULL constraint prospective_ada_rejection_pk PRIMARY KEY,
    person                 varchar(10)                    NOT NULL,
    date_charge_proved     timestamp with time zone       NOT NULL,
    days                   integer                        NOT NULL,
    rejection_at           timestamp with time zone       NOT NULL
);
CREATE INDEX prospective_ada_rejection_person_reference ON prospective_ada_rejection(person);
