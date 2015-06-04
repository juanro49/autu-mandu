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

import android.content.ContentResolver;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import me.kuehle.carreport.provider.base.AbstractContentValues;

/**
 * Content values wrapper for the {@code reminder} table.
 */
public class ReminderContentValues extends AbstractContentValues {
    @Override
    public Uri uri() {
        return ReminderColumns.CONTENT_URI;
    }

    /**
     * Update row(s) using the values stored by this object and the given selection.
     *
     * @param contentResolver The content resolver to use.
     * @param where The selection to use (can be {@code null}).
     */
    public int update(ContentResolver contentResolver, @Nullable ReminderSelection where) {
        return contentResolver.update(uri(), values(), where == null ? null : where.sel(), where == null ? null : where.args());
    }

    /**
     * Display title of the reminder.
     */
    public ReminderContentValues putTitle(@NonNull String value) {
        if (value == null) throw new IllegalArgumentException("title must not be null");
        mContentValues.put(ReminderColumns.TITLE, value);
        return this;
    }


    /**
     * Time span after which the reminder should go off. Together with the after_time_span_count this gives a time span like 3 days.
     */
    public ReminderContentValues putAfterTimeSpanUnit(@Nullable TimeSpanUnit value) {
        mContentValues.put(ReminderColumns.AFTER_TIME_SPAN_UNIT, value == null ? null : value.ordinal());
        return this;
    }

    public ReminderContentValues putAfterTimeSpanUnitNull() {
        mContentValues.putNull(ReminderColumns.AFTER_TIME_SPAN_UNIT);
        return this;
    }

    /**
     * Time span after which the reminder should go off. Together with the after_time_span_unit this gives a time span like every 3 days.
     */
    public ReminderContentValues putAfterTimeSpanCount(@Nullable Integer value) {
        mContentValues.put(ReminderColumns.AFTER_TIME_SPAN_COUNT, value);
        return this;
    }

    public ReminderContentValues putAfterTimeSpanCountNull() {
        mContentValues.putNull(ReminderColumns.AFTER_TIME_SPAN_COUNT);
        return this;
    }

    /**
     * Distance after which the reminder should go off.
     */
    public ReminderContentValues putAfterDistance(@Nullable Integer value) {
        mContentValues.put(ReminderColumns.AFTER_DISTANCE, value);
        return this;
    }

    public ReminderContentValues putAfterDistanceNull() {
        mContentValues.putNull(ReminderColumns.AFTER_DISTANCE);
        return this;
    }

    /**
     * Date on which the reminder starts to count.
     */
    public ReminderContentValues putStartDate(@NonNull Date value) {
        if (value == null) throw new IllegalArgumentException("startDate must not be null");
        mContentValues.put(ReminderColumns.START_DATE, value.getTime());
        return this;
    }


    public ReminderContentValues putStartDate(long value) {
        mContentValues.put(ReminderColumns.START_DATE, value);
        return this;
    }

    /**
     * Mileage on which the reminder starts to count.
     */
    public ReminderContentValues putStartMileage(int value) {
        mContentValues.put(ReminderColumns.START_MILEAGE, value);
        return this;
    }


    /**
     * Indicates if the reminder has gone off, but the notification has been dismissed.
     */
    public ReminderContentValues putNotificationDismissed(boolean value) {
        mContentValues.put(ReminderColumns.NOTIFICATION_DISMISSED, value);
        return this;
    }


    /**
     * When the reminder goes off, the user can snooze it. In this case the field contains the date on which the reminder will go off again.
     */
    public ReminderContentValues putSnoozedUntil(@Nullable Date value) {
        mContentValues.put(ReminderColumns.SNOOZED_UNTIL, value == null ? null : value.getTime());
        return this;
    }

    public ReminderContentValues putSnoozedUntilNull() {
        mContentValues.putNull(ReminderColumns.SNOOZED_UNTIL);
        return this;
    }

    public ReminderContentValues putSnoozedUntil(@Nullable Long value) {
        mContentValues.put(ReminderColumns.SNOOZED_UNTIL, value);
        return this;
    }

    public ReminderContentValues putCarId(long value) {
        mContentValues.put(ReminderColumns.CAR_ID, value);
        return this;
    }

}
