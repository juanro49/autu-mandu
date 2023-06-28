ALTER TABLE car
    ADD COLUMN buying_price DOUBLE NOT NULL DEFAULT 0;
    --ADD COLUMN make TEXT
    --ADD COLUMN model TEXT,
    --ADD COLUMN year INTEGER,
    --ADD COLUMN license_plate TEXT,
    --ADD COLUMN buying_date INTEGER;

CREATE TABLE station (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    station__name TEXT NOT NULL,
    CONSTRAINT unique_name UNIQUE (station__name) ON CONFLICT REPLACE
    );

ALTER TABLE refueling
    ADD COLUMN station_id INTEGER NOT NULL,
    ADD CONSTRAINT fk_station_id FOREIGN KEY (station_id) REFERENCES station (_id) ON DELETE CASCADE;




