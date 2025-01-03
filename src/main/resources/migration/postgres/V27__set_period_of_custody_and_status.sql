UPDATE adjustment SET status = 'ACTIVE' WHERE status = 'INACTIVE' AND legacy_data->>'adjustmentActive' = 'true';

UPDATE adjustment SET current_period_of_custody = true WHERE legacy_data->>'bookingActive' = 'true';
UPDATE adjustment SET current_period_of_custody = false WHERE legacy_data->>'bookingActive' != 'true';