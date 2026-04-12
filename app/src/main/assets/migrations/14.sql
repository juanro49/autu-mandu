-- Indexes for foreign keys
CREATE INDEX IF NOT EXISTS index_refueling_car_id ON refueling(car_id);
CREATE INDEX IF NOT EXISTS index_refueling_fuel_type_id ON refueling(fuel_type_id);
CREATE INDEX IF NOT EXISTS index_refueling_station_id ON refueling(station_id);
CREATE INDEX IF NOT EXISTS index_other_cost_car_id ON other_cost(car_id);
CREATE INDEX IF NOT EXISTS index_reminder_car_id ON reminder(car_id);
CREATE INDEX IF NOT EXISTS index_tire_usage_tire_id ON tire_usage(tire_id);
CREATE INDEX IF NOT EXISTS index_tire_list_car_id ON tire_list(car_id);
