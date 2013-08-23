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

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;

@Table(name = "fuel_types")
public class FuelType extends Model {
	@Column(name = "name", notNull = true, unique = true)
	public String name;

	public FuelType() {
		super();
	}

	public FuelType(String name) {
		super();
		this.name = name;
	}

	public List<Refueling> refuelings() {
		return new Select().from(Refueling.class)
				.where("fuel_type = ?", getId()).orderBy("date ASC").execute();
	}

	/**
	 * Removes all unused fuel types. These are the ones, not used by any
	 * refuelings and fuel tanks.
	 */
	public static void cleanUp() {
		String sqlRefuelings = new Select().from(Refueling.class)
				.where("refuelings.fuel_type = fuel_types.Id").toSql();
		String sqlPossibleTypes = new Select()
				.from(PossibleFuelTypeForFuelTank.class)
				.where("fuel_types_fuel_tanks.fuel_type = fuel_types.Id")
				.toSql();

		new Delete()
				.from(FuelType.class)
				.where("NOT EXISTS (" + sqlRefuelings + ") AND NOT EXISTS ("
						+ sqlPossibleTypes + ")").execute();
	}

	public static List<FuelType> getAll() {
		return new Select().from(FuelType.class).orderBy("name ASC").execute();
	}
}
