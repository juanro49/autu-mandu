/*
 * Copyright 2026 Juanro49
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

package org.juanro.autumandu.model.dto;

import androidx.room.Embedded;
import androidx.room.Ignore;
import org.juanro.autumandu.model.entity.Reminder;

public class ReminderWithCar {
    @Embedded
    private Reminder reminder;
    private String carName;

    // Calculated fields to avoid DB access on UI thread
    @Ignore
    private boolean due;
    @Ignore
    private boolean snoozed;
    @Ignore
    private Integer distanceToDue;
    @Ignore
    private Long timeToDue;

    public ReminderWithCar(Reminder reminder, String carName) {
        this.reminder = reminder;
        this.carName = carName;
    }

    public Reminder reminder() {
        return reminder;
    }

    public String carName() {
        return carName;
    }

    public boolean isDue() {
        return due;
    }

    public void setDue(boolean due) {
        this.due = due;
    }

    public boolean isSnoozed() {
        return snoozed;
    }

    public void setSnoozed(boolean snoozed) {
        this.snoozed = snoozed;
    }

    public Integer getDistanceToDue() {
        return distanceToDue;
    }

    public void setDistanceToDue(Integer distanceToDue) {
        this.distanceToDue = distanceToDue;
    }

    public Long getTimeToDue() {
        return timeToDue;
    }

    public void setTimeToDue(Long timeToDue) {
        this.timeToDue = timeToDue;
    }
}
