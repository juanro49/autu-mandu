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

import org.juanro.autumandu.model.AutuManduDatabase;
import org.juanro.autumandu.model.entity.Car;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class PreferencesGeneralViewModel extends AndroidViewModel {
    private final AutuManduDatabase db;
    private final LiveData<List<Car>> cars;
    private static final Executor DB_EXECUTOR = Executors.newSingleThreadExecutor();

    public PreferencesGeneralViewModel(@NonNull Application application) {
        super(application);
        db = AutuManduDatabase.getInstance(application);
        cars = db.getCarDao().getAllLiveData();
    }

    public LiveData<List<Car>> getCars() {
        return cars;
    }

    public void getCarById(long id, CarCallback callback) {
        DB_EXECUTOR.execute(() -> {
            Car car = db.getCarDao().getById(id);
            callback.onResult(car);
        });
    }

    public interface CarCallback {
        void onResult(Car car);
    }
}
