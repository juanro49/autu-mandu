/*
 * Copyright 2012 Jan Kühle
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

import me.kuehle.carreport.util.Recurrence;
import me.kuehle.carreport.util.RecurrenceInterval;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

public class OtherCost extends AbstractItem {
	private String title;
	private Date date;
	private int mileage;
	private float price;
	private Recurrence recurrence;
	private String note;
	private Car car;

	public OtherCost(int id) {
		synchronized (Helper.dbLock) {
			SQLiteDatabase db = Helper.getInstance().getReadableDatabase();
			Cursor cursor = db.query(OtherCostTable.NAME,
					OtherCostTable.ALL_COLUMNS, BaseColumns._ID + "=?",
					new String[] { String.valueOf(id) }, null, null, null);
			if (cursor.getCount() != 1) {
				cursor.close();
				throw new IllegalArgumentException(
						"An other cost with this ID does not exist!");
			} else {
				cursor.moveToFirst();
				this.id = id;
				this.title = cursor.getString(1);
				this.date = new Date(cursor.getLong(2));
				this.mileage = cursor.getInt(3);
				this.price = cursor.getFloat(4);
				this.recurrence = new Recurrence(
						RecurrenceInterval.getByValue(cursor.getInt(5)),
						cursor.getInt(6));
				this.note = cursor.getString(7);
				this.car = new Car(cursor.getInt(8));
				cursor.close();
			}
		}
	}

	private OtherCost(int id, String title, Date date, int mileage,
			float price, Recurrence recurrence, String note, Car car) {
		this.id = id;
		this.title = title;
		this.date = date;
		this.mileage = mileage;
		this.price = price;
		this.recurrence = recurrence;
		this.note = note;
		this.car = car;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public int getMileage() {
		return mileage;
	}

	public void setMileage(int mileage) {
		this.mileage = mileage;
	}

	public float getPrice() {
		return price;
	}

	public void setPrice(float price) {
		this.price = price;
	}

	public Recurrence getRecurrence() {
		return recurrence;
	}

	public void setRecurrence(Recurrence recurrence) {
		this.recurrence = recurrence;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public Car getCar() {
		return car;
	}

	public void setCar(Car car) {
		this.car = car;
	}

	public void delete() {
		if (!isDeleted()) {
			synchronized (Helper.dbLock) {
				SQLiteDatabase db = Helper.getInstance().getWritableDatabase();
				db.delete(OtherCostTable.NAME, BaseColumns._ID + "=?",
						new String[] { String.valueOf(id) });
			}
			Helper.getInstance().dataChanged();
			deleted = true;
		}
	}

	public void save() {
		if (!isDeleted()) {
			ContentValues values = new ContentValues();
			values.put(OtherCostTable.COL_TITLE, title);
			values.put(OtherCostTable.COL_DATE, date.getTime());
			values.put(OtherCostTable.COL_TACHO, mileage);
			values.put(OtherCostTable.COL_PRICE, price);
			values.put(OtherCostTable.COL_REP_INT, recurrence.getInterval()
					.getValue());
			values.put(OtherCostTable.COL_REP_MULTI, recurrence.getMultiplier());
			values.put(OtherCostTable.COL_NOTE, note);
			values.put(OtherCostTable.COL_CAR, car.getId());

			synchronized (Helper.dbLock) {
				SQLiteDatabase db = Helper.getInstance().getWritableDatabase();
				db.update(OtherCostTable.NAME, values, BaseColumns._ID + "=?",
						new String[] { String.valueOf(id) });
			}
			Helper.getInstance().dataChanged();
		}
	}

	public static OtherCost create(String title, Date date, int mileage,
			float price, Recurrence recurrence, String note, Car car) {
		return create(-1, title, date, mileage, price, recurrence, note, car);
	}
	
	public static OtherCost create(int id, String title, Date date, int mileage,
			float price, Recurrence recurrence, String note, Car car) {
		ContentValues values = new ContentValues();
		if (id != -1) {
			values.put(BaseColumns._ID, id);
		}
		values.put(OtherCostTable.COL_TITLE, title);
		values.put(OtherCostTable.COL_DATE, date.getTime());
		values.put(OtherCostTable.COL_TACHO, mileage);
		values.put(OtherCostTable.COL_PRICE, price);
		values.put(OtherCostTable.COL_REP_INT, recurrence.getInterval()
				.getValue());
		values.put(OtherCostTable.COL_REP_MULTI, recurrence.getMultiplier());
		values.put(OtherCostTable.COL_NOTE, note);
		values.put(OtherCostTable.COL_CAR, car.getId());

		synchronized (Helper.dbLock) {
			SQLiteDatabase db = Helper.getInstance().getWritableDatabase();
			id = (int) db.insert(OtherCostTable.NAME, null, values);
		}
		
		if (id == -1) {
			throw new IllegalArgumentException(
					"An other cost with this ID does already exist!");
		}
		
		Helper.getInstance().dataChanged();

		return new OtherCost(id, title, date, mileage, price, recurrence,
				note, car);
	}

	public static OtherCost[] getAllForCar(Car car, boolean orderDateAsc) {
		ArrayList<OtherCost> others = new ArrayList<OtherCost>();

		synchronized (Helper.dbLock) {
			SQLiteDatabase db = Helper.getInstance().getReadableDatabase();
			Cursor cursor = db.query(OtherCostTable.NAME,
					OtherCostTable.ALL_COLUMNS, OtherCostTable.COL_CAR + "=?",
					new String[] { String.valueOf(car.getId()) }, null, null,
					String.format("%s %s", OtherCostTable.COL_DATE,
							orderDateAsc ? "ASC" : "DESC"));

			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				others.add(new OtherCost(cursor.getInt(0), cursor.getString(1),
						new Date(cursor.getLong(2)), cursor.getInt(3), cursor
								.getFloat(4),
						new Recurrence(RecurrenceInterval.getByValue(cursor
								.getInt(5)), cursor.getInt(6)), cursor
								.getString(7), car));
				cursor.moveToNext();
			}
			cursor.close();
		}

		return others.toArray(new OtherCost[others.size()]);
	}

	public static String[] getAllTitles() {
		ArrayList<String> titles = new ArrayList<String>();

		synchronized (Helper.dbLock) {
			SQLiteDatabase db = Helper.getInstance().getReadableDatabase();
			Cursor cursor = db.rawQuery(String.format(
					"SELECT DISTINCT %s FROM %s ORDER BY %s",
					OtherCostTable.COL_TITLE, OtherCostTable.NAME,
					OtherCostTable.COL_TITLE), null);

			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				titles.add(cursor.getString(0));
				cursor.moveToNext();
			}
			cursor.close();
		}

		return titles.toArray(new String[titles.size()]);
	}
}
