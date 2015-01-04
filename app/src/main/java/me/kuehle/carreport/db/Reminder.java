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

package me.kuehle.carreport.db;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import me.kuehle.carreport.db.query.SafeSelect;
import me.kuehle.carreport.util.TimeSpan;

@Table(name = "reminders")
public class Reminder extends Model {
    @Column(name = "title", notNull = true)
    public String title;

    @Column(name = "after_time")
    public TimeSpan afterTime;

    @Column(name = "after_distance")
    public Integer afterDistance;

    @Column(name = "start_date", notNull = true)
    public Date startDate;

    @Column(name = "start_mileage", notNull = true)
    public int startMileage;

    @Column(name = "car", notNull = true, onUpdate = Column.ForeignKeyAction.CASCADE, onDelete = Column.ForeignKeyAction.CASCADE)
    public Car car;

    @Column(name = "notification_dismissed", notNull = true)
    public boolean notificationDismissed;

    @Column(name = "snoozed_until")
    public Date snoozedUntil;

    public Reminder() {
        super();
    }

    public Reminder(String title, TimeSpan afterTime, Integer afterDistance, Date startDate,
                    int startMileage, Car car) {
        super();
        this.title = title;
        this.afterTime = afterTime;
        this.afterDistance = afterDistance;
        this.startDate = startDate;
        this.startMileage = startMileage;
        this.car = car;
        this.notificationDismissed = false;
        this.snoozedUntil = null;
    }

    public boolean isDue() {
        if (afterDistance != null) {
            int latestMileage = car.getLatestMileage();
            if (startMileage + afterDistance <= latestMileage) {
                return true;
            }
        }

        if (afterTime != null) {
            long now = new Date().getTime();
            if (afterTime.addTo(startDate).getTime() <= now) {
                return true;
            }
        }

        return false;
    }

    public boolean isSnoozed() {
        long now = new Date().getTime();
        return snoozedUntil != null && snoozedUntil.getTime() >= now;
    }

    public static List<Reminder> getAll() {
        return SafeSelect.from(Reminder.class)
                .orderBy("title ASC")
                .execute();
    }

    public static List<Reminder> getAllDue(boolean includeDismissed) {
        List<Reminder> all = getAll();
        List<Reminder> due = new ArrayList<>();

        for (Reminder reminder : all) {
            // Filter dismissed notifications
            if (reminder.notificationDismissed && !includeDismissed) {
                continue;
            }

            // Filter snoozed notifications
            if (reminder.isSnoozed()) {
                continue;
            }

            if (reminder.isDue()) {
                due.add(reminder);
            }
        }

        return due;
    }
}
