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

package org.juanro.autumandu.util.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;
import org.juanro.autumandu.gui.MainActivity;

import java.util.concurrent.TimeUnit;

/**
 * Manager for scheduling synchronization tasks using WorkManager.
 * Optimized for battery efficiency and reliable network operations.
 */
public final class SyncManager {
    public static final String SYNC_WORK_NAME_PERIODIC = "org.juanro.autumandu.sync.periodic";
    public static final String SYNC_WORK_NAME_ONCE = "org.juanro.autumandu.sync.once";

    private static final int NOTIFICATION_ID_CONFLICT = 2;
    private static final String NOTIFICATION_CHANNEL_ID_SYNC = "sync";

    private SyncManager() {
        // Utility class
    }

    /**
     * Returns the currently active sync account, if any.
     * @param context Application context.
     * @return The active sync account or null.
     */
    @Nullable
    public static Account getCurrentSyncAccount(Context context) {
        AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccountsByType(Authenticator.ACCOUNT_TYPE);
        return accounts.length > 0 ? accounts[0] : null;
    }

    /**
     * Schedules a periodic sync task every hour.
     */
    public static void schedulePeriodicSync(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build();

        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(SyncWorker.class, 1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                SYNC_WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
        );
    }

    /**
     * Runs a sync task immediately.
     */
    public static void runSyncOnce(Context context) {
        runSyncOnce(context, false, false);
    }

    private static void runSyncOnce(Context context, boolean forceUpload, boolean forceDownload) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest.Builder builder = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS);

        if (forceUpload || forceDownload) {
            androidx.work.Data.Builder dataBuilder = new androidx.work.Data.Builder();
            if (forceUpload) dataBuilder.putBoolean(SyncWorker.KEY_FORCE_UPLOAD, true);
            if (forceDownload) dataBuilder.putBoolean(SyncWorker.KEY_FORCE_DOWNLOAD, true);
            builder.setInputData(dataBuilder.build());
        }

        OneTimeWorkRequest syncRequest = builder.build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                SYNC_WORK_NAME_ONCE,
                ExistingWorkPolicy.REPLACE,
                syncRequest
        );
    }

    /**
     * Resolves a sync conflict by either forcing an upload or a download.
     * @param context Application context.
     * @param useLocal If true, local data will be uploaded. If false, remote data will be downloaded.
     */
    public static void resolveConflict(Context context, boolean useLocal) {
        new Preferences(context).setSyncConflict(false);
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_CONFLICT);
        runSyncOnce(context, useLocal, !useLocal);
    }

    /**
     * Shows a notification that a sync conflict has occurred.
     * @param context Application context.
     */
    public static void showConflictNotification(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.createNotificationChannel(new NotificationChannel(
                        NOTIFICATION_CHANNEL_ID_SYNC,
                        context.getString(R.string.notification_sync_channel),
                        NotificationManager.IMPORTANCE_DEFAULT));
            }
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID_SYNC)
                .setSmallIcon(R.drawable.ic_c_notification_24dp)
                .setContentTitle(context.getString(R.string.notification_sync_conflict_title))
                .setContentText(context.getString(R.string.notification_sync_conflict_text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_CONFLICT, builder.build());
        } catch (SecurityException ignored) {
            // Permission might have been revoked since check
        }
    }
}
