ALTER TABLE fuel_types ADD COLUMN category TEXT;
UPDATE fuel_types SET category = (SELECT fuel_tanks.name FROM fuel_tanks JOIN refuelings ON refuelings.fuel_tank = fuel_tanks.Id WHERE refuelings.fuel_type = fuel_types.Id LIMIT 1);
CREATE TABLE refuelings2 (Id INTEGER PRIMARY KEY AUTOINCREMENT, date INTEGER NOT NULL ON CONFLICT FAIL, mileage INTEGER NOT NULL ON CONFLICT FAIL, volume REAL NOT NULL ON CONFLICT FAIL, price REAL NOT NULL ON CONFLICT FAIL, partial INTEGER NOT NULL ON CONFLICT FAIL, note TEXT NOT NULL ON CONFLICT FAIL, fuel_type INTEGER NOT NULL ON CONFLICT FAIL REFERENCES fuel_types(Id) ON DELETE CASCADE ON UPDATE CASCADE, car INTEGER NOT NULL ON CONFLICT FAIL REFERENCES cars(Id) ON DELETE CASCADE ON UPDATE CASCADE);
INSERT INTO refuelings2 (Id, date, mileage, volume, price, partial, note, fuel_type, car) SELECT refuelings.Id, date, mileage, volume, price, partial, note, fuel_type, fuel_tanks.car FROM refuelings JOIN fuel_tanks ON refuelings.fuel_tank = fuel_tanks.Id;
DROP TABLE refuelings;
ALTER TABLE refuelings2 RENAME TO refuelings;
DROP TABLE fuel_types_fuel_tanks;
DROP TABLE fuel_tanks;
CREATE TABLE reminders (Id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL ON CONFLICT FAIL, after_time TEXT NULL ON CONFLICT FAIL, after_distance INTEGER NULL ON CONFLICT FAIL, start_date INTEGER NOT NULL ON CONFLICT FAIL, start_mileage INTEGER NOT NULL ON CONFLICT FAIL, car INTEGER NOT NULL ON CONFLICT FAIL REFERENCES cars(Id) ON DELETE CASCADE ON UPDATE CASCADE, notification_dismissed INTEGER NOT NULL ON CONFLICT FAIL, snoozed_until INTEGER NULL ON CONFLICT FAIL);
