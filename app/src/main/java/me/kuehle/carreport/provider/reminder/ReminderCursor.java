/*
 * Copyright 2015 Jan Kühle
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
package me.kuehle.carreport.provider.reminder;

import java.util.Date;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import me.kuehle.carreport.provider.base.AbstractCursor;
import me.kuehle.carreport.provider.car.*;

/**
 * Cursor wrapper for the {@code reminder} table.
 */
public class ReminderCursor extends AbstractCursor implements ReminderModel {
    public ReminderCursor(Cursor cursor) {
        super(cursor);
    }

    /**
     * Primary key.
     */
    public long getId() {
        Long res = getLongOrNull(ReminderColumns._ID);
        if (res == null)
            throw new NullPointerException("The value of '_id' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    /**
     * Display title of the reminder.
     * Cannot be {@code null}.
     */
    @NonNull
    public String getTitle() {
        String res = getStringOrNull(ReminderColumns.TITLE);
        if (res == null)
            throw new NullPointerException("The value of 'title' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    /**
     * Time span after which the reminder should go off. Together with the after_time_span_count this gives a time span like 3 days.
     * Can be {@code null}.
     */
    @Nullable
    public TimeSpanUnit getAfterTimeSpanUnit() {
        Integer intValue = getIntegerOrNull(ReminderColumns.AFTER_TIME_SPAN_UNIT);
        if (intValue == null) return null;
        return TimeSpanUnit.values()[intValue];
    }

    /**
     * Time span after which the reminder should go off. Together with the after_time_span_unit this gives a time span like every 3 days.
     * Can be {@code null}.
     */
    @Nullable
    public Integer getAfterTimeSpanCount() {
        Integer res = getIntegerOrNull(ReminderColumns.AFTER_TIME_SPAN_COUNT);
        return res;
    }

    /**
     * Distance after which the reminder should go off.
     * Can be {@code null}.
     */
    @Nullable
    public Integer getAfterDistance() {
        Integer res = getIntegerOrNull(ReminderColumns.AFTER_DISTANCE);
        return res;
    }

    /**
     * Date on which the reminder starts to count.
     * Cannot be {@code null}.
     */
    @NonNull
    public Date getStartDate() {
        Date res = getDateOrNull(ReminderColumns.START_DATE);
        if (res == null)
            throw new NullPointerException("The value of 'start_date' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    /**
     * Mileage on which the reminder starts to count.
     */
    public int getStartMileage() {
        Integer res = getIntegerOrNull(ReminderColumns.START_MILEAGE);
        if (res == null)
            throw new NullPointerException("The value of 'start_mileage' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    /**
     * Indicates if the reminder has gone off, but the notification has been dismissed.
     */
    public boolean getNotificationDismissed() {
        Boolean res = getBooleanOrNull(ReminderColumns.NOTIFICATION_DISMISSED);
        if (res == null)
            throw new NullPointerException("The value of 'notification_dismissed' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    /**
     * When the reminder goes off, the user can snooze it. In this case the field contains the date on which the reminder will go off again.
     * Can be {@code null}.
     */
    @Nullable
    public Date getSnoozedUntil() {
        Date res = getDateOrNull(ReminderColumns.SNOOZED_UNTIL);
        return res;
    }

    /**
     * Get the {@code car_id} value.
     */
    public long getCarId() {
        Long res = getLongOrNull(ReminderColumns.CAR_ID);
        if (res == null)
            throw new NullPointerException("The value of 'car_id' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    /**
     * Name of the car. Only for display purposes.
     * Cannot be {@code null}.
     */
    @NonNull
    public String getCarName() {
        String res = getStringOrNull(CarColumns.NAME);
        if (res == null)
            throw new NullPointerException("The value of 'name' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    /**
     * Color of the car in android color representation.
     */
    public int getCarColor() {
        Integer res = getIntegerOrNull(CarColumns.COLOR);
        if (res == null)
            throw new NullPointerException("The value of 'color' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    /**
     * Initial mileage of the car, when it starts to be used in the app.
     */
    public int getCarInitialMileage() {
        Integer res = getIntegerOrNull(CarColumns.INITIAL_MILEAGE);
        if (res == null)
            throw new NullPointerException("The value of 'initial_mileage' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    /**
     * When the car has been suspended, this contains the start date.
     * Can be {@code null}.
     */
    @Nullable
    public Date getCarSuspendedSince() {
        Date res = getDateOrNull(CarColumns.SUSPENDED_SINCE);
        return res;
    }
}
