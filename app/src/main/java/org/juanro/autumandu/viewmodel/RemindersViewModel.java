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

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import org.juanro.autumandu.data.query.ReminderQueries;
import org.juanro.autumandu.model.AutuManduDatabase;
import org.juanro.autumandu.model.dto.ReminderWithCar;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class RemindersViewModel extends AndroidViewModel {
    private final AutuManduDatabase db;
    private final MediatorLiveData<List<ReminderWithCar>> reminders = new MediatorLiveData<>();
    private static final Executor DB_EXECUTOR = Executors.newFixedThreadPool(4);

    public RemindersViewModel(@NonNull Application application) {
        super(application);
        db = AutuManduDatabase.getInstance(application);

        LiveData<List<ReminderWithCar>> source = db.getReminderDao().getAllWithCarLiveData();
        reminders.addSource(source, list -> {
            if (list == null) {
                reminders.setValue(null);
                return;
            }

            DB_EXECUTOR.execute(() -> {
                for (ReminderWithCar item : list) {
                    ReminderQueries queries = new ReminderQueries(getApplication(), item);
                    item.setDue(queries.isDue());
                    item.setSnoozed(queries.isSnoozed());
                    item.setDistanceToDue(queries.getDistanceToDue());
                    item.setTimeToDue(queries.getTimeToDue());
                }
                reminders.postValue(list);
            });
        });
    }

    public LiveData<List<ReminderWithCar>> getReminders() {
        return reminders;
    }

    public void deleteReminders(long[] ids) {
        DB_EXECUTOR.execute(() -> {
            for (long id : ids) {
                db.getReminderDao().deleteById(id);
            }
        });
    }
}
