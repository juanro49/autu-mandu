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
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.juanro.autumandu.Application;
import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.model.AutuManduDatabase;
import org.juanro.autumandu.util.FileCopyUtil;

/**
 * Class for managing database backups and restores.
 * Optimized for Room persistence.
 */
public class Backup {
    private static final String INTERNAL_BACKUP = "rescue.db";
    private static final String TAG = "Backup";

    private final File dbFile;
    private final Preferences prefs;
    private final ContentResolver resolver;
    private final Context mContext;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public Backup(Context context) {
        this.prefs = new Preferences(context);
        this.dbFile = context.getDatabasePath(AutuManduDatabase.DATABASE_NAME);
        this.mContext = context;
        this.resolver = context.getContentResolver();
    }

    /**
     * @return The target path for a backup.
     */
    public DocumentFile getBackupDir() {
        DocumentFile backupDir = null;
        String backupPath = prefs.getBackupPath();

        try {
            if (backupPath.startsWith("content://")) {
                backupDir = DocumentFile.fromTreeUri(mContext, Uri.parse(backupPath));
            } else {
                backupDir = DocumentFile.fromFile(new File(backupPath));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting backup directory", e);
        }

        return backupDir;
    }

    /**
     * Do an automatically triggered backup, if necessary.
     * @return true or false corresponding to {@link #backup(boolean)} or null, if no backup needed.
     */
    public Boolean autoBackup() {
        if (backupFileExists() || !prefs.getAutoBackupEnabled()) {
            return null;
        }

        boolean success = backup(false);
        if (success) {
            DocumentFile backupDir = getBackupDir();
            if (backupDir != null) {
                DocumentFile[] allFiles = backupDir.listFiles();

                // Filtrar archivos que coinciden con el patrón cr-yyyy-MM-dd.db
                List<DocumentFile> backupFiles = new ArrayList<>();
                for (DocumentFile file : allFiles) {
                    String name = file.getName();
                    if (name != null && name.matches("cr-[0-9]{4}-[0-9]{2}-[0-9]{2}\\.db")) {
                        backupFiles.add(file);
                    }
                }

                // Ordenar por nombre (que al ser yyyy-MM-dd coincide con fecha)
                backupFiles.sort((f1, f2) -> {
                    String n1 = Objects.toString(f1.getName(), "");
                    String n2 = Objects.toString(f2.getName(), "");
                    return n1.compareTo(n2);
                });

                // Borrar los más antiguos si superan la retención
                int retention = prefs.getAutoBackupRetention();
                if (backupFiles.size() > retention) {
                    for (int i = 0; i < backupFiles.size() - retention; i++) {
                        backupFiles.get(i).delete();
                    }
                }
            }
        }
        return success;
    }

    /**
     * Trigger a backup.
     * @return Whether the database was backed successfully.
     */
    public boolean backup(boolean replace) {
        // Crucial for Room: Close all connections to flush WAL/SHM files to the main DB file.
        Application.closeDatabases();

        String fileName = "cr-" + dateFormat.format(new Date()) + ".db";
        DocumentFile backupDir = getBackupDir();

        if (backupDir == null) return false;

        DocumentFile backupFile = backupDir.createFile("*/*", fileName);

        if (backupFile == null && replace) {
            DocumentFile file = backupDir.findFile(fileName);
            if (file != null && file.isFile()) {
                file.delete();
                backupFile = backupDir.createFile("*/*", fileName);
            }
        }

        if (backupFile == null) {
            Log.e(TAG, "Backup error, can't create file in path: " + backupDir.getUri());
            return false;
        }

        try (ParcelFileDescriptor pfd = resolver.openFileDescriptor(backupFile.getUri(), "w")) {
            if (pfd == null) {
                Log.e(TAG, "Could not open ParcelFileDescriptor for " + backupFile.getUri());
                return false;
            }
            return FileCopyUtil.copyFile(dbFile, pfd);
        } catch (IOException e) {
            Log.e(TAG, "Backup error: " + e.getMessage());
            return false;
        }
    }

    public boolean backupFileExists() {
        DocumentFile backupDir = getBackupDir();
        if (backupDir == null) return false;

        String fileName = "cr-" + dateFormat.format(new Date()) + ".db";
        DocumentFile file = backupDir.findFile(fileName);
        return file != null && file.isFile();
    }

    /**
     * Do a safe restore from a Uri (not necessarily a file).
     * @param backup The Uri of the selected backup.
     * @return Whether the restore succeed.
     */
    public boolean restore(Uri backup) {
        // Crucial for Room: Close all connections before overwriting the file.
        Application.closeDatabases();

        File internalBackupFile = new File(dbFile.getParent(), INTERNAL_BACKUP);
        if (FileCopyUtil.copyFile(dbFile, internalBackupFile)) {
            try (InputStream backupSource = resolver.openInputStream(backup);
                 OutputStream backupTarget = new FileOutputStream(dbFile)) {

                if (backupSource != null && FileCopyUtil.copyFile(backupSource, backupTarget)) {
                    if (checkBackupSanity()) {
                        return true;
                    } else {
                        throw new IOException("Backup is insane.");
                    }
                }
                throw new IOException("Copying failed or backup source is null.");
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
            // Attempt to open the database via Room to verify its integrity.
            AutuManduDatabase.getInstance(Application.getContext()).getOpenHelper().getReadableDatabase();
            return true;
        } catch (Exception e) {
            Application.closeDatabases();
            Log.e(TAG, "Database is broken.", e);
            return false;
        }
    }
}
