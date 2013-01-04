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

package me.kuehle.carreport.util.backup;

import java.io.File;
import java.util.Date;
import java.util.HashMap;

import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.db.CarTable;
import me.kuehle.carreport.db.Helper;
import me.kuehle.carreport.db.OtherCost;
import me.kuehle.carreport.db.OtherCostTable;
import me.kuehle.carreport.db.Refueling;
import me.kuehle.carreport.db.RefuelingTable;
import me.kuehle.carreport.util.CSVReader;
import me.kuehle.carreport.util.CSVWriter;
import me.kuehle.carreport.util.Recurrence;
import me.kuehle.carreport.util.RecurrenceInterval;
import me.kuehle.carreport.util.Strings;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.provider.BaseColumns;

public class CSVExportImport {
	public static final String FILE_PREFIX = "carreport_export";
	public static final int SINGLE_FILE = 0;
	public static final int TWO_FILES = 1;
	public static final int THREE_FILES = 2;

	private static final String REFUELING_TITLE = "Refueling";

	private File dir;

	public CSVExportImport() {
		dir = Environment.getExternalStorageDirectory();
	}

	public boolean export(int option) {
		Helper helper = Helper.getInstance();
		if (option == SINGLE_FILE) {
			File export = new File(dir, FILE_PREFIX + ".csv");

			// Build SQL select statement
			HashMap<String, String> replacements = new HashMap<String, String>();
			replacements.put("%r_columns", Strings.join(new String[] {
					RefuelingTable.NAME + "." + BaseColumns._ID + " AS "
							+ BaseColumns._ID,
					"'" + REFUELING_TITLE + "' AS " + OtherCostTable.COL_TITLE,
					RefuelingTable.COL_DATE,
					RefuelingTable.COL_TACHO,
					RefuelingTable.COL_VOLUME,
					RefuelingTable.COL_PRICE,
					RefuelingTable.COL_PARTIAL,
					"'0' AS " + OtherCostTable.COL_REP_INT,
					"'1' AS " + OtherCostTable.COL_REP_MULTI,
					RefuelingTable.COL_NOTE,
					CarTable.NAME + "." + BaseColumns._ID + " AS "
							+ CarTable.NAME + BaseColumns._ID,
					CarTable.NAME + "." + CarTable.COL_NAME + " AS "
							+ CarTable.NAME + CarTable.COL_NAME,
					CarTable.NAME + "." + CarTable.COL_COLOR + " AS "
							+ CarTable.NAME + CarTable.COL_COLOR }, ", "));
			replacements.put("%o_columns", Strings.join(new String[] {
					OtherCostTable.NAME + "." + BaseColumns._ID + " AS "
							+ BaseColumns._ID,
					OtherCostTable.COL_TITLE,
					OtherCostTable.COL_DATE,
					OtherCostTable.COL_TACHO,
					"'' AS " + RefuelingTable.COL_VOLUME,
					OtherCostTable.COL_PRICE,
					"'0' AS " + RefuelingTable.COL_PARTIAL,
					OtherCostTable.COL_REP_INT,
					OtherCostTable.COL_REP_MULTI,
					OtherCostTable.COL_NOTE,
					CarTable.NAME + "." + BaseColumns._ID + " AS "
							+ CarTable.NAME + BaseColumns._ID,
					CarTable.NAME + "." + CarTable.COL_NAME + " AS "
							+ CarTable.NAME + CarTable.COL_NAME,
					CarTable.NAME + "." + CarTable.COL_COLOR + " AS "
							+ CarTable.NAME + CarTable.COL_COLOR }, ", "));
			replacements.put("%refuelings", RefuelingTable.NAME);
			replacements.put("%othercosts", OtherCostTable.NAME);
			replacements.put("%cars", CarTable.NAME);
			replacements.put("%r_car_id", RefuelingTable.COL_CAR);
			replacements.put("%o_car_id", OtherCostTable.COL_CAR);
			replacements.put("%id", BaseColumns._ID);
			String sql = Strings.replaceMap("SELECT %r_columns "
					+ "FROM %refuelings "
					+ "JOIN %cars ON %refuelings.%r_car_id = %cars.%id "
					+ "UNION ALL SELECT %o_columns " + "FROM %othercosts "
					+ "JOIN %cars ON %othercosts.%o_car_id = %cars.%id",
					replacements);

			CSVWriter writer = new CSVWriter();
			synchronized (Helper.dbLock) {
				SQLiteDatabase db = helper.getReadableDatabase();
				Cursor cursor = db.rawQuery(sql, null);
				writer.write(cursor, true);
				cursor.close();
			}

			writer.toFile(export);
			return true;
		} else if (option == TWO_FILES) {
			File exportRefuelings = new File(dir, FILE_PREFIX
					+ "_refuelings.csv");
			File exportOtherCosts = new File(dir, FILE_PREFIX
					+ "_othercosts.csv");

			// Build SQL select statement for refuelings
			HashMap<String, String> replacementsRefuelings = new HashMap<String, String>();
			replacementsRefuelings.put("%columns", Strings.join(new String[] {
					RefuelingTable.NAME + "." + BaseColumns._ID + " AS "
							+ BaseColumns._ID,
					RefuelingTable.COL_DATE,
					RefuelingTable.COL_TACHO,
					RefuelingTable.COL_VOLUME,
					RefuelingTable.COL_PRICE,
					RefuelingTable.COL_PARTIAL,
					RefuelingTable.COL_NOTE,
					CarTable.NAME + "." + BaseColumns._ID + " AS "
							+ CarTable.NAME + BaseColumns._ID,
					CarTable.NAME + "." + CarTable.COL_NAME + " AS "
							+ CarTable.NAME + CarTable.COL_NAME,
					CarTable.NAME + "." + CarTable.COL_COLOR + " AS "
							+ CarTable.NAME + CarTable.COL_COLOR }, ", "));
			replacementsRefuelings.put("%refuelings", RefuelingTable.NAME);
			replacementsRefuelings.put("%cars", CarTable.NAME);
			replacementsRefuelings.put("%car_id", RefuelingTable.COL_CAR);
			replacementsRefuelings.put("%id", BaseColumns._ID);
			String sqlRefuelings = Strings.replaceMap("SELECT %columns "
					+ "FROM %refuelings "
					+ "JOIN %cars ON %refuelings.%car_id = %cars.%id ",
					replacementsRefuelings);

			// Build SQL select statement for other costs
			HashMap<String, String> replacementsOtherCosts = new HashMap<String, String>();
			replacementsOtherCosts.put("%columns", Strings.join(new String[] {
					OtherCostTable.NAME + "." + BaseColumns._ID + " AS "
							+ BaseColumns._ID,
					OtherCostTable.COL_TITLE,
					OtherCostTable.COL_DATE,
					OtherCostTable.COL_TACHO,
					OtherCostTable.COL_PRICE,
					OtherCostTable.COL_REP_INT,
					OtherCostTable.COL_REP_MULTI,
					OtherCostTable.COL_NOTE,
					CarTable.NAME + "." + BaseColumns._ID + " AS "
							+ CarTable.NAME + BaseColumns._ID,
					CarTable.NAME + "." + CarTable.COL_NAME + " AS "
							+ CarTable.NAME + CarTable.COL_NAME,
					CarTable.NAME + "." + CarTable.COL_COLOR + " AS "
							+ CarTable.NAME + CarTable.COL_COLOR }, ", "));
			replacementsOtherCosts.put("%othercosts", OtherCostTable.NAME);
			replacementsOtherCosts.put("%cars", CarTable.NAME);
			replacementsOtherCosts.put("%car_id", OtherCostTable.COL_CAR);
			replacementsOtherCosts.put("%id", BaseColumns._ID);
			String sqlOtherCosts = Strings.replaceMap("SELECT %columns "
					+ "FROM %othercosts "
					+ "JOIN %cars ON %othercosts.%car_id = %cars.%id",
					replacementsOtherCosts);

			CSVWriter writerRefuelings = new CSVWriter();
			CSVWriter writerOtherCosts = new CSVWriter();
			synchronized (Helper.dbLock) {
				SQLiteDatabase db = helper.getReadableDatabase();
				Cursor cursor = db.rawQuery(sqlRefuelings, null);
				writerRefuelings.write(cursor, true);
				cursor.close();
				cursor = db.rawQuery(sqlOtherCosts, null);
				writerOtherCosts.write(cursor, true);
				cursor.close();
			}

			writerRefuelings.toFile(exportRefuelings);
			writerOtherCosts.toFile(exportOtherCosts);
			return true;
		} else if (option == THREE_FILES) {
			File exportCars = new File(dir, FILE_PREFIX + "_cars.csv");
			File exportRefuelings = new File(dir, FILE_PREFIX
					+ "_refuelings.csv");
			File exportOtherCosts = new File(dir, FILE_PREFIX
					+ "_othercosts.csv");

			CSVWriter writerCars = new CSVWriter();
			CSVWriter writerRefuelings = new CSVWriter();
			CSVWriter writerOtherCosts = new CSVWriter();
			synchronized (Helper.dbLock) {
				SQLiteDatabase db = helper.getReadableDatabase();
				Cursor cursor = db.query(CarTable.NAME, null, null, null, null,
						null, null);
				writerCars.write(cursor, true);
				cursor.close();
				cursor = db.query(RefuelingTable.NAME, null, null, null, null,
						null, null);
				writerRefuelings.write(cursor, true);
				cursor.close();
				cursor = db.query(OtherCostTable.NAME, null, null, null, null,
						null, null);
				writerOtherCosts.write(cursor, true);
				cursor.close();
			}

			writerCars.toFile(exportCars);
			writerRefuelings.toFile(exportRefuelings);
			writerOtherCosts.toFile(exportOtherCosts);
			return true;
		} else {
			return false;
		}
	}

	public boolean canExport() {
		return dir.canWrite();
	}

	public boolean canImport() {
		return false;
	}

	public boolean allExportFilesExist(int option) {
		if (option == SINGLE_FILE) {
			File export = new File(dir, FILE_PREFIX + ".csv");
			return export.isFile();
		} else if (option == TWO_FILES) {
			File exportRefuelings = new File(dir, FILE_PREFIX
					+ "_refuelings.csv");
			File exportOtherCosts = new File(dir, FILE_PREFIX
					+ "_othercosts.csv");
			return exportRefuelings.isFile() && exportOtherCosts.isFile();
		} else if (option == THREE_FILES) {
			File exportCars = new File(dir, FILE_PREFIX + "_cars.csv");
			File exportRefuelings = new File(dir, FILE_PREFIX
					+ "_refuelings.csv");
			File exportOtherCosts = new File(dir, FILE_PREFIX
					+ "_othercosts.csv");
			return exportCars.isFile() && exportRefuelings.isFile()
					&& exportOtherCosts.isFile();
		} else {
			return false;
		}
	}

	public boolean anyExportFileExist(int option) {
		if (option == SINGLE_FILE) {
			File export = new File(dir, FILE_PREFIX + ".csv");
			return export.isFile();
		} else if (option == TWO_FILES) {
			File exportRefuelings = new File(dir, FILE_PREFIX
					+ "_refuelings.csv");
			File exportOtherCosts = new File(dir, FILE_PREFIX
					+ "_othercosts.csv");
			return exportRefuelings.isFile() || exportOtherCosts.isFile();
		} else if (option == THREE_FILES) {
			File exportCars = new File(dir, FILE_PREFIX + "_cars.csv");
			File exportRefuelings = new File(dir, FILE_PREFIX
					+ "_refuelings.csv");
			File exportOtherCosts = new File(dir, FILE_PREFIX
					+ "_othercosts.csv");
			return exportCars.isFile() || exportRefuelings.isFile()
					|| exportOtherCosts.isFile();
		} else {
			return false;
		}
	}

	public boolean import_(int option) {
		if (!allExportFilesExist(option)) {
			return false;
		}

		boolean errors = false;
		if (option == SINGLE_FILE) {
			File export = new File(dir, FILE_PREFIX + ".csv");
			CSVReader reader = CSVReader.fromFile(export, true);

			for (int i = 0; i < reader.getRowCount(); i++) {
				try {
					importCar(reader, i, CarTable.NAME);
					if (reader.getString(i, OtherCostTable.COL_TITLE).equals(
							REFUELING_TITLE)) {
						importRefueling(reader, i);
					} else {
						importOtherCost(reader, i);
					}
				} catch (Exception e) {
					errors = true;
				}
			}
		} else if (option == TWO_FILES) {
			File exportRefuelings = new File(dir, FILE_PREFIX
					+ "_refuelings.csv");
			File exportOtherCosts = new File(dir, FILE_PREFIX
					+ "_othercosts.csv");
			CSVReader readerRefuelings = CSVReader.fromFile(exportRefuelings,
					true);
			CSVReader readerOtherCosts = CSVReader.fromFile(exportOtherCosts,
					true);

			for (int i = 0; i < readerRefuelings.getRowCount(); i++) {
				try {
					importCar(readerRefuelings, i, CarTable.NAME);
					importRefueling(readerRefuelings, i);
				} catch (Exception e) {
					errors = true;
				}
			}
			for (int i = 0; i < readerOtherCosts.getRowCount(); i++) {
				try {
					importCar(readerOtherCosts, i, CarTable.NAME);
					importOtherCost(readerOtherCosts, i);
				} catch (Exception e) {
					errors = true;
				}
			}
		} else if (option == THREE_FILES) {
			File exportCars = new File(dir, FILE_PREFIX + "_cars.csv");
			File exportRefuelings = new File(dir, FILE_PREFIX
					+ "_refuelings.csv");
			File exportOtherCosts = new File(dir, FILE_PREFIX
					+ "_othercosts.csv");
			CSVReader readerCars = CSVReader.fromFile(exportCars, true);
			CSVReader readerRefuelings = CSVReader.fromFile(exportRefuelings,
					true);
			CSVReader readerOtherCosts = CSVReader.fromFile(exportOtherCosts,
					true);

			for (int i = 0; i < readerCars.getRowCount(); i++) {
				try {
					importCar(readerCars, i, "");
				} catch (Exception e) {
					errors = true;
				}
			}
			for (int i = 0; i < readerRefuelings.getRowCount(); i++) {
				try {
					importRefueling(readerRefuelings, i);
				} catch (Exception e) {
					errors = true;
				}
			}
			for (int i = 0; i < readerOtherCosts.getRowCount(); i++) {
				try {
					importOtherCost(readerOtherCosts, i);
				} catch (Exception e) {
					errors = true;
				}
			}
		} else {
			errors = true;
		}

		return !errors;
	}

	private void importCar(CSVReader reader, int row, String titlePrefix) {
		int id = reader.getInt(row, titlePrefix + BaseColumns._ID);
		String name = reader.getString(row, titlePrefix + CarTable.COL_NAME);
		int color = reader.getInt(row, titlePrefix + CarTable.COL_COLOR);

		try {
			Car car = new Car(id);
			car.setName(name);
			car.setColor(color);
			car.save();
		} catch (IllegalArgumentException e) {
			Car.create(id, name, color);
		}
	}

	private void importRefueling(CSVReader reader, int row) {
		int id = reader.getInt(row, BaseColumns._ID);
		Date date = new Date(reader.getLong(row, RefuelingTable.COL_DATE));
		int mileage = reader.getInt(row, RefuelingTable.COL_TACHO);
		float volume = reader.getFloat(row, RefuelingTable.COL_VOLUME);
		float price = reader.getFloat(row, RefuelingTable.COL_PRICE);
		boolean partial = reader.getInt(row, RefuelingTable.COL_PARTIAL) > 0;
		String note = reader.getString(row, RefuelingTable.COL_NOTE);
		int carID = reader.getInt(row, CarTable.NAME + BaseColumns._ID);

		Car car = new Car(carID);
		try {
			Refueling refueling = new Refueling(id);
			refueling.setDate(date);
			refueling.setMileage(mileage);
			refueling.setVolume(volume);
			refueling.setPrice(price);
			refueling.setPartial(partial);
			refueling.setNote(note);
			refueling.setCar(car);
			refueling.save();
		} catch (IllegalArgumentException e) {
			Refueling.create(id, date, mileage, volume, price, partial, note,
					car);
		}
	}

	private void importOtherCost(CSVReader reader, int row) {
		int id = reader.getInt(row, BaseColumns._ID);
		String title = reader.getString(row, OtherCostTable.COL_TITLE);
		Date date = new Date(reader.getLong(row, OtherCostTable.COL_DATE));
		int mileage = reader.getInt(row, OtherCostTable.COL_TACHO);
		float price = reader.getFloat(row, OtherCostTable.COL_PRICE);
		int repeatInterval = reader.getInt(row, OtherCostTable.COL_REP_INT);
		int repeatMultiplier = reader.getInt(row, OtherCostTable.COL_REP_MULTI);
		String note = reader.getString(row, OtherCostTable.COL_NOTE);
		int carID = reader.getInt(row, CarTable.NAME + BaseColumns._ID);

		Recurrence recurrence = new Recurrence(
				RecurrenceInterval.getByValue(repeatInterval), repeatMultiplier);
		Car car = new Car(carID);
		try {
			OtherCost otherCost = new OtherCost(id);
			otherCost.setTitle(title);
			otherCost.setDate(date);
			otherCost.setMileage(mileage);
			otherCost.setPrice(price);
			otherCost.setRecurrence(recurrence);
			otherCost.setNote(note);
			otherCost.setCar(car);
			otherCost.save();
		} catch (IllegalArgumentException e) {
			OtherCost.create(id, title, date, mileage, price, recurrence, note,
					car);
		}
	}
}
