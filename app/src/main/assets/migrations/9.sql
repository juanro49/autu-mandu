CREATE TABLE car (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    car__name TEXT NOT NULL,
    color INTEGER NOT NULL,
    suspended_since INTEGER
);

INSERT INTO car(_id, car__name, color, suspended_since)
SELECT Id, name, color, suspended_since
FROM cars;



CREATE TABLE fuel_type (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    fuel_type__name TEXT NOT NULL,
    category TEXT,
    CONSTRAINT unique_name UNIQUE (fuel_type__name) ON CONFLICT REPLACE
);

INSERT INTO fuel_type (_id, fuel_type__name, category)
SELECT Id, name, category
FROM fuel_types;



CREATE TABLE other_cost (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    date INTEGER NOT NULL,
    mileage INTEGER,
    price REAL NOT NULL,
    recurrence_interval INTEGER NOT NULL,
    recurrence_multiplier INTEGER NOT NULL,
    end_date INTEGER,
    note TEXT NOT NULL,
    car_id INTEGER NOT NULL,
    CONSTRAINT fk_car_id FOREIGN KEY (car_id) REFERENCES car (_id) ON DELETE CASCADE
);

INSERT INTO other_cost (_id, title, date, mileage, price, recurrence_interval, recurrence_multiplier, end_date, note, car_id)
SELECT Id, title, date, mileage, price, substr(recurrence, 1, r_pos-1), substr(recurrence, r_pos+1), end_date, note, car
FROM (SELECT *, instr(recurrence,' ') AS r_pos FROM other_costs);



CREATE TABLE refueling (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    date INTEGER NOT NULL,
    mileage INTEGER NOT NULL,
    volume REAL NOT NULL,
    price REAL NOT NULL,
    partial INTEGER NOT NULL,
    note TEXT NOT NULL,
    fuel_type_id INTEGER NOT NULL,
    car_id INTEGER NOT NULL,
    CONSTRAINT fk_fuel_type_id FOREIGN KEY (fuel_type_id) REFERENCES fuel_type (_id) ON DELETE CASCADE,
    CONSTRAINT fk_car_id FOREIGN KEY (car_id) REFERENCES car (_id) ON DELETE CASCADE
);

INSERT INTO refueling (_id, date, mileage, volume, price, partial, note, fuel_type_id, car_id)
SELECT Id, date, mileage, volume, price, partial, note, fuel_type, car
FROM refuelings;



CREATE TABLE reminder (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    after_time_span_unit INTEGER,
    after_time_span_count INTEGER,
    after_distance INTEGER,
    start_date INTEGER NOT NULL,
    start_mileage INTEGER NOT NULL,
    notification_dismissed INTEGER NOT NULL,
    snoozed_until INTEGER,
    car_id INTEGER NOT NULL,
    CONSTRAINT fk_car_id FOREIGN KEY (car_id) REFERENCES car (_id) ON DELETE CASCADE
);

INSERT INTO reminder (_id, title, after_time_span_unit, after_time_span_count, after_distance, start_date, start_mileage, notification_dismissed, snoozed_until, car_id)
SELECT Id, title, substr(after_time, 1, at_pos-1), substr(after_time, at_pos+1), after_distance, start_date, start_mileage, notification_dismissed, snoozed_until, car
FROM (SELECT *, instr(after_time,' ') AS at_pos FROM reminders);



DROP TABLE other_costs;
DROP TABLE reminders;
DROP TABLE refuelings;
DROP TABLE cars;
DROP TABLE fuel_types;
