DROP TABLE unlawfully_at_large;

CREATE TABLE unlawfully_at_large
(
    id UUID        NOT NULL CONSTRAINT unlawfully_at_large_pk PRIMARY KEY,
    adjustment_id UUID        NOT NULL CONSTRAINT unlawfully_at_large_fk REFERENCES adjustment (id),
    type          VARCHAR(50) NOT NULL
);
