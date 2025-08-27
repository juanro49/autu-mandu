/*
 * Copyright 2015 Jan Kühle
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
package org.juanro.autumandu.provider;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.DefaultDatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.util.Log;

import org.juanro.autumandu.BuildConfig;
import org.juanro.autumandu.model.AutuManduDatabase;

@Deprecated
public class DataSQLiteOpenHelper extends SQLiteOpenHelper {
    private static final String TAG = DataSQLiteOpenHelper.class.getSimpleName();

    public static final String DATABASE_FILE_NAME = "data.db";
    private static final int DATABASE_VERSION = 13;
    private static DataSQLiteOpenHelper sInstance;
    private final Context mContext;
    private final DataSQLiteOpenHelperCallbacks mOpenHelperCallbacks;

    // @formatter:off
    /*
    public static final String SQL_CREATE_TABLE_CAR = "CREATE TABLE IF NOT EXISTS "
            + CarColumns.TABLE_NAME + " ( "
            + CarColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + CarColumns.NAME + " TEXT NOT NULL, "
            + CarColumns.COLOR + " INTEGER NOT NULL, "
            + CarColumns.INITIAL_MILEAGE + " INTEGER NOT NULL DEFAULT 0, "
            + CarColumns.SUSPENDED_SINCE + " INTEGER, "
            + CarColumns.BUYING_PRICE + " REAL "
            + " );";

    public static final String SQL_CREATE_TABLE_FUEL_TYPE = "CREATE TABLE IF NOT EXISTS "
            + FuelTypeColumns.TABLE_NAME + " ( "
            + FuelTypeColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + FuelTypeColumns.NAME + " TEXT NOT NULL, "
            + FuelTypeColumns.CATEGORY + " TEXT "
            + ", CONSTRAINT unique_name UNIQUE (fuel_type__name) ON CONFLICT REPLACE"
            + " );";

    public static final String SQL_CREATE_TABLE_STATION = "CREATE TABLE IF NOT EXISTS "
        + StationColumns.TABLE_NAME + " ( "
        + StationColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
        + StationColumns.NAME + " TEXT NOT NULL "
        + ", CONSTRAINT unique_name UNIQUE (station__name) ON CONFLICT REPLACE"
        + " );";

    public static final String SQL_CREATE_TABLE_OTHER_COST = "CREATE TABLE IF NOT EXISTS "
            + OtherCostColumns.TABLE_NAME + " ( "
            + OtherCostColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + OtherCostColumns.TITLE + " TEXT NOT NULL, "
            + OtherCostColumns.DATE + " INTEGER NOT NULL, "
            + OtherCostColumns.MILEAGE + " INTEGER, "
            + OtherCostColumns.PRICE + " REAL NOT NULL, "
            + OtherCostColumns.RECURRENCE_INTERVAL + " INTEGER NOT NULL, "
            + OtherCostColumns.RECURRENCE_MULTIPLIER + " INTEGER NOT NULL, "
            + OtherCostColumns.END_DATE + " INTEGER, "
            + OtherCostColumns.NOTE + " TEXT NOT NULL, "
            + OtherCostColumns.CAR_ID + " INTEGER NOT NULL "
            + ", CONSTRAINT fk_car_id FOREIGN KEY (" + OtherCostColumns.CAR_ID + ") REFERENCES car (_id) ON DELETE CASCADE"
            + " );";

    public static final String SQL_CREATE_TABLE_REFUELING = "CREATE TABLE IF NOT EXISTS "
            + RefuelingColumns.TABLE_NAME + " ( "
            + RefuelingColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + RefuelingColumns.DATE + " INTEGER NOT NULL, "
            + RefuelingColumns.MILEAGE + " INTEGER NOT NULL, "
            + RefuelingColumns.VOLUME + " REAL NOT NULL, "
            + RefuelingColumns.PRICE + " REAL NOT NULL, "
            + RefuelingColumns.PARTIAL + " INTEGER NOT NULL, "
            + RefuelingColumns.NOTE + " TEXT NOT NULL, "
            + RefuelingColumns.FUEL_TYPE_ID + " INTEGER NOT NULL, "
            + RefuelingColumns.CAR_ID + " INTEGER NOT NULL "
            + ", CONSTRAINT fk_fuel_type_id FOREIGN KEY (" + RefuelingColumns.FUEL_TYPE_ID + ") REFERENCES fuel_type (_id) ON DELETE CASCADE"
            + ", CONSTRAINT fk_station_id FOREIGN KEY (" + RefuelingColumns.STATION_ID + ") REFERENCES station (_id) ON DELETE CASCADE"
            + ", CONSTRAINT fk_car_id FOREIGN KEY (" + RefuelingColumns.CAR_ID + ") REFERENCES car (_id) ON DELETE CASCADE"
            + " );";

    public static final String SQL_CREATE_TABLE_REMINDER = "CREATE TABLE IF NOT EXISTS "
            + ReminderColumns.TABLE_NAME + " ( "
            + ReminderColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + ReminderColumns.TITLE + " TEXT NOT NULL, "
            + ReminderColumns.AFTER_TIME_SPAN_UNIT + " INTEGER, "
            + ReminderColumns.AFTER_TIME_SPAN_COUNT + " INTEGER, "
            + ReminderColumns.AFTER_DISTANCE + " INTEGER, "
            + ReminderColumns.START_DATE + " INTEGER NOT NULL, "
            + ReminderColumns.START_MILEAGE + " INTEGER NOT NULL, "
            + ReminderColumns.NOTIFICATION_DISMISSED + " INTEGER NOT NULL, "
            + ReminderColumns.SNOOZED_UNTIL + " INTEGER, "
            + ReminderColumns.CAR_ID + " INTEGER NOT NULL "
            + ", CONSTRAINT fk_car_id FOREIGN KEY (" + ReminderColumns.CAR_ID + ") REFERENCES car (_id) ON DELETE CASCADE"
            + " );";
     */
    // @formatter:on

    /**
     * Closes connection and forces re initialization of connection.
     */
    public static void resetInstance() {
        if (sInstance != null) {
            Log.d(TAG, "resetting instance");
            sInstance.close();
            sInstance = null;
        }
    }

    public static DataSQLiteOpenHelper getInstance(Context context) {
        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (sInstance == null) {
            sInstance = newInstance(context.getApplicationContext());
        }
        return sInstance;
    }

    private static DataSQLiteOpenHelper newInstance(Context context) {
        // prepare structures with Room
        AutuManduDatabase roomDB = AutuManduDatabase.getInstance(context);
        roomDB.getCarDao().getAll();
        roomDB.getFuelTypeDao().getAll();
        roomDB.getStationDao().getAll();
        roomDB.getOtherCostDao().getAll();
        roomDB.getRefuelingDao().getAll();
        roomDB.getReminderDao().getAll();
        roomDB.getTireDao().getAllTires();
        roomDB.getTireDao().getAllTireUsages();

        String name = roomDB.getOpenHelper().getDatabaseName();
        Log.d(TAG, String.format("Using database at %s via classic content provider.",
                context.getDatabasePath(name).getAbsolutePath()));

        // Execute legacy.
        return newInstancePostHoneycomb(context, name);
    }


    private static DataSQLiteOpenHelper newInstancePostHoneycomb(Context context, String path) {
        return new DataSQLiteOpenHelper(context, path, new DefaultDatabaseErrorHandler());
    }

    private DataSQLiteOpenHelper(Context context, String path, DatabaseErrorHandler errorHandler) {
        super(context, path,null, DATABASE_VERSION, errorHandler);
        mContext = context;
        mOpenHelperCallbacks = new DataSQLiteOpenHelperCallbacks();
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        if (BuildConfig.DEBUG) Log.d(TAG, "onCreate");
        mOpenHelperCallbacks.onPreCreate(mContext, db);
        // Do nothing any more here, action happens in AutuManduDatabase
        mOpenHelperCallbacks.onPostCreate(mContext, db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            setForeignKeyConstraintsEnabled(db);

        }
        mOpenHelperCallbacks.onOpen(mContext, db);
    }

    private void setForeignKeyConstraintsEnabled(SQLiteDatabase db) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            setForeignKeyConstraintsEnabledPreJellyBean(db);
        } else {
            setForeignKeyConstraintsEnabledPostJellyBean(db);
        }
    }

    private void setForeignKeyConstraintsEnabledPreJellyBean(SQLiteDatabase db) {
        db.execSQL("PRAGMA foreign_keys=ON;");
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void setForeignKeyConstraintsEnabledPostJellyBean(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Do nothing here, action happens in AutuManduDatabase
        //mOpenHelperCallbacks.onUpgrade(mContext, db, oldVersion, newVersion);
    }
}
