INSERT INTO gss.factory (factory_id, name) VALUES
    ('TMC-BD', 'Nhà máy Bình Dương'),
    ('TMC-LA', 'Nhà máy Long An');

INSERT INTO gss.gate_station
    (station_id, factory_id, name, direction, gate_id, kiosk_id, status)
VALUES
    ('BD-IN-01',  'TMC-BD', 'Cổng vào 1', 'IN',  'BD-TS-01', 'BD-KSK-01', 'ACTIVE'),
    ('BD-IN-02',  'TMC-BD', 'Cổng vào 2', 'IN',  'BD-TS-02', 'BD-KSK-02', 'ACTIVE'),
    ('BD-IN-03',  'TMC-BD', 'Cổng vào 3', 'IN',  'BD-TS-03', 'BD-KSK-03', 'ACTIVE'),
    ('BD-OUT-01', 'TMC-BD', 'Cổng ra 1',  'OUT', 'BD-TS-04', 'BD-KSK-04', 'ACTIVE'),
    ('BD-OUT-02', 'TMC-BD', 'Cổng ra 2',  'OUT', 'BD-TS-05', 'BD-KSK-05', 'ACTIVE'),
    ('BD-OUT-03', 'TMC-BD', 'Cổng ra 3',  'OUT', 'BD-TS-06', 'BD-KSK-06', 'MAINTENANCE'),
    ('LA-IN-01',  'TMC-LA', 'Cổng vào 1', 'IN',  'LA-TS-01', 'LA-KSK-01', 'ACTIVE'),
    ('LA-IN-02',  'TMC-LA', 'Cổng vào 2', 'IN',  'LA-TS-02', 'LA-KSK-02', 'ACTIVE'),
    ('LA-OUT-01', 'TMC-LA', 'Cổng ra 1',  'OUT', 'LA-TS-03', 'LA-KSK-03', 'ACTIVE'),
    ('LA-OUT-02', 'TMC-LA', 'Cổng ra 2',  'OUT', 'LA-TS-04', 'LA-KSK-04', 'ACTIVE');
