package me.kuehle.carreport.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class Helper extends SQLiteOpenHelper {
	public static final String DB_NAME = "data.db";
	public static final int DB_VERSION = 1;

	private static Helper instance = null;

	private Helper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public synchronized void close() {
		super.close();
		instance = null;
	}

	@Override
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		CarTable.onCreate(db);
		RefuelingTable.onCreate(db);
		OtherCostTable.onCreate(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		CarTable.onUpgrade(db, oldVersion, newVersion);
		RefuelingTable.onUpgrade(db, oldVersion, newVersion);
		OtherCostTable.onUpgrade(db, oldVersion, newVersion);
	}

	public static Helper getInstance() {
		return instance;
	}

	public static void init(Context context) {
		instance = new Helper(context);
	}
}
