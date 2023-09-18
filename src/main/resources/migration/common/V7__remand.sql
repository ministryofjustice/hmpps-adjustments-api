CREATE TABLE remand
(
    adjustment_id UUID           NOT NULL CONSTRAINT remand_pk PRIMARY KEY REFERENCES adjustment (id),
    unused_remand_id UUID        NOT NULL CONSTRAINT unused_remand_fk REFERENCES adjustment (id)
);
