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
import org.juanro.autumandu.model.entity.Trip;
import org.juanro.autumandu.model.entity.TripPrefab;

import org.juanro.autumandu.model.dto.RefuelingWithDetails;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TripDetailViewModel extends AndroidViewModel {
    private final AutuManduDatabase db;
    private final MutableLiveData<Long> tripId = new MutableLiveData<>();
    private final Executor dbExecutor = Executors.newSingleThreadExecutor();

    public TripDetailViewModel(@NonNull Application application) {
        super(application);
        this.db = AutuManduDatabase.getInstance(application);
    }

    public void setTripId(long id) {
        tripId.setValue(id);
    }

    public LiveData<Trip> getTrip() {
        return Transformations.switchMap(tripId, id -> {
            if (id == -1) {
                return new MutableLiveData<>(null);
            }
            return db.getTripDao().getTripByIdLive(id);
        });
    }

    public LiveData<List<Car>> getCars() {
        return db.getCarDao().getAllLiveData();
    }

    public LiveData<List<TripPrefab>> getPrefabsByType(long carId, String type) {
        return db.getTripPrefabDao().getPrefabsByTypeLive(carId, type);
    }

    public LiveData<List<RefuelingWithDetails>> getRefuelingsForCar(long carId) {
        return db.getRefuelingDao().getWithDetailsForCarLiveData(carId);
    }

    public void save(Trip trip, Runnable onSaved) {
        dbExecutor.execute(() -> {
            if (trip.getId() > 0) {
                db.getTripDao().update(trip);
            } else {
                db.getTripDao().insert(trip);
            }
            updatePrefabs(trip);
            if (onSaved != null) {
                onSaved.run();
            }
        });
    }

    public void delete(long id, Runnable onDeleted) {
        dbExecutor.execute(() -> {
            Trip trip = db.getTripDao().getTripById(id);
            if (trip != null) {
                db.getTripDao().delete(trip);
            }
            if (onDeleted != null) {
                onDeleted.run();
            }
        });
    }

    private void updatePrefabs(Trip trip) {
        updateOrCreatePrefab(trip.getCarId(), "route", trip.getRouteTarget());
        updateOrCreatePrefab(trip.getCarId(), "purpose", trip.getPurpose());
        updateOrCreatePrefab(trip.getCarId(), "driver", trip.getDriver());
    }

    private void updateOrCreatePrefab(long carId, String type, String value) {
        if (value == null || value.trim().isEmpty()) return;

        TripPrefab existing = db.getTripPrefabDao().getPrefabByTypeAndValue(carId, type, value.trim());
        if (existing != null) {
            existing.setUsageCount(existing.getUsageCount() + 1);
            db.getTripPrefabDao().update(existing);
        } else {
            TripPrefab prefab = new TripPrefab();
            prefab.setCarId(carId);
            prefab.setType(type);
            prefab.setValue(value.trim());
            prefab.setUsageCount(1);
            db.getTripPrefabDao().insert(prefab);
        }
    }

    public void getLastKmEnd(long carId, OnLoadedCallback<Integer> callback) {
        dbExecutor.execute(() -> {
            Trip lastTrip = db.getTripDao().getLastTripForCar(carId);
            callback.onLoaded(lastTrip != null ? lastTrip.getKmEnd() : 0);
        });
    }

    public interface OnLoadedCallback<T> {
        void onLoaded(T result);
    }
}
