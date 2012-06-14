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

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

public class OtherCost extends AbstractItem {
	private String title;
	private Date date;
	private int tachometer;
	private float price;
	private String note;
	private Car car;

	public OtherCost(int id) {
		Helper helper = Helper.getInstance();
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(OtherCostTable.NAME, null,
				BaseColumns._ID + "=?",
				new String[] { String.valueOf(id) }, null, null, null);
		if (cursor.getCount() != 1) {
			cursor.close();
			throw new IllegalArgumentException(
					"A fuel with this ID does not exist!");
		} else {
			cursor.moveToFirst();
			this.id = id;
			this.title = cursor.getString(1);
			this.date = new Date(cursor.getLong(2));
			this.tachometer = cursor.getInt(3);
			this.price = cursor.getFloat(4);
			this.note = cursor.getString(5);
			this.car = new Car(cursor.getInt(6));
			cursor.close();
		}
	}

	private OtherCost(int id, String title, Date date, int tachometer,
			float price, String note, Car car) {
		this.id = id;
		this.title = title;
		this.date = date;
		this.tachometer = tachometer;
		this.price = price;
		this.note = note;
		this.car = car;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
		save();
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
		save();
	}

	public int getTachometer() {
		return tachometer;
	}

	public void setTachometer(int tachometer) {
		this.tachometer = tachometer;
		save();
	}

	public float getPrice() {
		return price;
	}

	public void setPrice(float price) {
		this.price = price;
		save();
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
		save();
	}

	public Car getCar() {
		return car;
	}

	public void setCar(Car car) {
		this.car = car;
		save();
	}

	public void delete() {
		if (!isDeleted()) {
			Helper helper = Helper.getInstance();
			SQLiteDatabase db = helper.getWritableDatabase();
			db.delete(OtherCostTable.NAME, BaseColumns._ID + "=?",
					new String[] { String.valueOf(id) });
			deleted = true;
		}
	}

	private void save() {
		if (!isDeleted()) {
			Helper helper = Helper.getInstance();
			SQLiteDatabase db = helper.getWritableDatabase();

			ContentValues values = new ContentValues();
			values.put(OtherCostTable.COL_TITLE, title);
			values.put(OtherCostTable.COL_DATE, date.getTime());
			values.put(OtherCostTable.COL_TACHO, tachometer);
			values.put(OtherCostTable.COL_PRICE, price);
			values.put(OtherCostTable.COL_NOTE, note);
			values.put(OtherCostTable.COL_CAR, car.getId());
			db.update(OtherCostTable.NAME, values,
					BaseColumns._ID + "=?",
					new String[] { String.valueOf(id) });
		}
	}

	public static OtherCost create(String title, Date date, int tachometer,
			float price, String note, Car car) {
		Helper helper = Helper.getInstance();
		SQLiteDatabase db = helper.getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(OtherCostTable.COL_TITLE, title);
		values.put(OtherCostTable.COL_DATE, date.getTime());
		values.put(OtherCostTable.COL_TACHO, tachometer);
		values.put(OtherCostTable.COL_PRICE, price);
		values.put(OtherCostTable.COL_NOTE, note);
		values.put(OtherCostTable.COL_CAR, car.getId());
		int id = (int) db.insert(OtherCostTable.NAME, null, values);

		return new OtherCost(id, title, date, tachometer, price, note, car);
	}

	public static OtherCost[] getAllForCar(Car car, boolean orderDateAsc) {
		ArrayList<OtherCost> others = new ArrayList<OtherCost>();

		Helper helper = Helper.getInstance();
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(OtherCostTable.NAME, null,
				OtherCostTable.COL_CAR + "=?", new String[] { String
						.valueOf(car.getId()) }, null, null, String.format(
						"%s %s", OtherCostTable.COL_DATE, orderDateAsc ? "ASC"
								: "DESC"));

		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			others.add(new OtherCost(cursor.getInt(0), cursor.getString(1),
					new Date(cursor.getLong(2)), cursor.getInt(3), cursor
							.getFloat(4), cursor.getString(5), car));
			cursor.moveToNext();
		}
		cursor.close();

		return others.toArray(new OtherCost[others.size()]);
	}

	public static String[] getAllTitles() {
		ArrayList<String> titles = new ArrayList<String>();

		Helper helper = Helper.getInstance();
		SQLiteDatabase db = helper.getReadableDatabase();
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

		return titles.toArray(new String[titles.size()]);
	}
}
