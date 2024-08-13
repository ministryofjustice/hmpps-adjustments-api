CREATE TABLE unused_deductions_calculation_result
(
    id                     UUID                           NOT NULL constraint unused_deductions_calculation_result_pk PRIMARY KEY,
    person                 varchar(10)                    NOT NULL,
    calculation_at         timestamp with time zone       NOT NULL,
    status                 varchar(50)                    NOT NULL
);
CREATE INDEX unused_deductions_calculation_result_person_reference ON unused_deductions_calculation_result(person);
