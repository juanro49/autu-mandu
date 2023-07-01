<<<<<<< a7a91dd4ab86f0bd1a4879c4fd2c55350602a99c:app/src/main/java/me/kuehle/carreport/util/reminder/ReminderEnablerReceiver.java
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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.joda.time.DateTime;

public class ReminderEnablerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        scheduleAlarms(context);
    }

    public static void scheduleAlarms(Context context) {
        PendingIntent pendingIntent = ReminderService.getPendingIntent(context,
                ReminderService.ACTION_UPDATE_NOTIFICATION);

        DateTime startTime = new DateTime().withTime(9, 0, 0, 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setInexactRepeating(AlarmManager.RTC, startTime.getMillis(),
                AlarmManager.INTERVAL_DAY, pendingIntent);
    }
}
=======
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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.joda.time.DateTime;

public class ReminderEnablerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        scheduleAlarms(context);
    }

    public static void scheduleAlarms(Context context) {
        PendingIntent pendingIntent = ReminderService.getPendingIntent(context,
                ReminderService.ACTION_UPDATE_NOTIFICATION, PendingIntent.FLAG_IMMUTABLE);

        DateTime startTime = new DateTime().withTime(9, 0, 0, 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setInexactRepeating(AlarmManager.RTC, startTime.getMillis(),
                AlarmManager.INTERVAL_DAY, pendingIntent);
    }
}
>>>>>>> Refactorización y mejoras en migración versiones BBDD:app/src/main/java/org/juanro/autumandu/util/reminder/ReminderEnablerReceiver.java
