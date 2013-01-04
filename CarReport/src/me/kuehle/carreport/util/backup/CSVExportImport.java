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
import java.util.HashMap;

import me.kuehle.carreport.db.CarTable;
import me.kuehle.carreport.db.Helper;
import me.kuehle.carreport.db.OtherCostTable;
import me.kuehle.carreport.db.RefuelingTable;
import me.kuehle.carreport.util.CSVWriter;
import me.kuehle.carreport.util.Strings;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.provider.BaseColumns;

public class CSVExportImport {
	public static final String FILE_PREFIX = "carreport_export";
	public static final int EXPORT_SINGLE_FILE = 0;
	public static final int EXPORT_TWO_FILES = 1;
	public static final int EXPORT_THREE_FILES = 2;

	private File dir;

	public CSVExportImport() {
		dir = Environment.getExternalStorageDirectory();
	}

	public boolean export(int option) {
		Helper helper = Helper.getInstance();
		File dir = Environment.getExternalStorageDirectory();
		if (option == EXPORT_SINGLE_FILE) {
			File export = new File(dir, FILE_PREFIX + ".csv");

			// Build SQL select statement
			HashMap<String, String> replacements = new HashMap<String, String>();
			replacements.put(
					"%r_columns",
					Strings.join(new String[] {
							"'Refueling' AS title",
							RefuelingTable.COL_DATE,
							RefuelingTable.COL_TACHO,
							RefuelingTable.COL_VOLUME,
							RefuelingTable.COL_PRICE,
							RefuelingTable.COL_PARTIAL,
							"'0' AS repeat_interval",
							"'1' AS repeat_multiplier",
							RefuelingTable.COL_NOTE,
							CarTable.NAME + "." + CarTable.COL_NAME
									+ " AS carname",
							CarTable.NAME + "." + CarTable.COL_COLOR
									+ " AS carcolor" }, ", "));
			replacements.put(
					"%o_columns",
					Strings.join(new String[] {
							OtherCostTable.COL_TITLE,
							OtherCostTable.COL_DATE,
							OtherCostTable.COL_TACHO,
							"'' AS volume",
							OtherCostTable.COL_PRICE,
							"'0' AS partial",
							OtherCostTable.COL_REP_INT,
							OtherCostTable.COL_REP_MULTI,
							OtherCostTable.COL_NOTE,
							CarTable.NAME + "." + CarTable.COL_NAME
									+ " AS carname",
							CarTable.NAME + "." + CarTable.COL_COLOR
									+ " AS carcolor" }, ", "));
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
		} else if (option == EXPORT_TWO_FILES) {
			File exportRefuelings = new File(dir, FILE_PREFIX
					+ "_refuelings.csv");
			File exportOtherCosts = new File(dir, FILE_PREFIX
					+ "_othercosts.csv");

			// Build SQL select statement for refuelings
			HashMap<String, String> replacementsRefuelings = new HashMap<String, String>();
			replacementsRefuelings.put(
					"%columns",
					Strings.join(new String[] {
							RefuelingTable.COL_DATE,
							RefuelingTable.COL_TACHO,
							RefuelingTable.COL_VOLUME,
							RefuelingTable.COL_PRICE,
							RefuelingTable.COL_PARTIAL,
							RefuelingTable.COL_NOTE,
							CarTable.NAME + "." + CarTable.COL_NAME
									+ " AS carname",
							CarTable.NAME + "." + CarTable.COL_COLOR
									+ " AS carcolor" }, ", "));
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
			replacementsOtherCosts.put(
					"%columns",
					Strings.join(new String[] {
							OtherCostTable.COL_TITLE,
							OtherCostTable.COL_DATE,
							OtherCostTable.COL_TACHO,
							OtherCostTable.COL_PRICE,
							OtherCostTable.COL_REP_INT,
							OtherCostTable.COL_REP_MULTI,
							OtherCostTable.COL_NOTE,
							CarTable.NAME + "." + CarTable.COL_NAME
									+ " AS carname",
							CarTable.NAME + "." + CarTable.COL_COLOR
									+ " AS carcolor" }, ", "));
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
		} else if (option == EXPORT_THREE_FILES) {
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

	public boolean exportFilesExist(int option) {
		if (option == EXPORT_SINGLE_FILE) {
			File export = new File(dir, FILE_PREFIX + ".csv");
			return export.isFile();
		} else if (option == EXPORT_TWO_FILES) {
			File exportRefuelings = new File(dir, FILE_PREFIX
					+ "_refuelings.csv");
			File exportOtherCosts = new File(dir, FILE_PREFIX
					+ "_othercosts.csv");
			return exportRefuelings.isFile() || exportOtherCosts.isFile();
		} else if (option == EXPORT_THREE_FILES) {
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

	public boolean import_() {
		return false;
	}
}
