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

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import org.juanro.autumandu.model.dao.CarDao;
import org.juanro.autumandu.model.dao.RefuelingDao;
import org.juanro.autumandu.model.dao.TripDao;
import org.juanro.autumandu.model.dao.TripPrefabDao;
import org.juanro.autumandu.model.entity.Car;
import org.juanro.autumandu.model.entity.Trip;
import org.juanro.autumandu.model.entity.TripPrefab;

import org.juanro.autumandu.model.dto.RefuelingWithDetails;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class TripDetailViewModel extends ViewModel {
    private final CarDao carDao;
    private final TripDao tripDao;
    private final TripPrefabDao tripPrefabDao;
    private final RefuelingDao refuelingDao;

    private final MutableLiveData<Long> tripId = new MutableLiveData<>();
    private final Executor dbExecutor = Executors.newSingleThreadExecutor();

    @Inject
    public TripDetailViewModel(
            CarDao carDao,
            TripDao tripDao,
            TripPrefabDao tripPrefabDao,
            RefuelingDao refuelingDao
    ) {
        this.carDao = carDao;
        this.tripDao = tripDao;
        this.tripPrefabDao = tripPrefabDao;
        this.refuelingDao = refuelingDao;
    }

    public void setTripId(long id) {
        tripId.setValue(id);
    }

    public LiveData<Trip> getTrip() {
        return Transformations.switchMap(tripId, id -> {
            if (id == -1) {
                return new MutableLiveData<>(null);
            }
            return tripDao.getTripByIdLive(id);
        });
    }

    public LiveData<List<Car>> getCars() {
        return carDao.getAllLiveData();
    }

    public LiveData<List<TripPrefab>> getPrefabsByType(long carId, String type) {
        return tripPrefabDao.getPrefabsByTypeLive(carId, type);
    }

    public LiveData<List<RefuelingWithDetails>> getRefuelingsForCar(long carId) {
        return refuelingDao.getWithDetailsForCarLiveData(carId);
    }

    public void save(Trip trip, Runnable onSaved) {
        dbExecutor.execute(() -> {
            if (trip.getId() > 0) {
                tripDao.update(trip);
            } else {
                tripDao.insert(trip);
            }
            updatePrefabs(trip);
            if (onSaved != null) {
                onSaved.run();
            }
        });
    }

    public void delete(long id, Runnable onDeleted) {
        dbExecutor.execute(() -> {
            Trip trip = tripDao.getTripById(id);
            if (trip != null) {
                tripDao.delete(trip);
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

        TripPrefab existing = tripPrefabDao.getPrefabByTypeAndValue(carId, type, value.trim());
        if (existing != null) {
            existing.setUsageCount(existing.getUsageCount() + 1);
            tripPrefabDao.update(existing);
        } else {
            TripPrefab prefab = new TripPrefab();
            prefab.setCarId(carId);
            prefab.setType(type);
            prefab.setValue(value.trim());
            prefab.setUsageCount(1);
            tripPrefabDao.insert(prefab);
        }
    }

    public void getLastKmEnd(long carId, OnLoadedCallback<Integer> callback) {
        dbExecutor.execute(() -> {
            Trip lastTrip = tripDao.getLastTripForCar(carId);
            callback.onLoaded(lastTrip != null ? lastTrip.getKmEnd() : 0);
        });
    }

    public interface OnLoadedCallback<T> {
        void onLoaded(T result);
    }
}
