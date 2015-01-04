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

package me.kuehle.carreport.util.reminder;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;

import java.util.Date;
import java.util.List;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.db.Reminder;
import me.kuehle.carreport.gui.MainActivity;
import me.kuehle.carreport.gui.PreferencesActivity;
import me.kuehle.carreport.gui.PreferencesRemindersFragment;

public class ReminderService extends IntentService {
    public static final String ACTION_UPDATE_NOTIFICATION = "me.kuehle.carreport.util.reminder.ReminderService.UPDATE_NOTIFICATION";
    public static final String ACTION_MARK_REMINDERS_DONE = "me.kuehle.carreport.util.reminder.ReminderService.MARK_REMINDERS_DONE";
    public static final String ACTION_DISMISS_REMINDERS = "me.kuehle.carreport.util.reminder.ReminderService.DISMISS_REMINDERS";
    public static final String ACTION_SNOOZE_REMINDERS = "me.kuehle.carreport.util.reminder.ReminderService.SNOOZE_REMINDERS";

    public static final String EXTRA_REMINDER_IDS = "REMINDER_IDS";

    private static final int NOTIFICATION_ID = 1;

    public ReminderService() {
        super("Reminder Service");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        long[] reminderIds = intent.getLongArrayExtra(EXTRA_REMINDER_IDS);

        switch (action) {
            case ACTION_UPDATE_NOTIFICATION:
                updateNotification(this);
                break;
            case ACTION_MARK_REMINDERS_DONE:
                markRemindersDone(this, reminderIds);
                break;
            case ACTION_DISMISS_REMINDERS:
                dismissReminders(this, reminderIds);
                break;
            case ACTION_SNOOZE_REMINDERS:
                snoozeReminders(this, reminderIds);
                break;
        }
    }

    public static PendingIntent getPendingIntent(Context context, String action) {
        return getPendingIntent(context, action, null);
    }

    public static PendingIntent getPendingIntent(Context context, String action,
                                                 List<Reminder> reminders) {
        long[] reminderIds = null;
        if (reminders != null) {
            reminderIds = new long[reminders.size()];
            for (int i = 0; i < reminders.size(); i++) {
                reminderIds[i] = reminders.get(i).id;
            }
        }

        Intent intent = new Intent(context, ReminderService.class);
        intent.setAction(action);
        intent.putExtra(ReminderService.EXTRA_REMINDER_IDS, reminderIds);
        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static void updateNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

        List<Reminder> dueReminders = Reminder.getAllDue(false);
        if (dueReminders.size() > 0) {
            notificationManager.notify(NOTIFICATION_ID, buildNotification(context, dueReminders));
        } else {
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }

    public static void markRemindersDone(Context context, long... reminderIds) {
        Date now = new Date();

        for (long id : reminderIds) {
            Reminder reminder = Reminder.load(Reminder.class, id);
            reminder.startDate = now;
            reminder.startMileage = reminder.car.getLatestMileage();
            reminder.snoozedUntil = null;
            reminder.notificationDismissed = false;
            reminder.save();
        }

        updateNotification(context);
    }

    public static void dismissReminders(Context context, long... reminderIds) {
        for (long id : reminderIds) {
            Reminder reminder = Reminder.load(Reminder.class, id);
            reminder.notificationDismissed = true;
            reminder.save();
        }

        updateNotification(context);
    }

    public static void snoozeReminders(Context context, long... reminderIds) {
        Preferences prefs = new Preferences(context);

        Date now = new Date();
        Date snoozedUntil = prefs.getReminderSnoozeDuration().addTo(now);

        for (long id : reminderIds) {
            Reminder reminder = Reminder.load(Reminder.class, id);
            reminder.snoozedUntil = snoozedUntil;
            reminder.save();
        }

        updateNotification(context);
    }

    private static Notification buildNotification(Context context, List<Reminder> reminders) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_notification)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        // Content intent
        Intent contentIntent = new Intent(context, PreferencesActivity.class);
        contentIntent.putExtra(PreferencesActivity.EXTRA_SHOW_FRAGMENT,
                PreferencesRemindersFragment.class.getName());
        contentIntent.putExtra(PreferencesActivity.EXTRA_SHOW_FRAGMENT_TITLE,
                R.string.pref_title_header_reminders);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(new Intent(context, MainActivity.class))
                .addNextIntent(contentIntent);
        PendingIntent pendingContentIntent = stackBuilder.getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingContentIntent);

        // Dismiss intent
        builder.setDeleteIntent(getPendingIntent(context, ACTION_DISMISS_REMINDERS, reminders));

        // Specific layouts for one and many reminders
        if (reminders.size() == 1) {
            Reminder reminder = reminders.get(0);

            builder
                    .setContentTitle(context.getString(R.string.notification_reminder_title_single,
                            reminder.title))
                    .setContentText(reminder.car.name)
                    .addAction(R.drawable.ic_check,
                            context.getString(R.string.notification_reminder_action_done),
                            getPendingIntent(context, ACTION_MARK_REMINDERS_DONE, reminders))
                    .addAction(R.drawable.ic_snooze,
                            context.getString(R.string.notification_reminder_action_snooze),
                            getPendingIntent(context, ACTION_SNOOZE_REMINDERS, reminders));
        } else {
            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
            inboxStyle.setBigContentTitle(context.getString(
                    R.string.notification_reminder_title_multiple));

            String[] reminderTitles = new String[reminders.size()];
            for (int i = 0; i < reminders.size(); i++) {
                reminderTitles[i] = reminders.get(i).title;
                inboxStyle.addLine(String.format("%s (%s)", reminders.get(i).title,
                        reminders.get(i).car.name));
            }

            builder
                    .setContentTitle(context.getString(
                            R.string.notification_reminder_title_multiple))
                    .setContentText(TextUtils.join(", ", reminderTitles))
                    .setNumber(reminders.size())
                    .setStyle(inboxStyle)
                    .addAction(R.drawable.ic_snooze,
                            context.getString(R.string.notification_reminder_action_snooze_all),
                            getPendingIntent(context, ACTION_SNOOZE_REMINDERS, reminders));
        }

        return builder.build();
    }
}
