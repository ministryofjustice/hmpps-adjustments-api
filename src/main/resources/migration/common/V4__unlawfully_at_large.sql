CREATE TABLE unlawfully_at_large
(
    adjustment_id UUID        NOT NULL CONSTRAINT unlawfully_at_large_pk PRIMARY KEY REFERENCES adjustment (id),
    type          VARCHAR(50) NOT NULL
);
