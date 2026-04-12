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
import org.juanro.autumandu.model.dto.RefuelingWithDetails;
import org.juanro.autumandu.model.entity.Car;
import org.juanro.autumandu.model.entity.FuelType;
import org.juanro.autumandu.model.entity.Refueling;
import org.juanro.autumandu.model.entity.Station;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * ViewModel for refueling details.
 */
public class RefuelingDetailViewModel extends AndroidViewModel {
    private final AutuManduDatabase db;
    private final MutableLiveData<Long> refuelingId = new MutableLiveData<>();
    private final MutableLiveData<Long> carIdForDefaults = new MutableLiveData<>();

    private static final Executor DB_EXECUTOR = Executors.newSingleThreadExecutor();

    public RefuelingDetailViewModel(@NonNull Application application) {
        super(application);
        db = AutuManduDatabase.getInstance(application);
    }

    public void setRefuelingId(long id) {
        refuelingId.setValue(id);
    }

    public void setCarIdForDefaults(long id) {
        carIdForDefaults.setValue(id);
    }

    public LiveData<RefuelingWithDetails> getRefueling() {
        return Transformations.switchMap(refuelingId, id ->
                id == -1 ? new MutableLiveData<>(null) : db.getRefuelingDao().getByIdWithDetailsLiveData(id)
        );
    }

    public LiveData<FuelType> getMostUsedFuelType() {
        return Transformations.switchMap(carIdForDefaults, id -> db.getFuelTypeDao().getMostUsedForCarLiveData(id));
    }

    public LiveData<Station> getMostUsedStation() {
        return Transformations.switchMap(carIdForDefaults, id -> db.getStationDao().getMostUsedForCarLiveData(id));
    }

    public LiveData<List<FuelType>> getFuelTypes() {
        return db.getFuelTypeDao().getAllLiveData();
    }

    public LiveData<List<Station>> getStations() {
        return db.getStationDao().getAllLiveData();
    }

    public LiveData<List<Car>> getCars() {
        return db.getCarDao().getAllLiveData();
    }

    public void save(Refueling refueling, Runnable onSaved) {
        DB_EXECUTOR.execute(() -> {
            if (refueling.getId() != null && refueling.getId() > 0) {
                db.getRefuelingDao().update(refueling);
            } else {
                var ids = db.getRefuelingDao().insert(refueling);
                refueling.setId(ids[0]);
            }
            if (onSaved != null) {
                onSaved.run();
            }
        });
    }

    public void delete(long id, Runnable onDeleted) {
        DB_EXECUTOR.execute(() -> {
            db.getRefuelingDao().deleteById(id);
            if (onDeleted != null) {
                onDeleted.run();
            }
        });
    }

    public void getPreviousRefueling(long carId, Date date, OnLoadedCallback<Refueling> callback) {
        DB_EXECUTOR.execute(() -> {
            var refueling = db.getRefuelingDao().getPrevious(carId, date);
            callback.onLoaded(refueling);
        });
    }

    public void getNextRefueling(long carId, Date date, OnLoadedCallback<Refueling> callback) {
        DB_EXECUTOR.execute(() -> {
            var refueling = db.getRefuelingDao().getNext(carId, date);
            callback.onLoaded(refueling);
        });
    }

    public interface OnLoadedCallback<T> {
        void onLoaded(T result);
    }
}
