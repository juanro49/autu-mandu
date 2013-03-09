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

import me.kuehle.carreport.util.Strings;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

public class RefuelingTable {
	public static final String NAME = "refuelings";
	
	public static final String COL_DATE = "date";
	public static final String COL_TACHO = "tachometer"; // should be 'mileage'
	public static final String COL_VOLUME = "volume";
	public static final String COL_PRICE = "price";
	public static final String COL_PARTIAL = "partial";
	public static final String COL_NOTE = "note";
	
	public static final String COL_CAR = "cars_id";
	public static final String COL_FUELTYPE = "fueltypes_id";
	
	public static final String[] ALL_COLUMNS = {
		BaseColumns._ID, COL_DATE, COL_TACHO, COL_VOLUME,
		COL_PRICE, COL_PARTIAL, COL_NOTE, COL_CAR, COL_FUELTYPE
	};
	
	private static final String STMT_CREATE = "CREATE TABLE " + NAME + "( "
			+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ COL_DATE + " INTEGER NOT NULL, "
			+ COL_TACHO + " INTEGER NOT NULL, "
			+ COL_VOLUME + " REAL NOT NULL, "
			+ COL_PRICE + " REAL NOT NULL, "
			+ COL_PARTIAL + " INTEGER NOT NULL, "
			+ COL_NOTE + " TEXT NOT NULL, "
			+ COL_CAR + " INTEGER NOT NULL, "
			+ COL_FUELTYPE + " INTEGER DEFAULT NULL, "
			+ "FOREIGN KEY(" + COL_CAR + ") REFERENCES " + CarTable.NAME + "(" + BaseColumns._ID + ") ON UPDATE CASCADE ON DELETE CASCADE, "
	        + "FOREIGN KEY(" + COL_FUELTYPE + ") REFERENCES " + FuelTypeTable.NAME + "(" + BaseColumns._ID + ") ON UPDATE CASCADE ON DELETE SET DEFAULT);";
	
	// Add ON DELETE and ON UPDATE actions to cars_id column.
	private static final String[] STMT_UPGRADE_3 = {
		STMT_CREATE.replaceFirst(NAME, NAME + "2"),
		"INSERT INTO " + NAME + "2 (" + Strings.join(", ", new String[] { BaseColumns._ID, COL_DATE, COL_TACHO, COL_VOLUME, COL_PRICE, COL_PARTIAL, COL_NOTE, COL_CAR }) + ") "
			+ "SELECT " + Strings.join(", ", new String[] { NAME + "." + BaseColumns._ID, COL_DATE, COL_TACHO, COL_VOLUME, COL_PRICE, COL_PARTIAL, COL_NOTE, COL_CAR }) + " "
			+ "FROM " + NAME + " "
			+ "JOIN " + CarTable.NAME + " ON " + CarTable.NAME + "." + BaseColumns._ID + " = " + NAME + "." + COL_CAR + ";",
		"DROP TABLE " + NAME + ";",
		"ALTER TABLE " + NAME + "2 RENAME TO " + NAME + ";"
	};
	
	// Add column fueltypes_id.
	private static final String[] STMT_UPGRADE_6 = {
		STMT_CREATE.replaceFirst(NAME, NAME + "2"),
		"INSERT INTO " + NAME + "2 (" + Strings.join(", ", new String[] { BaseColumns._ID, COL_DATE, COL_TACHO, COL_VOLUME, COL_PRICE, COL_PARTIAL, COL_NOTE, COL_CAR }) + ") "
			+ "SELECT " + Strings.join(", ", new String[] { NAME + "." + BaseColumns._ID, COL_DATE, COL_TACHO, COL_VOLUME, COL_PRICE, COL_PARTIAL, COL_NOTE, COL_CAR }) + " "
			+ "FROM " + NAME + " "
			+ "JOIN " + CarTable.NAME + " ON " + CarTable.NAME + "." + BaseColumns._ID + " = " + NAME + "." + COL_CAR + ";",
		"DROP TABLE " + NAME + ";",
		"ALTER TABLE " + NAME + "2 RENAME TO " + NAME + ";"
	};
	
	public static void onCreate(SQLiteDatabase db) {
		db.execSQL(STMT_CREATE);
	}

	public static void onUpgrade(SQLiteDatabase db, int oldVersion,
			int newVersion) {
		if (oldVersion < 3) {
			for (String stmt : STMT_UPGRADE_3) {
				db.execSQL(stmt);
			}
		}
		
		if (oldVersion < 6) {
			for (String stmt : STMT_UPGRADE_6) {
				db.execSQL(stmt);
			}
		}
	}
}
