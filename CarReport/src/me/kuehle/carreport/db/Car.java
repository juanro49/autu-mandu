package me.kuehle.carreport.db;

import java.util.ArrayList;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class Car extends AbstractItem {
	private String name;
	private int color;

	public Car(int id) {
		Helper helper = Helper.getInstance();
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(CarTable.NAME, null, CarTable.COL_ID + "=?",
				new String[] { String.valueOf(id) }, null, null, null);
		if (cursor.getCount() != 1) {
			cursor.close();
			throw new IllegalArgumentException(
					"A car with this ID does not exist!");
		} else {
			cursor.moveToFirst();
			this.id = id;
			this.name = cursor.getString(1);
			this.color = cursor.getInt(2);
			cursor.close();
		}
	}

	private Car(int id, String name, int color) {
		this.id = id;
		this.name = name;
		this.color = color;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		save();
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
		save();
	}

	public void delete() {
		if (getCount() == 1) {
			throw new RuntimeException("The last car cannot be deleted!");
		} else if (!isDeleted()) {
			Helper helper = Helper.getInstance();
			SQLiteDatabase db = helper.getWritableDatabase();
			db.delete(CarTable.NAME, CarTable.COL_ID + "=?",
					new String[] { String.valueOf(id) });
			deleted = true;
		}
	}

	private void save() {
		if (!isDeleted()) {
			Helper helper = Helper.getInstance();
			SQLiteDatabase db = helper.getWritableDatabase();

			ContentValues values = new ContentValues();
			values.put(CarTable.COL_NAME, name);
			values.put(CarTable.COL_COLOR, color);
			db.update(CarTable.NAME, values, CarTable.COL_ID + "=?",
					new String[] { String.valueOf(id) });
		}
	}

	public static Car create(String name, int color) {
		Helper helper = Helper.getInstance();
		SQLiteDatabase db = helper.getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(CarTable.COL_NAME, name);
		values.put(CarTable.COL_COLOR, color);
		int id = (int) db.insert(CarTable.NAME, null, values);

		return new Car(id, name, color);
	}

	public static Car[] getAll() {
		ArrayList<Car> cars = new ArrayList<Car>();

		Helper helper = Helper.getInstance();
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(CarTable.NAME, null, null, null, null, null,
				null);

		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			cars.add(new Car(cursor.getInt(0), cursor.getString(1),
					cursor.getInt(2)));
			cursor.moveToNext();
		}
		cursor.close();

		return cars.toArray(new Car[cars.size()]);
	}

	public static int getCount() {
		Helper helper = Helper.getInstance();
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.rawQuery("SELECT count(*) FROM " + CarTable.NAME,
				null);
		cursor.moveToFirst();
		int count = cursor.getInt(0);
		cursor.close();
		return count;
	}
}
