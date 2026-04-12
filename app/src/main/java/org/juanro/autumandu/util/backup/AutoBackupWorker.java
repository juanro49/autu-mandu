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

package org.juanro.autumandu.util.backup;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;

/**
 * WorkManager Worker to perform automatic backups.
 * Replaces the old AutoBackupTask with a more robust implementation.
 */
public class AutoBackupWorker extends Worker {
    private static final String TAG = "AutoBackupWorker";

    public AutoBackupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        var context = getApplicationContext();
        var preferences = new Preferences(context);

        if (!preferences.getAutoBackupEnabled()) {
            return Result.success();
        }

        Log.d(TAG, "Starting automatic backup...");
        try {
            var backup = new Backup(context);
            final var result = backup.autoBackup();

            if (result != null) {
                // Toasts must be shown on the main thread
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(context, (result ? R.string.toast_auto_backup_succeeded :
                                R.string.toast_auto_backup_failed), Toast.LENGTH_SHORT).show()
                );
            }

            return result != null && result ? Result.success() : Result.failure();
        } catch (Exception e) {
            Log.e(TAG, "Error during automatic backup: " + e.getMessage(), e);
            return Result.failure();
        }
    }

    /**
     * Helper method to schedule a one-time backup.
     */
    public static void enqueue(@NonNull Context context) {
        var workRequest = new OneTimeWorkRequest.Builder(AutoBackupWorker.class)
                .addTag("auto_backup")
                .build();
        WorkManager.getInstance(context).enqueue(workRequest);
    }
}
