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

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CarDetailViewModel extends AndroidViewModel {
    private final AutuManduDatabase db;
    private final MutableLiveData<Long> carId = new MutableLiveData<>();

    private static final Executor DB_EXECUTOR = Executors.newSingleThreadExecutor();

    public CarDetailViewModel(@NonNull Application application) {
        super(application);
        db = AutuManduDatabase.getInstance(application);
    }

    public void setCarId(long id) {
        carId.setValue(id);
    }

    public LiveData<Car> getCar() {
        return Transformations.switchMap(carId, id -> {
            if (id == -1) {
                return new MutableLiveData<>(null);
            }
            return db.getCarDao().getByIdLiveData(id);
        });
    }

    public LiveData<Integer> getCarCount() {
        return db.getCarDao().getCountLiveData();
    }

    public void save(Car car, Runnable onSaved) {
        DB_EXECUTOR.execute(() -> {
            if (car.getId() != null && car.getId() > 0) {
                db.getCarDao().update(car);
            } else {
                long[] ids = db.getCarDao().insert(car);
                car.setId(ids[0]);
            }
            if (onSaved != null) {
                onSaved.run();
            }
        });
    }

    public void delete(long id, Runnable onDeleted) {
        DB_EXECUTOR.execute(() -> {
            db.getCarDao().deleteById(id);
            if (onDeleted != null) {
                onDeleted.run();
            }
        });
    }
}
