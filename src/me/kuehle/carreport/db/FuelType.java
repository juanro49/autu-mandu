package me.kuehle.carreport.db;

import java.util.ArrayList;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

public class FuelType extends AbstractItem {
	private Car car;
	private String name;
	private int tank;

	public FuelType(int id) {
		synchronized (Helper.dbLock) {
			SQLiteDatabase db = Helper.getInstance().getReadableDatabase();
			Cursor cursor = db.query(FuelTypeTable.NAME,
					FuelTypeTable.ALL_COLUMNS, BaseColumns._ID + "=?",
					new String[] { String.valueOf(id) }, null, null, null);
			if (cursor.getCount() != 1) {
				cursor.close();
				throw new IllegalArgumentException(
						"A fuel type with this ID does not exist!");
			} else {
				cursor.moveToFirst();
				this.id = id;
				this.car = new Car(cursor.getInt(1));
				this.name = cursor.getString(2);
				this.tank = cursor.getInt(3);
				cursor.close();
			}
		}
	}

	private FuelType(int id, Car car, String name, int tank) {
		this.id = id;
		this.car = car;
		this.name = name;
		this.tank = tank;
	}

	public Car getCar() {
		return car;
	}

	public void setCar(Car car) {
		this.car = car;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getTank() {
		return tank;
	}

	public void setTank(int tank) {
		this.tank = tank;
	}

	@Override
	public void delete() {
		if (!isDeleted()) {
			synchronized (Helper.dbLock) {
				SQLiteDatabase db = Helper.getInstance().getWritableDatabase();
				db.delete(FuelTypeTable.NAME, BaseColumns._ID + "=?",
						new String[] { String.valueOf(id) });
			}
			Helper.getInstance().dataChanged();
			deleted = true;
		}
	}

	public void save() {
		if (!isDeleted()) {
			ContentValues values = new ContentValues();
			values.put(FuelTypeTable.COL_CAR, car.getId());
			values.put(FuelTypeTable.COL_NAME, name);
			values.put(FuelTypeTable.COL_TANK, tank);

			synchronized (Helper.dbLock) {
				SQLiteDatabase db = Helper.getInstance().getWritableDatabase();
				db.update(FuelTypeTable.NAME, values, BaseColumns._ID + "=?",
						new String[] { String.valueOf(id) });
			}
			Helper.getInstance().dataChanged();
		}
	}

	public static FuelType create(Car car, String name, int tank) {
		return create(-1, car, name, tank);
	}

	public static FuelType create(int id, Car car, String name, int tank) {
		ContentValues values = new ContentValues();
		if (id != -1) {
			values.put(BaseColumns._ID, id);
		}
		values.put(FuelTypeTable.COL_CAR, car.getId());
		values.put(FuelTypeTable.COL_NAME, name);
		values.put(FuelTypeTable.COL_TANK, tank);

		synchronized (Helper.dbLock) {
			SQLiteDatabase db = Helper.getInstance().getWritableDatabase();
			id = (int) db.insert(FuelTypeTable.NAME, null, values);
		}

		if (id == -1) {
			throw new IllegalArgumentException(
					"A fuel type with this ID does already exist!");
		}

		Helper.getInstance().dataChanged();
		return new FuelType(id, car, name, tank);
	}

	public static FuelType[] getAllForCar(Car car) {
		ArrayList<FuelType> fuelTypes = new ArrayList<FuelType>();

		synchronized (Helper.dbLock) {
			SQLiteDatabase db = Helper.getInstance().getReadableDatabase();
			Cursor cursor = db.query(FuelTypeTable.NAME,
					FuelTypeTable.ALL_COLUMNS, FuelTypeTable.COL_CAR + "=?",
					new String[] { String.valueOf(car.getId()) }, null, null,
					null);

			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				fuelTypes.add(new FuelType(cursor.getInt(0), car, cursor
						.getString(2), cursor.getInt(3)));
				cursor.moveToNext();
			}
			cursor.close();
		}

		return fuelTypes.toArray(new FuelType[fuelTypes.size()]);
	}

	public static String[] getAllNames() {
		ArrayList<String> names = new ArrayList<String>();

		synchronized (Helper.dbLock) {
			SQLiteDatabase db = Helper.getInstance().getReadableDatabase();
			Cursor cursor = db.rawQuery(String.format(
					"SELECT DISTINCT %s FROM %s ORDER BY %s",
					FuelTypeTable.COL_NAME, FuelTypeTable.NAME,
					FuelTypeTable.COL_NAME), null);

			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				names.add(cursor.getString(0));
				cursor.moveToNext();
			}
			cursor.close();
		}

		return names.toArray(new String[names.size()]);
	}
}