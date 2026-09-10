-- Migration to version 18: Add is_partial to trip and allow nulls for end fields
ALTER TABLE `trip` RENAME TO `trip_old`;

CREATE TABLE `trip` (
    `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `car_id` INTEGER NOT NULL,
    `refueling_id` INTEGER,
    `date` TEXT NOT NULL,
    `date_end` TEXT,
    `time_start` TEXT NOT NULL,
    `time_end` TEXT,
    `route_target` TEXT,
    `purpose` TEXT,
    `companies_visited` TEXT,
    `driver` TEXT,
    `occupants` INTEGER,
    `cargo` TEXT,
    `km_start` INTEGER NOT NULL,
    `km_end` INTEGER,
    `km_business` INTEGER NOT NULL,
    `km_private` INTEGER NOT NULL,
    `km_home_work` INTEGER NOT NULL,
    `start_lat` REAL,
    `start_lon` REAL,
    `end_lat` REAL,
    `end_lon` REAL,
    `fuel_liters` REAL,
    `fuel_cost` REAL,
    `other_costs_description` TEXT,
    `other_costs_amount` REAL,
    `is_partial` INTEGER NOT NULL DEFAULT 0,
    `created_at` TEXT NOT NULL,
    `updated_at` TEXT NOT NULL,
    FOREIGN KEY(`car_id`) REFERENCES `car`(`_id`) ON UPDATE NO ACTION ON DELETE CASCADE,
    FOREIGN KEY(`refueling_id`) REFERENCES `refueling`(`_id`) ON UPDATE NO ACTION ON DELETE SET NULL
);

INSERT INTO `trip` (_id, car_id, refueling_id, date, date_end, time_start, time_end, route_target, purpose, companies_visited, driver, occupants, cargo, km_start, km_end, km_business, km_private, km_home_work, start_lat, start_lon, end_lat, end_lon, fuel_liters, fuel_cost, other_costs_description, other_costs_amount, is_partial, created_at, updated_at)
SELECT _id, car_id, refueling_id, date, date_end, time_start, time_end, route_target, purpose, companies_visited, driver, occupants, cargo, km_start, km_end, km_business, km_private, km_home_work, start_lat, start_lon, end_lat, end_lon, fuel_liters, fuel_cost, other_costs_description, other_costs_amount, 0, created_at, updated_at FROM `trip_old`;

DROP TABLE `trip_old`;

CREATE INDEX IF NOT EXISTS `index_trip_car_id` ON `trip` (`car_id`);
CREATE INDEX IF NOT EXISTS `index_trip_date` ON `trip` (`date`);
CREATE INDEX IF NOT EXISTS `index_trip_refueling_id` ON `trip` (`refueling_id`);
CREATE INDEX IF NOT EXISTS `idx_car_date` ON `trip` (`car_id`, `date`);
