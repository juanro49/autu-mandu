-- 1. Create tank table
CREATE TABLE IF NOT EXISTS `tank` (
    `_id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `car_id` INTEGER NOT NULL,
    `fuel_category` TEXT NOT NULL,
    `tank__name` TEXT,
    `capacity` REAL NOT NULL,
    `is_manually_set` INTEGER NOT NULL,
    FOREIGN KEY(`car_id`) REFERENCES `car`(`_id`) ON UPDATE NO ACTION ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS `index_tank_car_id` ON `tank` (`car_id`);

-- 2. Add columns to refueling
ALTER TABLE `refueling` ADD COLUMN `start_level` REAL NOT NULL DEFAULT 0.0;
ALTER TABLE `refueling` ADD COLUMN `end_level` REAL NOT NULL DEFAULT 0.0;
ALTER TABLE `refueling` ADD COLUMN `tank_id` INTEGER NOT NULL DEFAULT 0 REFERENCES `tank`(`_id`) ON DELETE CASCADE;

-- 3. Create index for tank_id
CREATE INDEX IF NOT EXISTS `index_refueling_tank_id` ON `refueling` (`tank_id`);

-- 4. Create tanks from existing history
INSERT INTO `tank` (car_id, fuel_category, tank__name, capacity, is_manually_set)
SELECT DISTINCT r.car_id, f.category,
       CASE WHEN f.category = 'additives' THEN f.fuel_type__name ELSE f.category END,
       0.0, 0
FROM refueling r
JOIN fuel_type f ON r.fuel_type_id = f._id
WHERE f.category IS NOT NULL;

-- 5. Update refueling tank_id links
UPDATE `refueling` SET `tank_id` = (
    SELECT t._id FROM tank t JOIN fuel_type f ON refueling.fuel_type_id = f._id
    WHERE t.car_id = refueling.car_id
      AND t.fuel_category = f.category
      AND (f.category != 'additives' OR t.tank__name = f.fuel_type__name)
    LIMIT 1
);
