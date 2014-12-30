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

import android.database.Cursor;

import com.activeandroid.Cache;
import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import me.kuehle.carreport.db.query.SafeSelect;

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

    public List<Refueling> getRefuelings() {
        return new Select().from(Refueling.class)
                .where("car = ?", id)
                .orderBy("date ASC")
                .execute();
    }

    public List<Refueling> getRefuelingsByFuelTypeCategory(String category) {
        return SafeSelect.from(Refueling.class)
                .join(FuelType.class).on("fuel_types.Id = refuelings.fuel_type")
                .where("refuelings.car = ? AND fuel_types.category = ?", id, category)
                .orderBy("refuelings.date ASC")
                .execute();
    }

    public List<OtherCost> getOtherCosts() {
        return new Select().from(OtherCost.class)
                .where("car = ?", id)
                .orderBy("date ASC")
                .execute();
    }

    public FuelType getMostUsedFuelType() {
        return SafeSelect.from(FuelType.class)
                .join(Refueling.class).on("refuelings.fuel_type = fuel_types.Id")
                .where("refuelings.car = ?", id)
                .groupBy("fuel_types.Id")
                .orderBy("COUNT(refuelings.Id) DESC")
                .limit(1)
                .executeSingle();
    }

    public List<String> getUsedFuelTypeCategories() {
        String sql = new Select("category").distinct().from(FuelType.class)
                .join(Refueling.class).on("refuelings.fuel_type = fuel_types.Id")
                .where("refuelings.car = " + id) // toSql function does not support where arguments
                .orderBy("category ASC")
                .toSql();
        Cursor cursor = Cache.openDatabase().rawQuery(sql, null);

        List<String> categories = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                categories.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return categories;
    }

    public static List<Car> getAll() {
        return new Select().from(Car.class)
                .orderBy("name ASC")
                .execute();
    }

    /**
     * Gets all cars, which are not suspended.
     *
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
