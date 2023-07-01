ALTER TABLE car
    ADD COLUMN buying_price DOUBLE NOT NULL DEFAULT 0;

CREATE TABLE station (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    station__name TEXT NOT NULL,
    CONSTRAINT unique_name UNIQUE (station__name) ON CONFLICT REPLACE
    );

INSERT INTO station (station__name)
VALUES ("Default");

ALTER TABLE refueling
    ADD COLUMN station_id INTEGER NOT NULL DEFAULT 1 REFERENCES station (_id) ON DELETE CASCADE;




