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

import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

public class FuelTypeTable {
	public static final String NAME = "fueltypes";

	public static final String COL_CAR = "cars_id";
	public static final String COL_NAME = "name";
	public static final String COL_TANK = "tank";

	public static final String[] ALL_COLUMNS = { BaseColumns._ID, COL_CAR,
			COL_NAME, COL_TANK };

	private static final String STMT_CREATE = "CREATE TABLE " + NAME + "( "
			+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ COL_CAR + " INTEGER NOT NULL, "
			+ COL_NAME + " TEXT NOT NULL, "
			+ COL_TANK + " INTEGER NOT NULL, "
	        + "FOREIGN KEY(" + COL_CAR + ") REFERENCES " + CarTable.NAME + "(" + BaseColumns._ID + ") ON UPDATE CASCADE ON DELETE CASCADE);";

	public static void onCreate(SQLiteDatabase db) {
		db.execSQL(STMT_CREATE);
	}

	public static void onUpgrade(SQLiteDatabase db, int oldVersion,
			int newVersion) {
		if (oldVersion < 5) {
			onCreate(db);
		}
	}
}
