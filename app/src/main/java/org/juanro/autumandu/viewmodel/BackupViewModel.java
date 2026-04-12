/*
 * Copyright 2026 Juanro49
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

package org.juanro.autumandu.viewmodel;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import org.juanro.autumandu.util.backup.Backup;
import org.juanro.autumandu.util.backup.CSVExportImport;
import org.juanro.autumandu.util.backup.CSVImportException;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * ViewModel for backup and CSV export/import operations.
 */
public class BackupViewModel extends AndroidViewModel {
    private final Backup backup;
    private final CSVExportImport csvExportImport;
    private static final Executor DB_EXECUTOR = Executors.newSingleThreadExecutor();

    public BackupViewModel(@NonNull Application application) {
        super(application);
        this.backup = new Backup(application);
        this.csvExportImport = new CSVExportImport(application);
    }

    public Backup getBackup() {
        return backup;
    }

    public CSVExportImport getCsvExportImport() {
        return csvExportImport;
    }

    public void runBackup(Runnable onComplete, ErrorCallback onError) {
        DB_EXECUTOR.execute(() -> {
            boolean success = backup.backup(true);
            if (success) {
                onComplete.run();
            } else {
                onError.onError(null);
            }
        });
    }

    public void runExportCSV(Runnable onComplete, ErrorCallback onError) {
        DB_EXECUTOR.execute(() -> {
            try {
                csvExportImport.export();
                onComplete.run();
            } catch (CSVImportException e) {
                onError.onError(e.getMessage());
            }
        });
    }

    public void runImportCSV(Runnable onComplete, ErrorCallback onError) {
        DB_EXECUTOR.execute(() -> {
            try {
                csvExportImport.importData();
                onComplete.run();
            } catch (CSVImportException e) {
                onError.onError(e.getMessage());
            }
        });
    }

    public void runRestore(Uri backupUri, Runnable onComplete, ErrorCallback onError) {
        DB_EXECUTOR.execute(() -> {
            boolean success = backup.restore(backupUri);
            if (success) {
                onComplete.run();
            } else {
                onError.onError(null);
            }
        });
    }

    public interface ErrorCallback {
        void onError(String message);
    }
}
