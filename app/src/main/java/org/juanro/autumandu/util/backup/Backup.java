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

package org.juanro.autumandu.util.backup;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import org.juanro.autumandu.Application;
import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.provider.DataSQLiteOpenHelper;
import org.juanro.autumandu.util.FileCopyUtil;

public class Backup {
    private static final String INTERNAL_BACKUP = "rescue.db";
    private static final String TAG = "Backup";

    private File dbFile;
    private Preferences prefs;
    private ContentResolver resolver;
    private Context mContext;

    public Backup(Context context) {
        prefs = new Preferences(context);
        dbFile = new File(DataSQLiteOpenHelper.getInstance(context).getReadableDatabase().getPath());
        mContext = context;
        resolver = context.getContentResolver();
    }

    /**
     * @return The target path for a backup.
     */
    public DocumentFile getBackupDir() {
        DocumentFile backupDir = null;

        if (prefs.getBackupPath().startsWith("/"))
        {
            backupDir = DocumentFile.fromFile(new File(prefs.getBackupPath()));
        }
        else
        {
            Uri backupDirUri = DocumentsContract.buildChildDocumentsUriUsingTree(Uri.parse(prefs.getBackupPath()), DocumentsContract.getTreeDocumentId(Uri.parse(prefs.getBackupPath())));
            backupDir = DocumentFile.fromTreeUri(mContext, backupDirUri);
        }

        return backupDir;
    }

    /**
     * Do an automatically triggered backup, if necessary.
     * @return true or false corresponding to {@link #backup()} or null, if no backup needed.
     */
    public Boolean autoBackup() {
        if (backupFileExists() || !prefs.getAutoBackupEnabled()) {
            return null;
        } else {
            boolean rtn = backup(false);
            if (rtn) {
                DocumentFile backupDir = getBackupDir();
                DocumentFile[] allFiles = backupDir.listFiles();
                ArrayList<String> backupFiles = new ArrayList<String>();

                for (final DocumentFile file: allFiles) {
                    if (file.getName().matches("cr-[0-9]+-[0-9]+-[0-9]+\\.db")) {
                        backupFiles.add(file.getName());
                    }
                }
                Arrays.sort(backupFiles.toArray());
                for (int deletionIndex = backupFiles.size() - prefs.getAutoBackupRetention() - 1;
                     deletionIndex >= 0; deletionIndex--) {
                    Objects.requireNonNull(backupDir.findFile(backupFiles.get(deletionIndex))).delete();
                }
            }
            return rtn;
        }
    }

    /**
     * Trigger a backup.
     * @return Whether the database was backed successfully.
     */
    public boolean backup(boolean replace) {
        Application.closeDatabases();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        DocumentFile backupFile = getBackupDir().createFile("*/*", "cr-" + df.format(new Date()) + ".db");
        ParcelFileDescriptor pfd = null;

        if (backupFile == null && replace) {
            DocumentFile file = getBackupDir().findFile("cr-" + df.format(new Date()) + ".db");

            if (file != null && file.isFile()) {
                file.delete();
                backupFile = getBackupDir().createFile("*/*", "cr-" + df.format(new Date()) + ".db");
            }
        }

        try {
            if (backupFile != null) {
                pfd = resolver.openFileDescriptor(backupFile.getUri(), "w");
            }
            else {
                Log.e(TAG, "Backup error, can't create file in path: " + getBackupDir().getUri());
                return false;
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Backup error " + e.getMessage());
            return false;
        }

        return FileCopyUtil.copyFile(dbFile, pfd);
    }

    public boolean backupFileExists() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        DocumentFile file = getBackupDir().findFile("cr-" + df.format(new Date()) + ".db");
        return file != null && file.isFile();
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
