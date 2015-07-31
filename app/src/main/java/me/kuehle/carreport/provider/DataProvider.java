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
package me.kuehle.carreport.provider;

import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import java.util.Arrays;

import me.kuehle.carreport.BuildConfig;
import me.kuehle.carreport.provider.base.BaseContentProvider;
import me.kuehle.carreport.provider.car.CarColumns;
import me.kuehle.carreport.provider.fueltype.FuelTypeColumns;
import me.kuehle.carreport.provider.othercost.OtherCostColumns;
import me.kuehle.carreport.provider.refueling.RefuelingColumns;
import me.kuehle.carreport.provider.reminder.ReminderColumns;

public class DataProvider extends BaseContentProvider {
    private static final String TAG = DataProvider.class.getSimpleName();

    private static final boolean DEBUG = BuildConfig.DEBUG;

    private static final String TYPE_CURSOR_ITEM = "vnd.android.cursor.item/";
    private static final String TYPE_CURSOR_DIR = "vnd.android.cursor.dir/";

    public static final String AUTHORITY = "me.kuehle.carreport.provider";
    public static final String CONTENT_URI_BASE = "content://" + AUTHORITY;

    private static final int URI_TYPE_CAR = 0;
    private static final int URI_TYPE_CAR_ID = 1;

    private static final int URI_TYPE_FUEL_TYPE = 2;
    private static final int URI_TYPE_FUEL_TYPE_ID = 3;

    private static final int URI_TYPE_OTHER_COST = 4;
    private static final int URI_TYPE_OTHER_COST_ID = 5;

    private static final int URI_TYPE_REFUELING = 6;
    private static final int URI_TYPE_REFUELING_ID = 7;

    private static final int URI_TYPE_REMINDER = 8;
    private static final int URI_TYPE_REMINDER_ID = 9;



    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        URI_MATCHER.addURI(AUTHORITY, CarColumns.TABLE_NAME, URI_TYPE_CAR);
        URI_MATCHER.addURI(AUTHORITY, CarColumns.TABLE_NAME + "/#", URI_TYPE_CAR_ID);
        URI_MATCHER.addURI(AUTHORITY, FuelTypeColumns.TABLE_NAME, URI_TYPE_FUEL_TYPE);
        URI_MATCHER.addURI(AUTHORITY, FuelTypeColumns.TABLE_NAME + "/#", URI_TYPE_FUEL_TYPE_ID);
        URI_MATCHER.addURI(AUTHORITY, OtherCostColumns.TABLE_NAME, URI_TYPE_OTHER_COST);
        URI_MATCHER.addURI(AUTHORITY, OtherCostColumns.TABLE_NAME + "/#", URI_TYPE_OTHER_COST_ID);
        URI_MATCHER.addURI(AUTHORITY, RefuelingColumns.TABLE_NAME, URI_TYPE_REFUELING);
        URI_MATCHER.addURI(AUTHORITY, RefuelingColumns.TABLE_NAME + "/#", URI_TYPE_REFUELING_ID);
        URI_MATCHER.addURI(AUTHORITY, ReminderColumns.TABLE_NAME, URI_TYPE_REMINDER);
        URI_MATCHER.addURI(AUTHORITY, ReminderColumns.TABLE_NAME + "/#", URI_TYPE_REMINDER_ID);
    }

    @Override
    protected SQLiteOpenHelper createSqLiteOpenHelper() {
        return DataSQLiteOpenHelper.getInstance(getContext());
    }

    @Override
    protected boolean hasDebug() {
        return DEBUG;
    }

    @Override
    public String getType(Uri uri) {
        int match = URI_MATCHER.match(uri);
        switch (match) {
            case URI_TYPE_CAR:
                return TYPE_CURSOR_DIR + CarColumns.TABLE_NAME;
            case URI_TYPE_CAR_ID:
                return TYPE_CURSOR_ITEM + CarColumns.TABLE_NAME;

            case URI_TYPE_FUEL_TYPE:
                return TYPE_CURSOR_DIR + FuelTypeColumns.TABLE_NAME;
            case URI_TYPE_FUEL_TYPE_ID:
                return TYPE_CURSOR_ITEM + FuelTypeColumns.TABLE_NAME;

            case URI_TYPE_OTHER_COST:
                return TYPE_CURSOR_DIR + OtherCostColumns.TABLE_NAME;
            case URI_TYPE_OTHER_COST_ID:
                return TYPE_CURSOR_ITEM + OtherCostColumns.TABLE_NAME;

            case URI_TYPE_REFUELING:
                return TYPE_CURSOR_DIR + RefuelingColumns.TABLE_NAME;
            case URI_TYPE_REFUELING_ID:
                return TYPE_CURSOR_ITEM + RefuelingColumns.TABLE_NAME;

            case URI_TYPE_REMINDER:
                return TYPE_CURSOR_DIR + ReminderColumns.TABLE_NAME;
            case URI_TYPE_REMINDER_ID:
                return TYPE_CURSOR_ITEM + ReminderColumns.TABLE_NAME;

        }
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (DEBUG) Log.d(TAG, "insert uri=" + uri + " values=" + values);
        return super.insert(uri, values);
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        if (DEBUG) Log.d(TAG, "bulkInsert uri=" + uri + " values.length=" + values.length);
        return super.bulkInsert(uri, values);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (DEBUG) Log.d(TAG, "update uri=" + uri + " values=" + values + " selection=" + selection + " selectionArgs=" + Arrays.toString(selectionArgs));
        return super.update(uri, values, selection, selectionArgs);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (DEBUG) Log.d(TAG, "delete uri=" + uri + " selection=" + selection + " selectionArgs=" + Arrays.toString(selectionArgs));
        return super.delete(uri, selection, selectionArgs);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (DEBUG)
            Log.d(TAG, "query uri=" + uri + " selection=" + selection + " selectionArgs=" + Arrays.toString(selectionArgs) + " sortOrder=" + sortOrder
                    + " groupBy=" + uri.getQueryParameter(QUERY_GROUP_BY) + " having=" + uri.getQueryParameter(QUERY_HAVING) + " limit=" + uri.getQueryParameter(QUERY_LIMIT));

        return super.query(uri, qualifyAmbiguousColumns(uri, projection), selection, selectionArgs, sortOrder);
    }

    /**
     * If projection is null (which means all columns should be returned), remove the _id
     * column from all joined tables, so only the _id column of the main table is included.
     */
    private static String[] qualifyAmbiguousColumns(Uri uri, String[] projection) {
        if (projection != null) return projection;

        int matchedId = URI_MATCHER.match(uri);
        switch (matchedId) {
            case URI_TYPE_CAR:
            case URI_TYPE_CAR_ID:
                return CarColumns.ALL_COLUMNS;

            case URI_TYPE_FUEL_TYPE:
            case URI_TYPE_FUEL_TYPE_ID:
                return FuelTypeColumns.ALL_COLUMNS;

            case URI_TYPE_OTHER_COST:
            case URI_TYPE_OTHER_COST_ID:
                projection = new String[OtherCostColumns.ALL_COLUMNS.length + CarColumns.ALL_COLUMNS.length - 1];
                System.arraycopy(OtherCostColumns.ALL_COLUMNS, 0, projection, 0, OtherCostColumns.ALL_COLUMNS.length);
                System.arraycopy(CarColumns.ALL_COLUMNS, 1, projection, OtherCostColumns.ALL_COLUMNS.length, CarColumns.ALL_COLUMNS.length - 1);
                return projection;

            case URI_TYPE_REFUELING:
            case URI_TYPE_REFUELING_ID:
                projection = new String[RefuelingColumns.ALL_COLUMNS.length + FuelTypeColumns.ALL_COLUMNS.length - 1 + CarColumns.ALL_COLUMNS.length - 1];
                System.arraycopy(RefuelingColumns.ALL_COLUMNS, 0, projection, 0, RefuelingColumns.ALL_COLUMNS.length);
                System.arraycopy(FuelTypeColumns.ALL_COLUMNS, 1, projection, RefuelingColumns.ALL_COLUMNS.length, FuelTypeColumns.ALL_COLUMNS.length - 1);
                System.arraycopy(CarColumns.ALL_COLUMNS, 1, projection, RefuelingColumns.ALL_COLUMNS.length + FuelTypeColumns.ALL_COLUMNS.length - 1, CarColumns.ALL_COLUMNS.length - 1);
                return projection;

            case URI_TYPE_REMINDER:
            case URI_TYPE_REMINDER_ID:
                projection = new String[ReminderColumns.ALL_COLUMNS.length + CarColumns.ALL_COLUMNS.length - 1];
                System.arraycopy(ReminderColumns.ALL_COLUMNS, 0, projection, 0, ReminderColumns.ALL_COLUMNS.length);
                System.arraycopy(CarColumns.ALL_COLUMNS, 1, projection, ReminderColumns.ALL_COLUMNS.length, CarColumns.ALL_COLUMNS.length - 1);
                return projection;

            default:
                throw new IllegalArgumentException("The uri '" + uri + "' is not supported by this ContentProvider");
        }
    }

    @Override
    protected QueryParams getQueryParams(Uri uri, String selection, String[] projection) {
        QueryParams res = new QueryParams();
        String id = null;
        int matchedId = URI_MATCHER.match(uri);
        switch (matchedId) {
            case URI_TYPE_CAR:
            case URI_TYPE_CAR_ID:
                res.table = CarColumns.TABLE_NAME;
                res.idColumn = CarColumns._ID;
                res.tablesWithJoins = CarColumns.TABLE_NAME;
                res.orderBy = CarColumns.DEFAULT_ORDER;
                break;

            case URI_TYPE_FUEL_TYPE:
            case URI_TYPE_FUEL_TYPE_ID:
                res.table = FuelTypeColumns.TABLE_NAME;
                res.idColumn = FuelTypeColumns._ID;
                res.tablesWithJoins = FuelTypeColumns.TABLE_NAME;
                res.orderBy = FuelTypeColumns.DEFAULT_ORDER;
                break;

            case URI_TYPE_OTHER_COST:
            case URI_TYPE_OTHER_COST_ID:
                res.table = OtherCostColumns.TABLE_NAME;
                res.idColumn = OtherCostColumns._ID;
                res.tablesWithJoins = OtherCostColumns.TABLE_NAME;
                if (CarColumns.hasColumns(projection)) {
                    res.tablesWithJoins += " LEFT OUTER JOIN " + CarColumns.TABLE_NAME + " AS " + OtherCostColumns.PREFIX_CAR + " ON " + OtherCostColumns.TABLE_NAME + "." + OtherCostColumns.CAR_ID + "=" + OtherCostColumns.PREFIX_CAR + "." + CarColumns._ID;
                }
                res.orderBy = OtherCostColumns.DEFAULT_ORDER;
                break;

            case URI_TYPE_REFUELING:
            case URI_TYPE_REFUELING_ID:
                res.table = RefuelingColumns.TABLE_NAME;
                res.idColumn = RefuelingColumns._ID;
                res.tablesWithJoins = RefuelingColumns.TABLE_NAME;
                if (FuelTypeColumns.hasColumns(projection)) {
                    res.tablesWithJoins += " LEFT OUTER JOIN " + FuelTypeColumns.TABLE_NAME + " AS " + RefuelingColumns.PREFIX_FUEL_TYPE + " ON " + RefuelingColumns.TABLE_NAME + "." + RefuelingColumns.FUEL_TYPE_ID + "=" + RefuelingColumns.PREFIX_FUEL_TYPE + "." + FuelTypeColumns._ID;
                }
                if (CarColumns.hasColumns(projection)) {
                    res.tablesWithJoins += " LEFT OUTER JOIN " + CarColumns.TABLE_NAME + " AS " + RefuelingColumns.PREFIX_CAR + " ON " + RefuelingColumns.TABLE_NAME + "." + RefuelingColumns.CAR_ID + "=" + RefuelingColumns.PREFIX_CAR + "." + CarColumns._ID;
                }
                res.orderBy = RefuelingColumns.DEFAULT_ORDER;
                break;

            case URI_TYPE_REMINDER:
            case URI_TYPE_REMINDER_ID:
                res.table = ReminderColumns.TABLE_NAME;
                res.idColumn = ReminderColumns._ID;
                res.tablesWithJoins = ReminderColumns.TABLE_NAME;
                if (CarColumns.hasColumns(projection)) {
                    res.tablesWithJoins += " LEFT OUTER JOIN " + CarColumns.TABLE_NAME + " AS " + ReminderColumns.PREFIX_CAR + " ON " + ReminderColumns.TABLE_NAME + "." + ReminderColumns.CAR_ID + "=" + ReminderColumns.PREFIX_CAR + "." + CarColumns._ID;
                }
                res.orderBy = ReminderColumns.DEFAULT_ORDER;
                break;

            default:
                throw new IllegalArgumentException("The uri '" + uri + "' is not supported by this ContentProvider");
        }

        switch (matchedId) {
            case URI_TYPE_CAR_ID:
            case URI_TYPE_FUEL_TYPE_ID:
            case URI_TYPE_OTHER_COST_ID:
            case URI_TYPE_REFUELING_ID:
            case URI_TYPE_REMINDER_ID:
                id = uri.getLastPathSegment();
        }
        if (id != null) {
            if (selection != null) {
                res.selection = res.table + "." + res.idColumn + "=" + id + " and (" + selection + ")";
            } else {
                res.selection = res.table + "." + res.idColumn + "=" + id;
            }
        } else {
            res.selection = selection;
        }
        return res;
    }
}
