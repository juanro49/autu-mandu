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

import me.kuehle.carreport.provider.base.BaseModel;

import java.util.Date;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * A reminder for a certain event of a car.
 */
public interface ReminderModel extends BaseModel {

    /**
     * Display title of the reminder.
     * Cannot be {@code null}.
     */
    @NonNull
    String getTitle();

    /**
     * Time span after which the reminder should go off. Together with the after_time_span_count this gives a time span like 3 days.
     * Can be {@code null}.
     */
    @Nullable
    TimeSpanUnit getAfterTimeSpanUnit();

    /**
     * Time span after which the reminder should go off. Together with the after_time_span_unit this gives a time span like every 3 days.
     * Can be {@code null}.
     */
    @Nullable
    Integer getAfterTimeSpanCount();

    /**
     * Distance after which the reminder should go off.
     * Can be {@code null}.
     */
    @Nullable
    Integer getAfterDistance();

    /**
     * Date on which the reminder starts to count.
     * Cannot be {@code null}.
     */
    @NonNull
    Date getStartDate();

    /**
     * Mileage on which the reminder starts to count.
     */
    int getStartMileage();

    /**
     * Indicates if the reminder has gone off, but the notification has been dismissed.
     */
    boolean getNotificationDismissed();

    /**
     * When the reminder goes off, the user can snooze it. In this case the field contains the date on which the reminder will go off again.
     * Can be {@code null}.
     */
    @Nullable
    Date getSnoozedUntil();

    /**
     * Get the {@code car_id} value.
     */
    long getCarId();
}
