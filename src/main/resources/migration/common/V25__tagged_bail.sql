CREATE TABLE tagged_bail
(
    adjustment_id UUID NOT NULL CONSTRAINT tagged_bail_pk PRIMARY KEY REFERENCES adjustment (id),
    court_case_uuid UUID NOT NULL
);