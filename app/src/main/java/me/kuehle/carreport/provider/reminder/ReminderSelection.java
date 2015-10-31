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
import android.database.Cursor;
import android.net.Uri;

import me.kuehle.carreport.provider.base.AbstractSelection;
import me.kuehle.carreport.provider.car.*;

/**
 * Selection for the {@code reminder} table.
 */
public class ReminderSelection extends AbstractSelection<ReminderSelection> {
    @Override
    protected Uri baseUri() {
        return ReminderColumns.CONTENT_URI;
    }

    /**
     * Query the given content resolver using this selection.
     *
     * @param contentResolver The content resolver to query.
     * @param projection A list of which columns to return. Passing null will return all columns, which is inefficient.
     * @param sortOrder How to order the rows, formatted as an SQL ORDER BY clause (excluding the ORDER BY itself). Passing null will use the default sort
     *            order, which may be unordered.
     * @return A {@code ReminderCursor} object, which is positioned before the first entry, or null.
     */
    public ReminderCursor query(ContentResolver contentResolver, String[] projection, String sortOrder) {
        Cursor cursor = contentResolver.query(uri(), projection, sel(), args(), sortOrder);
        if (cursor == null) return null;
        return new ReminderCursor(cursor);
    }

    /**
     * Equivalent of calling {@code query(contentResolver, projection, null)}.
     */
    public ReminderCursor query(ContentResolver contentResolver, String[] projection) {
        return query(contentResolver, projection, null);
    }

    /**
     * Equivalent of calling {@code query(contentResolver, projection, null, null)}.
     */
    public ReminderCursor query(ContentResolver contentResolver) {
        return query(contentResolver, null, null);
    }


    public ReminderSelection id(long... value) {
        addEquals("reminder." + ReminderColumns._ID, toObjectArray(value));
        return this;
    }

    public ReminderSelection title(String... value) {
        addEquals(ReminderColumns.TITLE, value);
        return this;
    }

    public ReminderSelection titleNot(String... value) {
        addNotEquals(ReminderColumns.TITLE, value);
        return this;
    }

    public ReminderSelection titleLike(String... value) {
        addLike(ReminderColumns.TITLE, value);
        return this;
    }

    public ReminderSelection titleContains(String... value) {
        addContains(ReminderColumns.TITLE, value);
        return this;
    }

    public ReminderSelection titleStartsWith(String... value) {
        addStartsWith(ReminderColumns.TITLE, value);
        return this;
    }

    public ReminderSelection titleEndsWith(String... value) {
        addEndsWith(ReminderColumns.TITLE, value);
        return this;
    }

    public ReminderSelection afterTimeSpanUnit(TimeSpanUnit... value) {
        addEquals(ReminderColumns.AFTER_TIME_SPAN_UNIT, value);
        return this;
    }

    public ReminderSelection afterTimeSpanUnitNot(TimeSpanUnit... value) {
        addNotEquals(ReminderColumns.AFTER_TIME_SPAN_UNIT, value);
        return this;
    }


    public ReminderSelection afterTimeSpanCount(Integer... value) {
        addEquals(ReminderColumns.AFTER_TIME_SPAN_COUNT, value);
        return this;
    }

    public ReminderSelection afterTimeSpanCountNot(Integer... value) {
        addNotEquals(ReminderColumns.AFTER_TIME_SPAN_COUNT, value);
        return this;
    }

    public ReminderSelection afterTimeSpanCountGt(int value) {
        addGreaterThan(ReminderColumns.AFTER_TIME_SPAN_COUNT, value);
        return this;
    }

    public ReminderSelection afterTimeSpanCountGtEq(int value) {
        addGreaterThanOrEquals(ReminderColumns.AFTER_TIME_SPAN_COUNT, value);
        return this;
    }

    public ReminderSelection afterTimeSpanCountLt(int value) {
        addLessThan(ReminderColumns.AFTER_TIME_SPAN_COUNT, value);
        return this;
    }

    public ReminderSelection afterTimeSpanCountLtEq(int value) {
        addLessThanOrEquals(ReminderColumns.AFTER_TIME_SPAN_COUNT, value);
        return this;
    }

    public ReminderSelection afterDistance(Integer... value) {
        addEquals(ReminderColumns.AFTER_DISTANCE, value);
        return this;
    }

    public ReminderSelection afterDistanceNot(Integer... value) {
        addNotEquals(ReminderColumns.AFTER_DISTANCE, value);
        return this;
    }

    public ReminderSelection afterDistanceGt(int value) {
        addGreaterThan(ReminderColumns.AFTER_DISTANCE, value);
        return this;
    }

    public ReminderSelection afterDistanceGtEq(int value) {
        addGreaterThanOrEquals(ReminderColumns.AFTER_DISTANCE, value);
        return this;
    }

    public ReminderSelection afterDistanceLt(int value) {
        addLessThan(ReminderColumns.AFTER_DISTANCE, value);
        return this;
    }

    public ReminderSelection afterDistanceLtEq(int value) {
        addLessThanOrEquals(ReminderColumns.AFTER_DISTANCE, value);
        return this;
    }

    public ReminderSelection startDate(Date... value) {
        addEquals(ReminderColumns.START_DATE, value);
        return this;
    }

    public ReminderSelection startDateNot(Date... value) {
        addNotEquals(ReminderColumns.START_DATE, value);
        return this;
    }

    public ReminderSelection startDate(long... value) {
        addEquals(ReminderColumns.START_DATE, toObjectArray(value));
        return this;
    }

    public ReminderSelection startDateAfter(Date value) {
        addGreaterThan(ReminderColumns.START_DATE, value);
        return this;
    }

    public ReminderSelection startDateAfterEq(Date value) {
        addGreaterThanOrEquals(ReminderColumns.START_DATE, value);
        return this;
    }

    public ReminderSelection startDateBefore(Date value) {
        addLessThan(ReminderColumns.START_DATE, value);
        return this;
    }

    public ReminderSelection startDateBeforeEq(Date value) {
        addLessThanOrEquals(ReminderColumns.START_DATE, value);
        return this;
    }

    public ReminderSelection startMileage(int... value) {
        addEquals(ReminderColumns.START_MILEAGE, toObjectArray(value));
        return this;
    }

    public ReminderSelection startMileageNot(int... value) {
        addNotEquals(ReminderColumns.START_MILEAGE, toObjectArray(value));
        return this;
    }

    public ReminderSelection startMileageGt(int value) {
        addGreaterThan(ReminderColumns.START_MILEAGE, value);
        return this;
    }

    public ReminderSelection startMileageGtEq(int value) {
        addGreaterThanOrEquals(ReminderColumns.START_MILEAGE, value);
        return this;
    }

    public ReminderSelection startMileageLt(int value) {
        addLessThan(ReminderColumns.START_MILEAGE, value);
        return this;
    }

    public ReminderSelection startMileageLtEq(int value) {
        addLessThanOrEquals(ReminderColumns.START_MILEAGE, value);
        return this;
    }

    public ReminderSelection notificationDismissed(boolean value) {
        addEquals(ReminderColumns.NOTIFICATION_DISMISSED, toObjectArray(value));
        return this;
    }

    public ReminderSelection snoozedUntil(Date... value) {
        addEquals(ReminderColumns.SNOOZED_UNTIL, value);
        return this;
    }

    public ReminderSelection snoozedUntilNot(Date... value) {
        addNotEquals(ReminderColumns.SNOOZED_UNTIL, value);
        return this;
    }

    public ReminderSelection snoozedUntil(Long... value) {
        addEquals(ReminderColumns.SNOOZED_UNTIL, value);
        return this;
    }

    public ReminderSelection snoozedUntilAfter(Date value) {
        addGreaterThan(ReminderColumns.SNOOZED_UNTIL, value);
        return this;
    }

    public ReminderSelection snoozedUntilAfterEq(Date value) {
        addGreaterThanOrEquals(ReminderColumns.SNOOZED_UNTIL, value);
        return this;
    }

    public ReminderSelection snoozedUntilBefore(Date value) {
        addLessThan(ReminderColumns.SNOOZED_UNTIL, value);
        return this;
    }

    public ReminderSelection snoozedUntilBeforeEq(Date value) {
        addLessThanOrEquals(ReminderColumns.SNOOZED_UNTIL, value);
        return this;
    }

    public ReminderSelection carId(long... value) {
        addEquals(ReminderColumns.CAR_ID, toObjectArray(value));
        return this;
    }

    public ReminderSelection carIdNot(long... value) {
        addNotEquals(ReminderColumns.CAR_ID, toObjectArray(value));
        return this;
    }

    public ReminderSelection carIdGt(long value) {
        addGreaterThan(ReminderColumns.CAR_ID, value);
        return this;
    }

    public ReminderSelection carIdGtEq(long value) {
        addGreaterThanOrEquals(ReminderColumns.CAR_ID, value);
        return this;
    }

    public ReminderSelection carIdLt(long value) {
        addLessThan(ReminderColumns.CAR_ID, value);
        return this;
    }

    public ReminderSelection carIdLtEq(long value) {
        addLessThanOrEquals(ReminderColumns.CAR_ID, value);
        return this;
    }

    public ReminderSelection carName(String... value) {
        addEquals(CarColumns.NAME, value);
        return this;
    }

    public ReminderSelection carNameNot(String... value) {
        addNotEquals(CarColumns.NAME, value);
        return this;
    }

    public ReminderSelection carNameLike(String... value) {
        addLike(CarColumns.NAME, value);
        return this;
    }

    public ReminderSelection carNameContains(String... value) {
        addContains(CarColumns.NAME, value);
        return this;
    }

    public ReminderSelection carNameStartsWith(String... value) {
        addStartsWith(CarColumns.NAME, value);
        return this;
    }

    public ReminderSelection carNameEndsWith(String... value) {
        addEndsWith(CarColumns.NAME, value);
        return this;
    }

    public ReminderSelection carColor(int... value) {
        addEquals(CarColumns.COLOR, toObjectArray(value));
        return this;
    }

    public ReminderSelection carColorNot(int... value) {
        addNotEquals(CarColumns.COLOR, toObjectArray(value));
        return this;
    }

    public ReminderSelection carColorGt(int value) {
        addGreaterThan(CarColumns.COLOR, value);
        return this;
    }

    public ReminderSelection carColorGtEq(int value) {
        addGreaterThanOrEquals(CarColumns.COLOR, value);
        return this;
    }

    public ReminderSelection carColorLt(int value) {
        addLessThan(CarColumns.COLOR, value);
        return this;
    }

    public ReminderSelection carColorLtEq(int value) {
        addLessThanOrEquals(CarColumns.COLOR, value);
        return this;
    }

    public ReminderSelection carInitialMileage(int... value) {
        addEquals(CarColumns.INITIAL_MILEAGE, toObjectArray(value));
        return this;
    }

    public ReminderSelection carInitialMileageNot(int... value) {
        addNotEquals(CarColumns.INITIAL_MILEAGE, toObjectArray(value));
        return this;
    }

    public ReminderSelection carInitialMileageGt(int value) {
        addGreaterThan(CarColumns.INITIAL_MILEAGE, value);
        return this;
    }

    public ReminderSelection carInitialMileageGtEq(int value) {
        addGreaterThanOrEquals(CarColumns.INITIAL_MILEAGE, value);
        return this;
    }

    public ReminderSelection carInitialMileageLt(int value) {
        addLessThan(CarColumns.INITIAL_MILEAGE, value);
        return this;
    }

    public ReminderSelection carInitialMileageLtEq(int value) {
        addLessThanOrEquals(CarColumns.INITIAL_MILEAGE, value);
        return this;
    }

    public ReminderSelection carSuspendedSince(Date... value) {
        addEquals(CarColumns.SUSPENDED_SINCE, value);
        return this;
    }

    public ReminderSelection carSuspendedSinceNot(Date... value) {
        addNotEquals(CarColumns.SUSPENDED_SINCE, value);
        return this;
    }

    public ReminderSelection carSuspendedSince(Long... value) {
        addEquals(CarColumns.SUSPENDED_SINCE, value);
        return this;
    }

    public ReminderSelection carSuspendedSinceAfter(Date value) {
        addGreaterThan(CarColumns.SUSPENDED_SINCE, value);
        return this;
    }

    public ReminderSelection carSuspendedSinceAfterEq(Date value) {
        addGreaterThanOrEquals(CarColumns.SUSPENDED_SINCE, value);
        return this;
    }

    public ReminderSelection carSuspendedSinceBefore(Date value) {
        addLessThan(CarColumns.SUSPENDED_SINCE, value);
        return this;
    }

    public ReminderSelection carSuspendedSinceBeforeEq(Date value) {
        addLessThanOrEquals(CarColumns.SUSPENDED_SINCE, value);
        return this;
    }
}
