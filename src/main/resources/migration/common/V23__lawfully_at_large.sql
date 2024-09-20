CREATE TABLE lawfully_at_large
(
    adjustment_id UUID        NOT NULL CONSTRAINT lawfully_at_large_pk PRIMARY KEY REFERENCES adjustment (id),
    affects_dates VARCHAR(50) NOT NULL
);
