package me.kuehle.carreport.db;

import android.database.sqlite.SQLiteDatabase;

public class RefuelingTable {
	public static final String NAME = "refuelings";
	
	public static final String COL_ID = "_id";
	public static final String COL_DATE = "date";
	public static final String COL_TACHO = "tachometer";
	public static final String COL_VOLUME = "volume";
	public static final String COL_PRICE = "price";
	public static final String COL_PARTIAL = "partial";
	public static final String COL_NOTE = "note";
	
	public static final String COL_CAR = "cars_id";
	
	private static final String STMT_CREATE = "create table " + NAME + "( "
			+ COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ COL_DATE + " INTEGER NOT NULL,"
			+ COL_TACHO + " INTEGER NOT NULL,"
			+ COL_VOLUME + " REAL NOT NULL,"
			+ COL_PRICE + " REAL NOT NULL,"
			+ COL_PARTIAL + " INTEGER NOT NULL,"
			+ COL_NOTE + " TEXT NOT NULL,"
			+ COL_CAR + " INTEGER NOT NULL,"
			+ "FOREIGN KEY(" + COL_CAR + ") REFERENCES " + CarTable.NAME + "(" + CarTable.COL_ID + "));";
	
	public static void onCreate(SQLiteDatabase db) {
		db.execSQL(STMT_CREATE);
	}

	public static void onUpgrade(SQLiteDatabase db, int oldVersion,
			int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + NAME);
		onCreate(db);
	}
}
