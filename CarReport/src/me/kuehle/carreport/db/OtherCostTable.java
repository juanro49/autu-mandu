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

public class OtherCostTable {
	public static final String NAME = "othercosts";
	
	public static final String COL_TITLE = "title";
	public static final String COL_DATE = "date";
	public static final String COL_TACHO = "tachometer";
	public static final String COL_PRICE = "price";
	public static final String COL_REP_INT = "repeat_interval";
	public static final String COL_REP_MULTI = "repeat_multiplier";
	public static final String COL_NOTE = "note";
	
	public static final String COL_CAR = "cars_id";
	
	public static final String[] ALL_COLUMNS = {
		BaseColumns._ID, COL_TITLE, COL_DATE, COL_TACHO,
		COL_PRICE, COL_REP_INT, COL_REP_MULTI, COL_NOTE,
		COL_CAR
	};
	
	private static final String STMT_CREATE = "CREATE TABLE " + NAME + "( "
			+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ COL_TITLE + " TEXT NOT NULL,"
			+ COL_DATE + " INTEGER NOT NULL,"
			+ COL_TACHO + " INTEGER NOT NULL DEFAULT -1,"
			+ COL_PRICE + " REAL NOT NULL,"
			+ COL_REP_INT + " INTEGER NOT NULL DEFAULT 0,"
			+ COL_REP_MULTI + " INTEGER NOT NULL DEFAULT 1,"
			+ COL_NOTE + " TEXT NOT NULL,"
			+ COL_CAR + " INTEGER NOT NULL,"
			+ "FOREIGN KEY(" + COL_CAR + ") REFERENCES " + CarTable.NAME + "(" + BaseColumns._ID + "));";
	
	private static final String STMT_UPGRADE_1TO2_1 = "ALTER TABLE " + NAME
			+ " ADD COLUMN " + COL_REP_INT + " INTEGER NOT NULL DEFAULT 0;";
	private static final String STMT_UPGRADE_1TO2_2 = "ALTER TABLE " + NAME
			+ " ADD COLUMN " + COL_REP_MULTI + " INTEGER NOT NULL DEFAULT 1;";
	
	public static void onCreate(SQLiteDatabase db) {
		db.execSQL(STMT_CREATE);
	}

	public static void onUpgrade(SQLiteDatabase db, int oldVersion,
			int newVersion) {
		if (oldVersion == 1 && newVersion == 2) {
			db.execSQL(STMT_UPGRADE_1TO2_1);
			db.execSQL(STMT_UPGRADE_1TO2_2);
		}
	}
}
