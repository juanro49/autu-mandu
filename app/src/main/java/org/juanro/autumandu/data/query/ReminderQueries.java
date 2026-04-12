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

package org.juanro.autumandu.data.query;

import android.content.Context;

import java.util.Date;

import org.juanro.autumandu.model.dto.ReminderWithCar;
import org.juanro.autumandu.util.MileageUtil;
import org.juanro.autumandu.util.TimeSpan;

/**
 * Provides complex queries for reminders that involve logic beyond simple database access.
 */
public class ReminderQueries {
    private final Context context;
    private final ReminderWithCar reminderWithCar;

    public ReminderQueries(Context context, ReminderWithCar reminder) {
        this.context = context.getApplicationContext();
        this.reminderWithCar = reminder;
    }

    /**
     * Calculates the remaining distance until the reminder is due.
     * @return Distance to due, or null if no distance-based reminder is set.
     */
    public Integer getDistanceToDue() {
        var reminder = reminderWithCar.reminder();
        if (reminder.getAfterDistance() != null) {
            int latestMileage = MileageUtil.getLatestMileage(context, reminder.getCarId());
            return reminder.getStartMileage() + reminder.getAfterDistance() - latestMileage;
        } else {
            return null;
        }
    }

    /**
     * Calculates the remaining time until the reminder is due.
     * @return Time in milliseconds to due, or null if no time-based reminder is set.
     */
    public Long getTimeToDue() {
        var reminder = reminderWithCar.reminder();
        if (reminder.getAfterTimeSpanUnit() != null) {
            long now = new Date().getTime();
            var span = new TimeSpan(reminder.getAfterTimeSpanUnit(),
                    reminder.getAfterTimeSpanCount() == null ? 1 : reminder.getAfterTimeSpanCount());

            return span.addTo(reminder.getStartDate()).getTime() - now;
        } else {
            return null;
        }
    }

    /**
     * Checks if the reminder is currently snoozed.
     * @return true if snoozed, false otherwise.
     */
    public boolean isSnoozed() {
        var reminder = reminderWithCar.reminder();
        long now = new Date().getTime();
        return reminder.getSnoozedUntil() != null && reminder.getSnoozedUntil().getTime() >= now;
    }

    /**
     * Checks if the reminder is due (distance or time exceeded).
     * @return true if due, false otherwise.
     */
    public boolean isDue() {
        var distanceToDue = getDistanceToDue();
        if (distanceToDue != null && distanceToDue <= 0) {
            return true;
        }

        var timeToDue = getTimeToDue();
        return timeToDue != null && timeToDue <= 0;
    }
}
