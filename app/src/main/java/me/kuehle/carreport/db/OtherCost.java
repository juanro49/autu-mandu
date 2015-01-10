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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import me.kuehle.carreport.util.Recurrence;
import android.database.Cursor;

import com.activeandroid.Cache;
import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Column.ForeignKeyAction;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

@Table(name = "other_costs")
public class OtherCost extends Model {
	@Column(name = "title", notNull = true)
	public String title;

	@Column(name = "date", notNull = true)
	public Date date;

	@Column(name = "end_date")
	public Date endDate;

	@Column(name = "mileage")
	public int mileage;

	@Column(name = "price", notNull = true)
	public float price;

	@Column(name = "recurrence", notNull = true)
	public Recurrence recurrence;

	@Column(name = "note", notNull = true)
	public String note;

	@Column(name = "car", notNull = true, onUpdate = ForeignKeyAction.CASCADE, onDelete = ForeignKeyAction.CASCADE)
	public Car car;

	public OtherCost() {
		super();
	}

	public OtherCost(String title, Date date, Date endDate, int mileage,
			float price, Recurrence recurrence, String note, Car car) {
		super();
		this.title = title;
		this.date = date;
		this.endDate = endDate;
		this.mileage = mileage;
		this.price = price;
		this.recurrence = recurrence;
		this.note = note;
		this.car = car;
	}

    public static List<String> getAllExpenditureTitles() {
        return getAllTitlesFiltered("price > 0");
    }

    public static List<String> getAllIncomeTitles() {
        return getAllTitlesFiltered("price < 0");
    }

    public static OtherCost getLatest(Car car) {
        return new Select().from(OtherCost.class)
                .where("car = ?", car.id)
                .orderBy("date DESC")
                .executeSingle();
    }

	private static List<String> getAllTitlesFiltered(String where) {
		String sql = new Select("title").distinct().from(OtherCost.class)
                .where(where)
                .orderBy("title COLLATE UNICODE ASC")
                .toSql();
		Cursor cursor = Cache.openDatabase().rawQuery(sql, null);

		List<String> titles = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                titles.add(cursor.getString(0));
            }

            cursor.close();
        }

		return titles;
	}
}
