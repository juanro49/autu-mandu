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

import java.util.List;

import me.kuehle.carreport.db.query.SafeSelect;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Column.ForeignKeyAction;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;

@Table(name = "fuel_tanks")
public class FuelTank extends Model {
	@Column(name = "car", notNull = true, onUpdate = ForeignKeyAction.CASCADE, onDelete = ForeignKeyAction.CASCADE)
	public Car car;

	@Column(name = "name", notNull = true)
	public String name;

	public FuelTank() {
		super();
	}

	public FuelTank(Car car, String name) {
		super();
		this.car = car;
		this.name = name;
	}

	public List<FuelType> fuelTypes() {
		return SafeSelect.from(FuelType.class)
				.join(PossibleFuelTypeForFuelTank.class)
				.on("fuel_types.id = fuel_types_fuel_tanks.fuel_type")
				.where("fuel_types_fuel_tanks.fuel_tank = ?", id)
				.execute();
	}

	public List<Refueling> refuelings() {
		return new Select().from(Refueling.class)
				.where("fuel_tank = ?", id).orderBy("date ASC").execute();
	}

	/**
	 * Removes all unused fuel tanks. These are the ones, not used by any
	 * refuelings and fuel types.
	 */
	public static void cleanUp() {
		String sqlRefuelings = new Select().from(Refueling.class)
				.where("refuelings.fuel_tank = fuel_tanks.Id").toSql();
		String sqlPossibleTypes = new Select()
				.from(PossibleFuelTypeForFuelTank.class)
				.where("fuel_types_fuel_tanks.fuel_tank = fuel_tanks.Id")
				.toSql();

		new Delete()
				.from(FuelTank.class)
				.where("NOT EXISTS (" + sqlRefuelings + ") AND NOT EXISTS ("
						+ sqlPossibleTypes + ")").execute();
	}
}
