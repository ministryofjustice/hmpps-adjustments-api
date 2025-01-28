CREATE TABLE time_spent_in_custody_abroad
(
    adjustment_id UUID NOT NULL CONSTRAINT time_spent_in_custody_abroad_pk PRIMARY KEY REFERENCES adjustment (id),
    documentation_source VARCHAR(50) NOT NULL
);
