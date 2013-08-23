/*
 * Copyright 2013 Jan Kühle
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.kuehle.carreport.db;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Column.ForeignKeyAction;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;

@Table(name = "fuel_types_fuel_tanks")
public class PossibleFuelTypeForFuelTank extends Model {
	@Column(name = "fuel_type", notNull = true, onUpdate = ForeignKeyAction.CASCADE, onDelete = ForeignKeyAction.CASCADE)
	public FuelType fuelType;

	@Column(name = "fuel_tank", notNull = true, onUpdate = ForeignKeyAction.CASCADE, onDelete = ForeignKeyAction.CASCADE)
	public FuelTank fuelTank;

	public PossibleFuelTypeForFuelTank() {
		super();
	}

	public PossibleFuelTypeForFuelTank(FuelType fuelType, FuelTank fuelTank) {
		super();
		this.fuelType = fuelType;
		this.fuelTank = fuelTank;
	}

	public static void deleteAll(Car car) {
		new Delete()
				.from(PossibleFuelTypeForFuelTank.class)
				.where("fuel_types_fuel_tanks.fuel_tank IN ("
						+ new Select("fuel_tanks.Id").from(FuelTank.class)
								.where("fuel_tanks.car = " + car.getId())
								.toSql() + ")").execute();
	}
}
