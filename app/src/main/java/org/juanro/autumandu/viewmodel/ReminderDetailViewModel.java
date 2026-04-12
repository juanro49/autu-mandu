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
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import org.juanro.autumandu.model.AutuManduDatabase;
import org.juanro.autumandu.model.entity.Car;
import org.juanro.autumandu.model.entity.OtherCost;
import org.juanro.autumandu.model.entity.Refueling;
import org.juanro.autumandu.model.entity.Reminder;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ReminderDetailViewModel extends AndroidViewModel {
    private final AutuManduDatabase db;
    private final MutableLiveData<Long> reminderId = new MutableLiveData<>();
    private final MutableLiveData<Long> selectedCarId = new MutableLiveData<>();

    private static final Executor DB_EXECUTOR = Executors.newSingleThreadExecutor();

    public ReminderDetailViewModel(@NonNull Application application) {
        super(application);
        db = AutuManduDatabase.getInstance(application);
    }

    public void setReminderId(long id) {
        reminderId.setValue(id);
    }

    public void setSelectedCarId(long id) {
        selectedCarId.setValue(id);
    }

    public LiveData<Reminder> getReminder() {
        return Transformations.switchMap(reminderId, id -> {
            if (id == -1) {
                return new MutableLiveData<>(null);
            }
            return db.getReminderDao().getByIdLiveData(id);
        });
    }

    public LiveData<List<Car>> getActiveCars() {
        return db.getCarDao().getNotSuspendedLiveData();
    }

    public LiveData<Car> getSelectedCar() {
        return Transformations.switchMap(selectedCarId, id -> {
            if (id == -1) {
                return new MutableLiveData<>(null);
            }
            return db.getCarDao().getByIdLiveData(id);
        });
    }

    public LiveData<Refueling> getLastRefuelingForSelectedCar() {
        return Transformations.switchMap(selectedCarId, id -> {
            if (id == -1) {
                return new MutableLiveData<>(null);
            }
            return db.getRefuelingDao().getLastForCarLiveData(id);
        });
    }

    public LiveData<OtherCost> getLastOtherCostForSelectedCar() {
        return Transformations.switchMap(selectedCarId, id -> {
            if (id == -1) {
                return new MutableLiveData<>(null);
            }
            return db.getOtherCostDao().getLastForCarLiveData(id);
        });
    }

    public LiveData<Integer> getLatestMileageForSelectedCar() {
        return Transformations.switchMap(selectedCarId, id -> {
            if (id == -1) {
                return new MutableLiveData<>(0);
            }
            return db.getCarDao().getLatestMileageLiveData(id);
        });
    }

    public void save(Reminder reminder, Runnable onSaved) {
        DB_EXECUTOR.execute(() -> {
            if (reminder.getId() != null && reminder.getId() > 0) {
                db.getReminderDao().update(reminder);
            } else {
                long[] ids = db.getReminderDao().insert(reminder);
                reminder.setId(ids[0]);
            }
            if (onSaved != null) {
                onSaved.run();
            }
        });
    }

    public void delete(long id, Runnable onDeleted) {
        DB_EXECUTOR.execute(() -> {
            db.getReminderDao().deleteById(id);
            if (onDeleted != null) {
                onDeleted.run();
            }
        });
    }
}
