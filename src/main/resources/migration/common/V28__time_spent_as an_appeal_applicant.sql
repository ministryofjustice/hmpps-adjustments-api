CREATE TABLE time_spent_as_an_appeal_applicant
(
    adjustment_id UUID NOT NULL CONSTRAINT time_spent_as_an_appeal_applicant_pk PRIMARY KEY REFERENCES adjustment (id),
    court_of_appeal_reference_number VARCHAR(30) NOT NULL
);
