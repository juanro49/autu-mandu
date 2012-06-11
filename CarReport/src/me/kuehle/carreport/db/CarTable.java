package me.kuehle.carreport.db;

import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;

public class CarTable {
	public static final String NAME = "cars";
	
	public static final String COL_ID = "_id";
	public static final String COL_NAME = "name";
	public static final String COL_COLOR = "color";
	
	private static final String STMT_CREATE = "CREATE TABLE " + NAME + "( "
			+ COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ COL_NAME + " TEXT NOT NULL, "
			+ COL_COLOR + " INTEGER NOT NULL);";
	private static final String STMT_INSERT_DEFAULT = "INSERT INTO " + NAME
			+ " VALUES(1, 'Default Car', " + Color.BLUE + ");";
	
	public static void onCreate(SQLiteDatabase db) {
		db.execSQL(STMT_CREATE);
		db.execSQL(STMT_INSERT_DEFAULT);
	}

	public static void onUpgrade(SQLiteDatabase db, int oldVersion,
			int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + NAME);
		onCreate(db);
	}
}
