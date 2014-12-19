CREATE TABLE othercosts2 (_id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, date INTEGER NOT NULL, tachometer INTEGER NOT NULL DEFAULT -1, price REAL NOT NULL, repeat_interval INTEGER NOT NULL DEFAULT 0, repeat_multiplier INTEGER NOT NULL DEFAULT 1, note TEXT NOT NULL, cars_id INTEGER NOT NULL, FOREIGN KEY(cars_id) REFERENCES cars(_id) ON UPDATE CASCADE ON DELETE CASCADE);
INSERT INTO othercosts2 (_id, title, date, tachometer, price, repeat_interval, repeat_multiplier, note, cars_id) SELECT othercosts._id, title, date, tachometer, price, repeat_interval, repeat_multiplier, note, cars_id FROM othercosts JOIN cars ON cars._id = othercosts.cars_id;
DROP TABLE othercosts;
ALTER TABLE othercosts2 RENAME TO othercosts;
CREATE TABLE refuelings2 (_id INTEGER PRIMARY KEY AUTOINCREMENT, date INTEGER NOT NULL, tachometer INTEGER NOT NULL, volume REAL NOT NULL, price REAL NOT NULL, partial INTEGER NOT NULL, note TEXT NOT NULL, cars_id INTEGER NOT NULL, FOREIGN KEY(cars_id) REFERENCES cars(_id) ON UPDATE CASCADE ON DELETE CASCADE);
INSERT INTO refuelings2 (_id, date, tachometer, volume, price, partial, note, cars_id) SELECT refuelings._id, date, tachometer, volume, price, partial, note, cars_id FROM refuelings JOIN cars ON cars._id = refuelings.cars_id;
DROP TABLE refuelings;
ALTER TABLE refuelings2 RENAME TO refuelings;