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
import java.util.Arrays;
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
     * Do an automatically triggered backup, if necessary.
     * @return true or false corresponding to {@link #backup()} or null, if no backup needed.
     */
    public Boolean autoBackup() {
        if (backupFileExists() || !prefs.getAutoBackupEnabled()) {
            return null;
        } else {
            boolean rtn = backup();
            if (rtn) {
                File backupDir = getBackupFile().getParentFile();
                File[] backupFiles = backupDir.listFiles(file -> file.getName().matches(
                        "cr-[0-9]+-[0-9]+-[0-9]+\\.db"));
                Arrays.sort(backupFiles);
                for (int deletionIndex = backupFiles.length - prefs.getAutoBackupRetention() - 1;
                     deletionIndex >= 0; deletionIndex--) {
                    backupFiles[deletionIndex].delete();
                }
            }
            return rtn;
        }
    }

    /**
     * Trigger a backup.
     * @return Whether the database was backed successfully.
     */
    public boolean backup() {
        Application.closeDatabases();
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
        Application.closeDatabases();
        File internalBackupFile = new File(dbFile.getParent(), INTERNAL_BACKUP);
        if (FileCopyUtil.copyFile(dbFile, internalBackupFile)) {
            try {
                InputStream backupSource = resolver.openInputStream(backup);
                OutputStream backupTarget = new FileOutputStream(dbFile);
                if (FileCopyUtil.copyFile(backupSource, backupTarget)) {
                    if (checkBackupSanity()) {
                        return true;
                    } else {
                        throw new IOException("Backup is insane.");
                    }
                }
                throw new IOException("Copying failed.");
            } catch (IOException e) {
                Log.w(TAG, "Need to restore internally, got Exception.", e);
                FileCopyUtil.copyFile(internalBackupFile, dbFile);
            }
        } else {
            Log.e(TAG, "Could not do an internal Backup before restore.");
        }
        return false;
    }

    /**
     * Checks whether the current DB file is valid.
     * @return Whether the DB is valid.
     */
    private boolean checkBackupSanity() {
        try {
            DataSQLiteOpenHelper.getInstance(Application.getContext());
            return true;
        } catch (Exception e) {
            Application.closeDatabases();
            Log.e(TAG, "Database is broken.", e);
            return false;
        }
    }
}
