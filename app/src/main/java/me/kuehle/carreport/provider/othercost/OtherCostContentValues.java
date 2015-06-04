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
package me.kuehle.carreport.provider.othercost;

import java.util.Date;

import android.content.ContentResolver;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import me.kuehle.carreport.provider.base.AbstractContentValues;

/**
 * Content values wrapper for the {@code other_cost} table.
 */
public class OtherCostContentValues extends AbstractContentValues {
    @Override
    public Uri uri() {
        return OtherCostColumns.CONTENT_URI;
    }

    /**
     * Update row(s) using the values stored by this object and the given selection.
     *
     * @param contentResolver The content resolver to use.
     * @param where The selection to use (can be {@code null}).
     */
    public int update(ContentResolver contentResolver, @Nullable OtherCostSelection where) {
        return contentResolver.update(uri(), values(), where == null ? null : where.sel(), where == null ? null : where.args());
    }

    /**
     * Display title of the cost.
     */
    public OtherCostContentValues putTitle(@NonNull String value) {
        if (value == null) throw new IllegalArgumentException("title must not be null");
        mContentValues.put(OtherCostColumns.TITLE, value);
        return this;
    }


    /**
     * Date on which the cost occured.
     */
    public OtherCostContentValues putDate(@NonNull Date value) {
        if (value == null) throw new IllegalArgumentException("date must not be null");
        mContentValues.put(OtherCostColumns.DATE, value.getTime());
        return this;
    }


    public OtherCostContentValues putDate(long value) {
        mContentValues.put(OtherCostColumns.DATE, value);
        return this;
    }

    /**
     * Mileage on which the cost occured.
     */
    public OtherCostContentValues putMileage(@Nullable Integer value) {
        mContentValues.put(OtherCostColumns.MILEAGE, value);
        return this;
    }

    public OtherCostContentValues putMileageNull() {
        mContentValues.putNull(OtherCostColumns.MILEAGE);
        return this;
    }

    /**
     * The price of the cost. If it is an income, the price it negative.
     */
    public OtherCostContentValues putPrice(float value) {
        mContentValues.put(OtherCostColumns.PRICE, value);
        return this;
    }


    /**
     * Recurrence information. Together with the recurrence_multiplier this gives a recurrence like every 5 days.
     */
    public OtherCostContentValues putRecurrenceInterval(@NonNull RecurrenceInterval value) {
        if (value == null) throw new IllegalArgumentException("recurrenceInterval must not be null");
        mContentValues.put(OtherCostColumns.RECURRENCE_INTERVAL, value.ordinal());
        return this;
    }


    /**
     * Recurrence information. Together with the recurrence_interval this gives a recurrence like every 5 days.
     */
    public OtherCostContentValues putRecurrenceMultiplier(int value) {
        mContentValues.put(OtherCostColumns.RECURRENCE_MULTIPLIER, value);
        return this;
    }


    /**
     * Date on which the recurrence ends or null, if there is no known end date yet.
     */
    public OtherCostContentValues putEndDate(@Nullable Date value) {
        mContentValues.put(OtherCostColumns.END_DATE, value == null ? null : value.getTime());
        return this;
    }

    public OtherCostContentValues putEndDateNull() {
        mContentValues.putNull(OtherCostColumns.END_DATE);
        return this;
    }

    public OtherCostContentValues putEndDate(@Nullable Long value) {
        mContentValues.put(OtherCostColumns.END_DATE, value);
        return this;
    }

    /**
     * A note for this cost. Just for display purposes.
     */
    public OtherCostContentValues putNote(@NonNull String value) {
        if (value == null) throw new IllegalArgumentException("note must not be null");
        mContentValues.put(OtherCostColumns.NOTE, value);
        return this;
    }


    public OtherCostContentValues putCarId(long value) {
        mContentValues.put(OtherCostColumns.CAR_ID, value);
        return this;
    }

}
