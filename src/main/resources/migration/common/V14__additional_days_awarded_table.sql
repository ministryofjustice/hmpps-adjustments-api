
CREATE TABLE additional_days_awarded
(
    adjustment_id UUID        NOT NULL CONSTRAINT additional_days_awarded_pk PRIMARY KEY REFERENCES adjustment (id),
    prospective               BOOLEAN NOT NULL
);


INSERT INTO additional_days_awarded ( adjustment_id, prospective )
SELECT adjustment_id, FALSE as prospective
FROM  adjudication_charges
GROUP BY adjustment_id;

