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

package org.juanro.autumandu.util.reminder;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

/**
 * Worker to update reminder notifications using WorkManager.
 * Handles both one-time updates and periodic checks.
 */
public class ReminderWorker extends Worker {
    private static final String PERIODIC_WORK_NAME = "reminder_periodic_update";

    public static final String KEY_ACTION = "action";
    public static final String KEY_REMINDER_IDS = "reminder_ids";

    public static final String ACTION_UPDATE = "update";
    public static final String ACTION_MARK_DONE = "mark_done";
    public static final String ACTION_DISMISS = "dismiss";
    public static final String ACTION_SNOOZE = "snooze";

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String action = getInputData().getString(KEY_ACTION);
        long[] ids = getInputData().getLongArray(KEY_REMINDER_IDS);
        Context context = getApplicationContext();

        if (ACTION_MARK_DONE.equals(action) && ids != null) {
            ReminderService.markRemindersDone(context, ids);
        } else if (ACTION_DISMISS.equals(action) && ids != null) {
            ReminderService.dismissReminders(context, ids);
        } else if (ACTION_SNOOZE.equals(action) && ids != null) {
            ReminderService.snoozeReminders(context, ids);
        } else {
            // Por defecto o si es ACTION_UPDATE
            ReminderService.updateNotification(context);
        }

        return Result.success();
    }

    /**
     * Enqueues a one-time work request to update notifications immediately.
     */
    public static void enqueueUpdate(@NonNull Context context) {
        enqueueAction(context, ACTION_UPDATE, null);
    }

    /**
     * Enqueues a one-time work request to perform a specific action.
     */
    public static void enqueueAction(@NonNull Context context, String action, long[] ids) {
        Data.Builder builder = new Data.Builder()
                .putString(KEY_ACTION, action);

        if (ids != null) {
            builder.putLongArray(KEY_REMINDER_IDS, ids);
        }

        Data inputData = builder.build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ReminderWorker.class)
                .setInputData(inputData)
                .build();
        WorkManager.getInstance(context).enqueue(request);
    }

    /**
     * Schedules periodic updates (e.g., once a day) to check for due reminders.
     */
    public static void schedulePeriodicUpdate(@NonNull Context context) {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                ReminderWorker.class, 1, TimeUnit.DAYS)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
        );
    }
}
