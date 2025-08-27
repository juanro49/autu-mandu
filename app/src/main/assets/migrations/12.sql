ALTER TABLE car
    ADD COLUMN num_tires INTEGER NOT NULL DEFAULT 4;

CREATE TABLE tire_list (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    buy_date INTEGER NOT NULL,
    trash_date INTEGER DEFAULT NULL,
    price REAL NOT NULL,
    quantity INTEGER NOT NULL,
    manufacturer TEXT NOT NULL,
    model TEXT NOT NULL,
    note TEXT NOT NULL,
    car_id INTEGER NOT NULL,
    CONSTRAINT fk_car_id FOREIGN KEY (car_id) REFERENCES car (_id) ON DELETE CASCADE
);

CREATE TABLE tire_usage (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    distance_mount INTEGER NOT NULL,
    date_mount INTEGER NOT NULL,
    distance_umount INTEGER NOT NULL,
    date_umount INTEGER DEFAULT NULL,
    tire_id INTEGER NOT NULL,
    CONSTRAINT fk_tire_id FOREIGN KEY (tire_id) REFERENCES tire_list (_id) ON DELETE CASCADE
);
