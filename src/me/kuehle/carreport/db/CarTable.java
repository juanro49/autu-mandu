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
import android.graphics.Color;
import android.provider.BaseColumns;

public class CarTable {
	public static final String NAME = "cars";
	
	public static final String COL_NAME = "name";
	public static final String COL_COLOR = "color";
	public static final String COL_SUSPENDED = "suspended_since";
	
	public static final String[] ALL_COLUMNS = {
		BaseColumns._ID, COL_NAME, COL_COLOR, COL_SUSPENDED
	};
	
	private static final String STMT_CREATE = "CREATE TABLE " + NAME + "( "
			+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ COL_NAME + " TEXT NOT NULL, "
			+ COL_COLOR + " INTEGER NOT NULL, "
			+ COL_SUSPENDED + " INTEGER DEFAULT NULL);";
	private static final String STMT_INSERT_DEFAULT = "INSERT INTO " + NAME
			+ "(" + BaseColumns._ID + ", " + COL_NAME + ", " + COL_COLOR + ")"
			+ " VALUES(1, 'Default Car', " + Color.BLUE + ");";
	
	// Add suspended column.
	private static final String STMT_UPGRADE_3TO4 =
			"ALTER TABLE " + NAME + " ADD COLUMN " + COL_SUSPENDED + " INTEGER DEFAULT NULL;";
	
	public static void onCreate(SQLiteDatabase db) {
		db.execSQL(STMT_CREATE);
		db.execSQL(STMT_INSERT_DEFAULT);
	}

	public static void onUpgrade(SQLiteDatabase db, int oldVersion,
			int newVersion) {
		if(oldVersion <= 3) {
			db.execSQL(STMT_UPGRADE_3TO4);
		}
	}
}