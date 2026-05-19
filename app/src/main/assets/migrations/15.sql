-- Tabla principal: Viajes
CREATE TABLE IF NOT EXISTS `trip` (
    `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `car_id` INTEGER NOT NULL,
    `refueling_id` INTEGER,
    `date` TEXT NOT NULL,
    `date_end` TEXT NOT NULL,
    `time_start` TEXT NOT NULL,
    `time_end` TEXT NOT NULL,
    `route_target` TEXT NOT NULL,
    `purpose` TEXT NOT NULL,
    `companies_visited` TEXT,
    `driver` TEXT,
    `occupants` INTEGER,
    `cargo` TEXT,
    `km_start` INTEGER NOT NULL,
    `km_end` INTEGER NOT NULL,
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
    `created_at` TEXT NOT NULL,
    `updated_at` TEXT NOT NULL,
    FOREIGN KEY(`car_id`) REFERENCES `car`(`_id`) ON UPDATE NO ACTION ON DELETE CASCADE,
    FOREIGN KEY(`refueling_id`) REFERENCES `refueling`(`_id`) ON UPDATE NO ACTION ON DELETE SET NULL
);

-- Tabla de valores predefinidos (prefabs)
CREATE TABLE IF NOT EXISTS `trip_prefab` (
    `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `car_id` INTEGER NOT NULL,
    `type` TEXT NOT NULL,
    `value` TEXT NOT NULL,
    `usage_count` INTEGER NOT NULL,
    FOREIGN KEY(`car_id`) REFERENCES `car`(`_id`) ON UPDATE NO ACTION ON DELETE CASCADE
);

-- Índices (Nombres y estructuras exactas de Room)
CREATE INDEX IF NOT EXISTS `index_trip_car_id` ON `trip` (`car_id`);
CREATE INDEX IF NOT EXISTS `index_trip_date` ON `trip` (`date`);
CREATE INDEX IF NOT EXISTS `index_trip_refueling_id` ON `trip` (`refueling_id`);
CREATE INDEX IF NOT EXISTS `idx_car_date` ON `trip` (`car_id`, `date`);

CREATE INDEX IF NOT EXISTS `index_trip_prefab_car_id_type` ON `trip_prefab` (`car_id`, `type`);
CREATE UNIQUE INDEX IF NOT EXISTS `index_trip_prefab_car_id_type_value` ON `trip_prefab` (`car_id`, `type`, `value`);
