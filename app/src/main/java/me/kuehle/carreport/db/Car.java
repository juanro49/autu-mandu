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

import java.util.Date;
import java.util.List;

import me.kuehle.carreport.db.query.SafeSelect;
import android.database.Cursor;

import com.activeandroid.Cache;
import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

@Table(name = "cars")
public class Car extends Model {
	@Column(name = "name", notNull = true)
	public String name;

	@Column(name = "color", notNull = true)
	public int color;

	@Column(name = "suspended_since")
	public Date suspendedSince;

	public Car() {
		super();
	}

	public Car(String name, int color, Date suspendedSince) {
		super();
		this.name = name;
		this.color = color;
		this.suspendedSince = suspendedSince;
	}

	public boolean isSuspended() {
		return suspendedSince != null;
	}

	public List<Refueling> refuelings() {
		return SafeSelect.from(Refueling.class).join(FuelTank.class)
				.on("refuelings.fuel_tank = fuel_tanks.Id")
				.where("fuel_tanks.car = ?", id)
				.orderBy("refuelings.date ASC").execute();
	}

	public List<OtherCost> otherCosts() {
		return new Select().from(OtherCost.class).where("car = ?", id)
				.orderBy("date ASC").execute();
	}

	public List<FuelTank> fuelTanks() {
		return getMany(FuelTank.class, "car");
	}

	public static List<Car> getAll() {
		return new Select().from(Car.class)
                .orderBy("name ASC")
                .execute();
	}

    /**
     * Gets all cars, which are not suspended.
     * @return All not suspended cars.
     */
    public static List<Car> getAllActive() {
        return new Select().from(Car.class)
                .where("suspended_since IS NULL")
                .orderBy("name ASC")
                .execute();
    }

	public static int getCount() {
		String sql = new Select("COUNT(*)").from(Car.class).toSql();
		Cursor cursor = Cache.openDatabase().rawQuery(sql, null);

		int count = 0;
		if (cursor.moveToFirst() && cursor.getColumnCount() == 1) {
			count = cursor.getInt(0);
		}

		cursor.close();
		return count;
	}
}
