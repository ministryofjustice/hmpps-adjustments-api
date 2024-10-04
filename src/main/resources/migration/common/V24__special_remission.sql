CREATE TABLE special_remission
(
    adjustment_id UUID        NOT NULL CONSTRAINT special_remission_pk PRIMARY KEY REFERENCES adjustment (id),
    type VARCHAR(50) NOT NULL
);
