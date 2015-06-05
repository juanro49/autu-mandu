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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import me.kuehle.carreport.BuildConfig;

/**
 * Implement your custom database creation or upgrade code here.
 * <p/>
 * This file will not be overwritten if you re-run the content provider generator.
 */
public class DataSQLiteOpenHelperCallbacks {
    private static final String TAG = DataSQLiteOpenHelperCallbacks.class.getSimpleName();

    public void onOpen(final Context context, final SQLiteDatabase db) {
        if (BuildConfig.DEBUG) Log.d(TAG, "onOpen");
    }

    public void onPreCreate(final Context context, final SQLiteDatabase db) {
        if (BuildConfig.DEBUG) Log.d(TAG, "onPreCreate");
    }

    public void onPostCreate(final Context context, final SQLiteDatabase db) {
        if (BuildConfig.DEBUG) Log.d(TAG, "onPostCreate");
    }

    public void onUpgrade(final Context context, final SQLiteDatabase db, final int oldVersion, final int newVersion) {
        if (BuildConfig.DEBUG) Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);

        db.beginTransaction();
        try {
            for (int i = oldVersion + 1; i <= newVersion; i++) {
                String rawSql = getMigrationFileContent(context, i);
                String[] commands = prepareSqlStatements(rawSql);
                for (String command : commands) {
                    db.execSQL(command);
                }
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private String[] prepareSqlStatements(String rawSql) {
        rawSql = rawSql.replaceAll("[\r\n]", " ");
        String[] rawCommands = rawSql.split(";");

        List<String> commands = new ArrayList<>(rawCommands.length);
        for (int i = 0; i < rawCommands.length; i++) {
            rawCommands[i] = rawCommands[i].trim();
            if (rawCommands[i].length() > 0) {
                commands.add(rawCommands[i]);
            }
        }

        return commands.toArray(new String[commands.size()]);
    }

    private String getMigrationFileContent(Context context, int targetVersion) {
        try {
            InputStream in = context.getAssets().open(String.format("migrations/%d.sql", targetVersion));

            byte[] buffer = new byte[in.available()];
            //noinspection ResultOfMethodCallIgnored
            in.read(buffer);
            in.close();

            return new String(buffer);
        } catch (IOException e) {
            Log.e(TAG, "Error reading migration file.", e);
            return "";
        }
    }
}
