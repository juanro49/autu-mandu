/*
 * Copyright 2015 Jan Kühle
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.juanro.autumandu.util.reminder;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Date;

import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;
import org.juanro.autumandu.data.query.ReminderQueries;
import org.juanro.autumandu.gui.MainActivity;
import org.juanro.autumandu.gui.pref.PreferencesActivity;
import org.juanro.autumandu.gui.pref.PreferencesRemindersFragment;
import org.juanro.autumandu.model.AutuManduDatabase;
import org.juanro.autumandu.model.dto.ReminderWithCar;
import org.juanro.autumandu.util.MileageUtil;

/**
 * Service for handling reminders and showing notifications.
 */
public class ReminderService {
    public static final String ACTION_UPDATE_NOTIFICATION = "org.juanro.autumandu.util.reminder.ReminderService.UPDATE_NOTIFICATION";
    public static final String ACTION_MARK_REMINDERS_DONE = "org.juanro.autumandu.util.reminder.ReminderService.MARK_REMINDERS_DONE";
    public static final String ACTION_DISMISS_REMINDERS = "org.juanro.autumandu.util.reminder.ReminderService.DISMISS_REMINDERS";
    public static final String ACTION_SNOOZE_REMINDERS = "org.juanro.autumandu.util.reminder.ReminderService.SNOOZE_REMINDERS";

    public static final String EXTRA_REMINDER_IDS = "REMINDER_IDS";

    private static final int NOTIFICATION_ID = 1;
    private static final String NOTIFICATION_CHANNEL_ID = "reminders";

    private ReminderService() {
        // Utility class
    }

    /**
     * Creates a PendingIntent for a Broadcast to handle reminder actions.
     */
    public static PendingIntent getPendingIntent(Context context, String action, long... reminderIds) {
        var intent = new Intent(context, ReminderActionReceiver.class);
        intent.setAction(action);
        intent.putExtra(ReminderService.EXTRA_REMINDER_IDS, reminderIds);
        return PendingIntent.getBroadcast(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void updateNotification(Context context) {
        var notificationManager = NotificationManagerCompat.from(context);
        long[] dueIds = getDueReminderIds(context);

        if (dueIds.length > 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                var systemManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (systemManager != null) {
                    systemManager.createNotificationChannel(buildNotificationChannel(context));
                }
            }

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(NOTIFICATION_ID, buildNotification(context, dueIds));
            }
        } else {
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }

    @NonNull
    private static long[] getDueReminderIds(Context context) {
        var db = AutuManduDatabase.getInstance(context);
        var reminders = db.getReminderDao().getAllWithCar();
        var dueReminderIdsList = new ArrayList<Long>();

        for (var reminder : reminders) {
            var queries = new ReminderQueries(context, reminder);
            if (reminder.reminder().isNotificationDismissed() || queries.isSnoozed()) {
                continue;
            }

            if (queries.isDue()) {
                dueReminderIdsList.add(reminder.reminder().getId());
            }
        }

        var ids = new long[dueReminderIdsList.size()];
        for (int i = 0; i < dueReminderIdsList.size(); i++) {
            ids[i] = dueReminderIdsList.get(i);
        }
        return ids;
    }

    public static void markRemindersDone(Context context, long... reminderIds) {
        var now = new Date();
        var db = AutuManduDatabase.getInstance(context);

        for (long id : reminderIds) {
            var reminder = db.getReminderDao().getById(id);
            if (reminder != null) {
                reminder.setStartDate(now);
                reminder.setStartMileage(MileageUtil.getLatestMileage(context, reminder.getCarId()));
                reminder.setSnoozedUntil(null);
                reminder.setNotificationDismissed(false);
                db.getReminderDao().update(reminder);
            }
        }

        updateNotification(context);
    }

    public static void dismissReminders(Context context, long... reminderIds) {
        var db = AutuManduDatabase.getInstance(context);
        for (long id : reminderIds) {
            var reminder = db.getReminderDao().getById(id);
            if (reminder != null) {
                reminder.setNotificationDismissed(true);
                db.getReminderDao().update(reminder);
            }
        }

        updateNotification(context);
    }

    public static void snoozeReminders(Context context, long... reminderIds) {
        var preferences = new Preferences(context);
        var db = AutuManduDatabase.getInstance(context);

        var now = new Date();
        var snoozedUntil = preferences.getReminderSnoozeDuration().addTo(now);

        for (long id : reminderIds) {
            var reminder = db.getReminderDao().getById(id);
            if (reminder != null) {
                reminder.setSnoozedUntil(snoozedUntil);
                db.getReminderDao().update(reminder);
            }
        }

        updateNotification(context);
    }

    private static Notification buildNotification(Context context, long... reminderIds) {
        var db = AutuManduDatabase.getInstance(context);
        var reminders = new ArrayList<ReminderWithCar>();
        for (long id : reminderIds) {
            reminders.add(db.getReminderDao().getByIdWithCar(id));
        }

        var builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_c_notification_24dp)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        // Content intent
        var contentIntent = new Intent(context, PreferencesActivity.class);
        contentIntent.putExtra(PreferencesActivity.EXTRA_SHOW_FRAGMENT,
                PreferencesRemindersFragment.class.getName());
        contentIntent.putExtra(PreferencesActivity.EXTRA_SHOW_FRAGMENT_TITLE,
                R.string.pref_title_header_reminders);

        var stackBuilder = TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(new Intent(context, MainActivity.class))
                .addNextIntent(contentIntent);
        var pendingContentIntent = stackBuilder.getPendingIntent(0,
            PendingIntent.FLAG_UPDATE_CURRENT |  PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(pendingContentIntent);

        // Dismiss intent
        builder.setDeleteIntent(getPendingIntent(context, ACTION_DISMISS_REMINDERS, reminderIds));

        // Specific layouts for one and many reminders
        if (reminders.size() == 1) {
            var reminder = reminders.get(0);

            builder
                    .setContentTitle(context.getString(R.string.notification_reminder_title_single,
                            reminder.reminder().getTitle()))
                    .setContentText(reminder.carName())
                    .addAction(R.drawable.ic_check_24dp,
                            context.getString(R.string.notification_reminder_action_done),
                            getPendingIntent(context, ACTION_MARK_REMINDERS_DONE, reminderIds))
                    .addAction(R.drawable.ic_snooze_24dp,
                            context.getString(R.string.notification_reminder_action_snooze),
                            getPendingIntent(context, ACTION_SNOOZE_REMINDERS, reminderIds));
        } else {
            var inboxStyle = new NotificationCompat.InboxStyle();
            inboxStyle.setBigContentTitle(context.getString(
                    R.string.notification_reminder_title_multiple));

            var reminderTitles = new ArrayList<String>(reminders.size());
            for (var reminder : reminders) {
                reminderTitles.add(reminder.reminder().getTitle());
                inboxStyle.addLine(String.format("%s (%s)", reminder.reminder().getTitle(),
                        reminder.carName()));
            }

            builder
                    .setContentTitle(context.getString(
                            R.string.notification_reminder_title_multiple))
                    .setContentText(TextUtils.join(", ", reminderTitles))
                    .setNumber(reminders.size())
                    .setStyle(inboxStyle)
                    .addAction(R.drawable.ic_snooze_24dp,
                            context.getString(R.string.notification_reminder_action_snooze_all),
                            getPendingIntent(context, ACTION_SNOOZE_REMINDERS, reminderIds));
        }

        return builder.build();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static NotificationChannel buildNotificationChannel(Context context) {
        return new NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.notification_reminder_channel),
                NotificationManager.IMPORTANCE_LOW);
    }
}
