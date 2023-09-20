UPDATE adjustment SET status = 'DELETED' WHERE deleted = true;
UPDATE adjustment SET status = 'INACTIVE' WHERE legacy_data->>'active' = 'false';
UPDATE adjustment SET status = 'ACTIVE' WHERE status IS NULL;