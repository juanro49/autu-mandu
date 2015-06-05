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
package me.kuehle.carreport.data.query;

import android.content.Context;

import java.util.Date;

import me.kuehle.carreport.provider.reminder.ReminderCursor;
import me.kuehle.carreport.util.TimeSpan;

public class ReminderQueries {
    private Context mContext;
    private ReminderCursor mReminder;

    public ReminderQueries(Context context, ReminderCursor reminder) {
        mContext = context;
        mReminder = reminder;
    }

    public Integer getDistanceToDue() {
        if (mReminder.getAfterDistance() != null) {
            int latestMileage = CarQueries.getLatestMileage(mContext, mReminder.getCarId());
            return mReminder.getStartMileage() + mReminder.getAfterDistance() - latestMileage;
        } else {
            return null;
        }
    }

    public Long getTimeToDue() {
        if (mReminder.getAfterTimeSpanUnit() != null) {
            long now = new Date().getTime();
            TimeSpan span = new TimeSpan(mReminder.getAfterTimeSpanUnit(),
                    mReminder.getAfterTimeSpanCount() == null ? 1 : mReminder.getAfterTimeSpanCount());

            return span.addTo(mReminder.getStartDate()).getTime() - now;
        } else {
            return null;
        }
    }

    public boolean isSnoozed() {
        long now = new Date().getTime();
        return mReminder.getSnoozedUntil() != null && mReminder.getSnoozedUntil().getTime() >= now;
    }

    public boolean isDue() {
        if (mReminder.getAfterDistance() != null) {
            if (getDistanceToDue() <= 0) {
                return true;
            }
        }

        if (mReminder.getAfterTimeSpanUnit() != null) {
            if (getTimeToDue() <= 0) {
                return true;
            }
        }

        return false;
    }
}
