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

public class Refueling extends AbstractItem {
	private Date date;
	private int mileage;
	private float volume;
	private float price;
	private boolean partial;
	private String note;
	private Car car;
	private FuelType fuelType;

	public Refueling(int id) {
		Helper helper = Helper.getInstance();
		synchronized (Helper.dbLock) {
			SQLiteDatabase db = helper.getReadableDatabase();
			Cursor cursor = db.query(RefuelingTable.NAME,
					RefuelingTable.ALL_COLUMNS, BaseColumns._ID + "=?",
					new String[] { String.valueOf(id) }, null, null, null);
			if (cursor.getCount() != 1) {
				cursor.close();
				throw new IllegalArgumentException(
						"A refueling with this ID does not exist!");
			} else {
				cursor.moveToFirst();
				this.id = id;
				this.date = new Date(cursor.getLong(1));
				this.mileage = cursor.getInt(2);
				this.volume = cursor.getFloat(3);
				this.price = cursor.getFloat(4);
				this.partial = cursor.getInt(5) > 0;
				this.note = cursor.getString(6);
				this.car = new Car(cursor.getInt(7));
				this.fuelType = cursor.isNull(8) ? null : new FuelType(
						cursor.getInt(8));
				cursor.close();
			}
		}
	}

	private Refueling(int id, Date date, int mileage, float volume,
			float price, boolean partial, String note, Car car,
			FuelType fuelType) {
		this.id = id;
		this.date = date;
		this.mileage = mileage;
		this.volume = volume;
		this.price = price;
		this.partial = partial;
		this.note = note;
		this.car = car;
		this.fuelType = fuelType;
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

	public float getVolume() {
		return volume;
	}

	public void setVolume(float volume) {
		this.volume = volume;
	}

	public float getPrice() {
		return price;
	}

	public void setPrice(float price) {
		this.price = price;
	}

	public boolean isPartial() {
		return partial;
	}

	public void setPartial(boolean partial) {
		this.partial = partial;
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

	public FuelType getFuelType() {
		return fuelType;
	}

	public void setFuelType(FuelType fuelType) {
		this.fuelType = fuelType;
	}

	public void delete() {
		if (!isDeleted()) {
			Helper helper = Helper.getInstance();
			synchronized (Helper.dbLock) {
				SQLiteDatabase db = helper.getWritableDatabase();
				db.delete(RefuelingTable.NAME, BaseColumns._ID + "=?",
						new String[] { String.valueOf(id) });
			}
			helper.dataChanged();
			deleted = true;
		}
	}

	public void save() {
		if (!isDeleted()) {
			ContentValues values = new ContentValues();
			values.put(RefuelingTable.COL_DATE, date.getTime());
			values.put(RefuelingTable.COL_TACHO, mileage);
			values.put(RefuelingTable.COL_VOLUME, volume);
			values.put(RefuelingTable.COL_PRICE, price);
			values.put(RefuelingTable.COL_PARTIAL, partial);
			values.put(RefuelingTable.COL_NOTE, note);
			values.put(RefuelingTable.COL_CAR, car.getId());
			values.put(RefuelingTable.COL_FUELTYPE, fuelType == null ? null
					: fuelType.getId());

			Helper helper = Helper.getInstance();
			synchronized (Helper.dbLock) {
				SQLiteDatabase db = helper.getWritableDatabase();
				db.update(RefuelingTable.NAME, values, BaseColumns._ID + "=?",
						new String[] { String.valueOf(id) });
			}
			helper.dataChanged();
		}
	}

	public static Refueling create(Date date, int mileage, float volume,
			float price, boolean partial, String note, Car car,
			FuelType fuelType) {
		return create(-1, date, mileage, volume, price, partial, note, car,
				fuelType);
	}

	public static Refueling create(int id, Date date, int mileage,
			float volume, float price, boolean partial, String note, Car car,
			FuelType fuelType) {
		ContentValues values = new ContentValues();
		if (id != -1) {
			values.put(BaseColumns._ID, id);
		}
		values.put(RefuelingTable.COL_DATE, date.getTime());
		values.put(RefuelingTable.COL_TACHO, mileage);
		values.put(RefuelingTable.COL_VOLUME, volume);
		values.put(RefuelingTable.COL_PRICE, price);
		values.put(RefuelingTable.COL_PARTIAL, partial);
		values.put(RefuelingTable.COL_NOTE, note);
		values.put(RefuelingTable.COL_CAR, car.getId());
		values.put(RefuelingTable.COL_FUELTYPE, fuelType == null ? null
				: fuelType.getId());

		Helper helper = Helper.getInstance();
		synchronized (Helper.dbLock) {
			SQLiteDatabase db = helper.getWritableDatabase();
			id = (int) db.insert(RefuelingTable.NAME, null, values);
		}

		if (id == -1) {
			throw new IllegalArgumentException(
					"A refueling with this ID does already exist!");
		}

		helper.dataChanged();

		return new Refueling(id, date, mileage, volume, price, partial, note,
				car, fuelType);
	}

	public static Refueling[] getAllForCar(Car car, boolean orderDateAsc) {
		ArrayList<Refueling> refuelings = new ArrayList<Refueling>();

		Helper helper = Helper.getInstance();
		synchronized (Helper.dbLock) {
			SQLiteDatabase db = helper.getReadableDatabase();
			Cursor cursor = db.query(RefuelingTable.NAME,
					RefuelingTable.ALL_COLUMNS, RefuelingTable.COL_CAR + "=?",
					new String[] { String.valueOf(car.getId()) }, null, null,
					String.format("%s %s", RefuelingTable.COL_DATE,
							orderDateAsc ? "ASC" : "DESC"));

			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				FuelType fuelType = cursor.isNull(8) ? null : new FuelType(
						cursor.getInt(8));
				refuelings.add(new Refueling(cursor.getInt(0), new Date(cursor
						.getLong(1)), cursor.getInt(2), cursor.getFloat(3),
						cursor.getFloat(4), cursor.getInt(5) > 0, cursor
								.getString(6), car, fuelType));
				cursor.moveToNext();
			}
			cursor.close();
		}

		return refuelings.toArray(new Refueling[refuelings.size()]);
	}

	public static Refueling[] getAllForCar(Car car, FuelType fuelType,
			boolean orderDateAsc) {
		ArrayList<Refueling> refuelings = new ArrayList<Refueling>();

		String selection = RefuelingTable.COL_CAR + " = ?";
		String[] selectionArgs;
		if (fuelType == null) {
			selection += " AND " + RefuelingTable.COL_FUELTYPE + " IS NULL";
			selectionArgs = new String[] { String.valueOf(car.getId()) };
		} else {
			selection += " AND " + RefuelingTable.COL_FUELTYPE + " = ?";
			selectionArgs = new String[] { String.valueOf(car.getId()),
					String.valueOf(fuelType.getId()) };
		}

		Helper helper = Helper.getInstance();
		synchronized (Helper.dbLock) {
			SQLiteDatabase db = helper.getReadableDatabase();
			Cursor cursor = db.query(RefuelingTable.NAME,
					RefuelingTable.ALL_COLUMNS, selection, selectionArgs, null,
					null, String.format("%s %s", RefuelingTable.COL_DATE,
							orderDateAsc ? "ASC" : "DESC"));

			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				refuelings.add(new Refueling(cursor.getInt(0), new Date(cursor
						.getLong(1)), cursor.getInt(2), cursor.getFloat(3),
						cursor.getFloat(4), cursor.getInt(5) > 0, cursor
								.getString(6), car, fuelType));
				cursor.moveToNext();
			}
			cursor.close();
		}

		return refuelings.toArray(new Refueling[refuelings.size()]);
	}

	public static int getCount() {
		int count;
		Helper helper = Helper.getInstance();
		synchronized (Helper.dbLock) {
			SQLiteDatabase db = helper.getReadableDatabase();
			Cursor cursor = db.rawQuery("SELECT count(*) FROM "
					+ RefuelingTable.NAME, null);
			cursor.moveToFirst();
			count = cursor.getInt(0);
			cursor.close();
		}
		return count;
	}

	public static Refueling getFirst() {
		Refueling fuel;
		Helper helper = Helper.getInstance();
		synchronized (Helper.dbLock) {
			SQLiteDatabase db = helper.getReadableDatabase();
			Cursor cursor = db
					.rawQuery("SELECT * FROM " + RefuelingTable.NAME
							+ " ORDER BY " + RefuelingTable.COL_DATE
							+ " LIMIT 1", null);

			if (cursor.getCount() != 1) {
				return null;
			}
			cursor.moveToFirst();
			FuelType fuelType = cursor.isNull(8) ? null : new FuelType(
					cursor.getInt(8));
			fuel = new Refueling(cursor.getInt(0), new Date(cursor.getLong(1)),
					cursor.getInt(2), cursor.getFloat(3), cursor.getFloat(4),
					cursor.getInt(5) > 0, cursor.getString(6), new Car(
							cursor.getInt(7)), fuelType);
			cursor.close();
		}

		return fuel;
	}

	public static Refueling getLast() {
		Refueling fuel;
		Helper helper = Helper.getInstance();
		synchronized (Helper.dbLock) {
			SQLiteDatabase db = helper.getReadableDatabase();
			Cursor cursor = db.rawQuery("SELECT * FROM " + RefuelingTable.NAME
					+ " ORDER BY " + RefuelingTable.COL_DATE + " DESC LIMIT 1",
					null);

			if (cursor.getCount() != 1) {
				return null;
			}
			cursor.moveToFirst();
			FuelType fuelType = cursor.isNull(8) ? null : new FuelType(
					cursor.getInt(8));
			fuel = new Refueling(cursor.getInt(0), new Date(cursor.getLong(1)),
					cursor.getInt(2), cursor.getFloat(3), cursor.getFloat(4),
					cursor.getInt(5) > 0, cursor.getString(6), new Car(
							cursor.getInt(7)), fuelType);
			cursor.close();
		}

		return fuel;
	}
}
