INSERT INTO adjustment (id,adjustment_type,person,days,effective_days,days_calculated,legacy_data,from_date,to_date,status,source,recall_id) VALUES
    ('3f8a973b-c16e-46ef-8a02-07ac378d990e','UNLAWFULLY_AT_LARGE','BCDEFG',null,3,3,'{"type": "UAL", "comment": "Adjustment from recall", "bookingId": 1090016, "migration": false, "postedDate": "2023-07-27", "sentenceSequence": null}','2023-07-27 00:00:00+01','2023-07-29 00:00:00+01','ACTIVE','DPS', '2ea3ae97-c469-491e-ae93-bdcda9d8ac91');

INSERT INTO adjustment_history (id, adjustment_id,change_at,change_by_username,change,change_type,change_source, prison_id) VALUES
    ('9b094ec9-bd32-415d-87fa-44f6a9bde7d9', '3f8a973b-c16e-46ef-8a02-07ac378d990e','2023-07-27 10:36:07.815798+01','USR_ADM','{}','CREATE','NOMIS', 'LDS');