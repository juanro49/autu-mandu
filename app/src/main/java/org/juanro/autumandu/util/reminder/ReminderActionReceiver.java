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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Receiver to handle quick actions from reminder notifications.
 * Replaces the old IntentService behavior using WorkManager.
 */
public class ReminderActionReceiver extends BroadcastReceiver {
    private static final String TAG = "ReminderActionReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        String action = intent.getAction();
        long[] reminderIds = intent.getLongArrayExtra(ReminderService.EXTRA_REMINDER_IDS);

        Log.d(TAG, "Action received: " + action);

        if (reminderIds == null || reminderIds.length == 0) {
            if (ReminderService.ACTION_UPDATE_NOTIFICATION.equals(action)) {
                ReminderWorker.enqueueUpdate(context);
            }
            return;
        }

        // Delegamos todas las acciones a ReminderWorker (WorkManager)
        // para unificar la lógica de segundo plano y evitar bloqueos en el hilo UI.
        switch (action) {
            case ReminderService.ACTION_MARK_REMINDERS_DONE ->
                ReminderWorker.enqueueAction(context, ReminderWorker.ACTION_MARK_DONE, reminderIds);
            case ReminderService.ACTION_DISMISS_REMINDERS ->
                ReminderWorker.enqueueAction(context, ReminderWorker.ACTION_DISMISS, reminderIds);
            case ReminderService.ACTION_SNOOZE_REMINDERS ->
                ReminderWorker.enqueueAction(context, ReminderWorker.ACTION_SNOOZE, reminderIds);
            case ReminderService.ACTION_UPDATE_NOTIFICATION ->
                ReminderWorker.enqueueUpdate(context);
        }
    }
}
