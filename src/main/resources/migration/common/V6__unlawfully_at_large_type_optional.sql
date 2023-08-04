ALTER TABLE unlawfully_at_large ALTER COLUMN type DROP NOT NULL;

-- The nomis migration data doesnt have the type of ual set
INSERT INTO unlawfully_at_large (adjustment_id)
    (SELECT id
     FROM adjustment a
     WHERE source = 'NOMIS'
       AND adjustment_type = 'UNLAWFULLY_AT_LARGE'
       AND NOT EXISTS (SELECT NULL FROM unlawfully_at_large WHERE adjustment_id = a.id));
