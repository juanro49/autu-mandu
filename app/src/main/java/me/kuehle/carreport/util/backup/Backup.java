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

package me.kuehle.carreport.util.backup;

import android.content.ContentResolver;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import me.kuehle.carreport.Application;
import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.provider.DataSQLiteOpenHelper;
import me.kuehle.carreport.util.FileCopyUtil;

public class Backup {
    private static final String INTERNAL_BACKUP = "rescue.db";
    private static final String TAG = "Backup";

    private File dbFile;
    private Preferences prefs;
    private ContentResolver resolver;

    public Backup(Context context) {
        prefs = new Preferences(context);
        dbFile = new File(DataSQLiteOpenHelper.getInstance(context).getReadableDatabase().getPath());
        resolver = context.getContentResolver();
    }

    /**
     * @return The target path for a backup.
     */
    public File getBackupFile() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return new File(prefs.getBackupPath(), "cr-" + df.format(new Date()) + ".db");
    }

    /**
     * Trigger a backup.
     * @return Whether the database was backed successfully.
     */
    public boolean backup() {
        File backupFile = getBackupFile();
        File backupDir = backupFile.getParentFile();
        if (!backupDir.isDirectory()) {
            if (!backupDir.mkdir()) {
                return false;
            }
        }
        return FileCopyUtil.copyFile(dbFile, backupFile);
    }

    public boolean backupFileExists() {
        return getBackupFile().isFile();
    }

    /**
     * Do a safe restore from a Uri (not necessarily a file).
     * @param backup The Uri of the selected backup.
     * @return Whether the restore succeed.
     */
    public boolean restore(Uri backup) {
        File internalBackupFile = new File(dbFile.getParent(), INTERNAL_BACKUP);
        if (FileCopyUtil.copyFile(dbFile, internalBackupFile)) {
            try {
                InputStream backupSource = resolver.openInputStream(backup);
                OutputStream backupTarget = new FileOutputStream(dbFile);
                if (FileCopyUtil.copyFile(backupSource, backupTarget)) {
                    if (checkBackupSanity(dbFile)) {
                        Application.reinitializeDatabase();
                        return true;
                    } else {
                        throw new IOException("Backup is insane.");
                    }
                }
                throw new IOException("Copying failed.");
            } catch (IOException e) {
                Log.w(TAG, "Need to restore internally, got Exception.", e);
                FileCopyUtil.copyFile(internalBackupFile, dbFile);
                Application.reinitializeDatabase();
            }
        } else {
            Log.e(TAG, "Could not do an internal Backup before restore.");
        }
        return false;
    }

    /**
     * Checks a file for sanity for use as restored backup.
     * @param dbFile A file that claims to be a valid backup-file.
     * @return Whether the file is valid.
     */
    private boolean checkBackupSanity(File dbFile) {
        try {
            SQLiteDatabase instance = SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null,
                    SQLiteDatabase.OPEN_READONLY);
            instance.rawQuery("PRAGMA integrity_check(1)", null);
            // TODO: a version check should be built in here. This would require a schema change.
            instance.close();
            return true;
        } catch (SQLiteException e) {
            Log.e(TAG, "Database is broken.", e);
            return false;
        }
    }
}
