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
import java.util.List;

@Table(name = "fuel_types")
public class FuelType extends Model {
    @Column(name = "name", notNull = true, unique = true)
    public String name;

    @Column(name = "category")
    public String category;

    public FuelType() {
        super();
    }

    public FuelType(String name, String category) {
        super();
        this.name = name;
        this.category = category;
    }

    public List<Refueling> getRefuelings() {
        return new Select().from(Refueling.class)
                .where("fuel_type = ?", id)
                .orderBy("date ASC")
                .execute();
    }

    public static List<FuelType> getAll() {
        return new Select().from(FuelType.class).orderBy("name ASC").execute();
    }

    public static List<String> getAllCategories() {
        String sql = new Select("category").distinct().from(FuelType.class)
                .orderBy("category ASC").toSql();
        Cursor cursor = Cache.openDatabase().rawQuery(sql, null);

        List<String> titles = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                titles.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return titles;
    }

    public static int getCount() {
        String sql = new Select("COUNT(*)").from(FuelType.class).toSql();
        Cursor cursor = Cache.openDatabase().rawQuery(sql, null);

        int count = 0;
        if (cursor.moveToFirst() && cursor.getColumnCount() == 1) {
            count = cursor.getInt(0);
        }

        cursor.close();
        return count;
    }

    public static void ensureAtLeastOneFuelType() {
        if (getCount() == 0) {
            new FuelType("Default", "Default").save();
        }
    }
}
