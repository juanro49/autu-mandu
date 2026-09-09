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

package org.juanro.autumandu.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import org.juanro.autumandu.data.query.ReminderQueries;
import org.juanro.autumandu.model.dao.ReminderDao;
import org.juanro.autumandu.model.dto.ReminderWithCar;
import org.juanro.autumandu.util.reminder.ReminderService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class RemindersViewModel extends AndroidViewModel {
    private final ReminderDao reminderDao;
    private final MediatorLiveData<List<ReminderWithCar>> reminders = new MediatorLiveData<>();
    private static final Executor DB_EXECUTOR = Executors.newFixedThreadPool(4);

    @Inject
    public RemindersViewModel(Application application, ReminderDao reminderDao) {
        super(application);
        this.reminderDao = reminderDao;

        LiveData<List<ReminderWithCar>> source = reminderDao.getAllWithCarLiveData();
        reminders.addSource(source, list -> {
            if (list == null) {
                reminders.setValue(null);
                return;
            }

            DB_EXECUTOR.execute(() -> {
                List<ReminderWithCar> processedList = new ArrayList<>(list);
                for (ReminderWithCar item : processedList) {
                    ReminderQueries queries = new ReminderQueries(getApplication(), item);
                    item.setDue(queries.isDue());
                    item.setSnoozed(queries.isSnoozed());
                    item.setDistanceToDue(queries.getDistanceToDue());
                    item.setTimeToDue(queries.getTimeToDue());
                }
                reminders.postValue(processedList);
            });
        });
    }

    public LiveData<List<ReminderWithCar>> getReminders() {
        return reminders;
    }

    public void deleteReminders(long[] ids) {
        DB_EXECUTOR.execute(() -> {
            for (long id : ids) {
                reminderDao.deleteById(id);
            }
        });
    }
    public void markAsDone(long id) {
        DB_EXECUTOR.execute(() -> ReminderService.markRemindersDone(getApplication(), id));
    }

    public void snooze(long id) {
        DB_EXECUTOR.execute(() -> ReminderService.snoozeReminders(getApplication(), id));
    }
}
