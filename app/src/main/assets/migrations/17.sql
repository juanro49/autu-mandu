-- Migration to add split_price to other_cost
ALTER TABLE `other_cost` ADD COLUMN `split_price` INTEGER NOT NULL DEFAULT 0;

-- Migration to add buying_price_split_months and purchase_date to car
ALTER TABLE `car` ADD COLUMN `buying_price_split_months` INTEGER NOT NULL DEFAULT 0;
ALTER TABLE `car` ADD COLUMN `purchase_date` INTEGER;
