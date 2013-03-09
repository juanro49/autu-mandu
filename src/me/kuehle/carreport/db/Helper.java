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

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.util.backup.Dropbox;
import android.app.backup.BackupManager;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class Helper extends SQLiteOpenHelper {
	public static final String DB_NAME = "data.db";
	public static final int DB_VERSION = 6;
	public static final Object[] dbLock = new Object[0];

	private static Helper instance = null;

	private Context context;

	private Helper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		this.context = context;
	}

	@Override
	public synchronized void close() {
		super.close();
		instance = null;
	}

	public void dataChanged() {
		BackupManager backupManager = new BackupManager(context);
		backupManager.dataChanged();
		if (new Preferences(context).isSyncOnChange()) {
			Dropbox.getInstance().synchronize();
		}
	}

	@Override
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		CarTable.onCreate(db);
		FuelTypeTable.onCreate(db);
		RefuelingTable.onCreate(db);
		OtherCostTable.onCreate(db);
	}

	@Override
	public void onOpen(SQLiteDatabase db) {
		super.onOpen(db);
		if (!db.isReadOnly()) {
			db.execSQL("PRAGMA foreign_keys=ON;");
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		CarTable.onUpgrade(db, oldVersion, newVersion);
		FuelTypeTable.onUpgrade(db, oldVersion, newVersion);
		RefuelingTable.onUpgrade(db, oldVersion, newVersion);
		OtherCostTable.onUpgrade(db, oldVersion, newVersion);
	}

	public void reinitialize() {
		init(context);
	}

	public static Helper getInstance() {
		return instance;
	}

	public static void init(Context context) {
		instance = new Helper(context);
	}
}
