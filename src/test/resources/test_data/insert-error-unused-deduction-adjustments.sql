INSERT INTO adjustment(id,adjustment_type,person,days,effective_days,days_calculated,legacy_data,from_date,to_date,status,source) VALUES
    ('72ba4684-5674-4ada-9aa4-41011ff23451','REMAND','A1234TR',null,100,100,'{"type": "UAL", "comment": "comment added on nomis", "bookingId": 123, "migration": false, "postedDate": "2023-07-27", "sentenceSequence": 1}','2023-07-27 05:00:00+01','2023-11-03 05:00:00+01','ACTIVE','DPS'),
    ('5d2c10d0-0a31-49d1-93a9-52213bb344a5','TAGGED_BAIL','A1234TR',100,100,null,'{"type": "UAL", "comment": "comment added on nomis", "bookingId": 123, "migration": false, "postedDate": "2023-07-27", "sentenceSequence": 1}',null,null,'ACTIVE','DPS');



INSERT INTO adjustment_history (id,adjustment_id,change_at,change_by_username,change,change_type,change_source, prison_id) VALUES
  ('e577058c-549a-4513-b9b1-779b32858636','72ba4684-5674-4ada-9aa4-41011ff23451','2023-07-27 10:36:07.815798+01','USR_ADM','{}','CREATE','DPS', 'LDS'),
  ('08b1d202-445d-472c-b183-43b8707d8e48','5d2c10d0-0a31-49d1-93a9-52213bb344a5','2023-07-27 10:36:07.815798+01','USR_ADM','{}','CREATE','DPS', 'LDS');
