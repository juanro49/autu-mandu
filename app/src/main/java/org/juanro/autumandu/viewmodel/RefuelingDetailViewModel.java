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

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import org.juanro.autumandu.DistanceEntryMode;
import org.juanro.autumandu.PriceEntryMode;
import org.juanro.autumandu.model.dao.CarDao;
import org.juanro.autumandu.model.dao.FuelTypeDao;
import org.juanro.autumandu.model.dao.RefuelingDao;
import org.juanro.autumandu.model.dao.StationDao;
import org.juanro.autumandu.model.dao.TankDao;
import org.juanro.autumandu.model.dao.TripDao;
import org.juanro.autumandu.model.dto.RefuelingWithDetails;
import org.juanro.autumandu.model.entity.Car;
import org.juanro.autumandu.model.entity.FuelType;
import org.juanro.autumandu.model.entity.Refueling;
import org.juanro.autumandu.model.entity.Station;
import org.juanro.autumandu.model.entity.Tank;
import org.juanro.autumandu.model.entity.Trip;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel for refueling details.
 */
@HiltViewModel
public class RefuelingDetailViewModel extends ViewModel {
    private final CarDao carDao;
    private final FuelTypeDao fuelTypeDao;
    private final RefuelingDao refuelingDao;
    private final StationDao stationDao;
    private final TankDao tankDao;
    private final TripDao tripDao;

    private final MutableLiveData<Long> refuelingId = new MutableLiveData<>();
    private final MutableLiveData<Long> carIdForDefaults = new MutableLiveData<>();
    private final MutableLiveData<Float> learnedCapacity = new MutableLiveData<>();

    private static final Executor DB_EXECUTOR = Executors.newSingleThreadExecutor();

    @Inject
    public RefuelingDetailViewModel(
            CarDao carDao,
            FuelTypeDao fuelTypeDao,
            RefuelingDao refuelingDao,
            StationDao stationDao,
            TankDao tankDao,
            TripDao tripDao
    ) {
        this.carDao = carDao;
        this.fuelTypeDao = fuelTypeDao;
        this.refuelingDao = refuelingDao;
        this.stationDao = stationDao;
        this.tankDao = tankDao;
        this.tripDao = tripDao;
    }

    public void setRefuelingId(long id) {
        refuelingId.setValue(id);
    }

    public void setCarIdForDefaults(long id) {
        carIdForDefaults.setValue(id);
    }

    public LiveData<RefuelingWithDetails> getRefueling() {
        return Transformations.switchMap(refuelingId, id ->
                id == -1 ? new MutableLiveData<>(null) : refuelingDao.getByIdWithDetailsLiveData(id)
        );
    }

    public LiveData<List<Trip>> getLinkedTrips() {
        return Transformations.switchMap(refuelingId, id ->
                id == -1 ? new MutableLiveData<>(new java.util.ArrayList<>()) : tripDao.getTripsForRefuelingLive(id)
        );
    }

    public LiveData<FuelType> getMostUsedFuelType() {
        return Transformations.switchMap(carIdForDefaults, fuelTypeDao::getMostUsedForCarLiveData);
    }

    public LiveData<Station> getMostUsedStation() {
        return Transformations.switchMap(carIdForDefaults, stationDao::getMostUsedForCarLiveData);
    }

    public LiveData<List<FuelType>> getFuelTypes() {
        return fuelTypeDao.getAllLiveData();
    }

    public LiveData<List<Station>> getStations() {
        return stationDao.getAllLiveData();
    }

    public LiveData<List<Car>> getCars() {
        return carDao.getAllLiveData();
    }

    public LiveData<List<Tank>> getTanksForCar(long carId) {
        return tankDao.getTanksForCarLiveData(carId);
    }

    public LiveData<Float> getLearnedCapacity() {
        return learnedCapacity;
    }

    public void getDisplayMileage(RefuelingWithDetails refueling, DistanceEntryMode mode, OnLoadedCallback<Integer> callback) {
        if (mode == DistanceEntryMode.TOTAL) {
            callback.onLoaded(refueling.mileage());
        } else {
            getPreviousRefueling(refueling.tankId(), refueling.date(), previous -> {
                if (previous != null) {
                    callback.onLoaded(refueling.mileage() - previous.getMileage());
                } else {
                    callback.onLoaded(refueling.mileage() - refueling.carInitialMileage());
                }
            });
        }
    }

    public void validateMileage(int mileage, long tankId, Date date, DistanceEntryMode entryMode, OnLoadedCallback<Boolean> callback) {
        getPreviousRefueling(tankId, date, previousRefueling ->
                getNextRefueling(tankId, date, nextRefueling -> {
                    boolean showWarning;
                    if (entryMode == DistanceEntryMode.TOTAL) {
                        showWarning = (previousRefueling != null && previousRefueling.getMileage() > mileage) ||
                                (nextRefueling != null && nextRefueling.getMileage() < mileage);
                    } else {
                        showWarning = previousRefueling != null &&
                                nextRefueling != null &&
                                previousRefueling.getMileage() + mileage > nextRefueling.getMileage();
                    }
                    callback.onLoaded(showWarning);
                }));
    }

    public record SaveParams(
            @Nullable Long currentId,
            int mileageInput,
            Date date,
            boolean partial,
            String note,
            long fuelTypeId,
            long stationId,
            long carId,
            long tankId,
            float volumeInput,
            float priceInput,
            float startLevel,
            float endLevel,
            DistanceEntryMode distanceEntryMode,
            PriceEntryMode priceEntryMode,
            Runnable onSaved
    ) {}

    public void save(SaveParams params) {
        getPreviousRefueling(params.tankId(), params.date(), previousRefueling -> {
            Refueling refueling = new Refueling();
            if (params.currentId() != null && params.currentId() > 0) {
                refueling.setId(params.currentId());
            }

            int mileage = params.mileageInput();
            if (previousRefueling != null && params.distanceEntryMode() == DistanceEntryMode.TRIP) {
                refueling.setMileage(mileage + previousRefueling.getMileage());
            } else {
                refueling.setMileage(mileage);
            }

            refueling.setDate(params.date());
            refueling.setPartial(params.partial());
            refueling.setNote(params.note());
            refueling.setFuelTypeId(params.fuelTypeId());
            refueling.setStationId(params.stationId());
            refueling.setCarId(params.carId());
            refueling.setTankId(params.tankId());
            refueling.setStartLevel(params.startLevel());
            refueling.setEndLevel(params.endLevel());

            switch (params.priceEntryMode()) {
                case TOTAL_AND_VOLUME -> {
                    refueling.setVolume(params.volumeInput());
                    refueling.setPrice(params.priceInput());
                }
                case PER_UNIT_AND_TOTAL -> {
                    refueling.setVolume(params.priceInput() / params.volumeInput());
                    refueling.setPrice(params.priceInput());
                }
                case PER_UNIT_AND_VOLUME -> {
                    refueling.setVolume(params.volumeInput());
                    refueling.setPrice(params.volumeInput() * params.priceInput());
                }
            }

            checkLearning(refueling);
            save(refueling, params.onSaved());
        });
    }

    private void checkLearning(Refueling refueling) {
        if (refueling.getVolume() > 0 && refueling.getEndLevel() > refueling.getStartLevel()) {
            DB_EXECUTOR.execute(() -> {
                Tank tank = tankDao.getById(refueling.getTankId());
                if (tank != null && !tank.isManuallySet()) {
                    float levelDiff = (refueling.getEndLevel() - refueling.getStartLevel()) / 100f;
                    float capacity = refueling.getVolume() / levelDiff;
                    if (Math.abs(capacity - tank.getCapacity()) > 0.1f) {
                        learnedCapacity.postValue(capacity);
                    }
                }
            });
        }
    }

    public void updateTankCapacity(long tankId, float capacity) {
        DB_EXECUTOR.execute(() -> {
            Tank tank = tankDao.getById(tankId);
            if (tank != null) {
                tank.setCapacity(capacity);
                tankDao.update(tank);
            }
        });
    }

    public void save(Refueling refueling, Runnable onSaved) {
        DB_EXECUTOR.execute(() -> {
            if (refueling.getId() != null && refueling.getId() > 0) {
                refuelingDao.update(refueling);
            } else {
                var ids = refuelingDao.insert(refueling);
                refueling.setId(ids[0]);
            }
            if (onSaved != null) {
                onSaved.run();
            }
        });
    }

    public void delete(long id, Runnable onDeleted) {
        DB_EXECUTOR.execute(() -> {
            refuelingDao.deleteById(id);
            if (onDeleted != null) {
                onDeleted.run();
            }
        });
    }

    public void getPreviousRefueling(long tankId, Date date, OnLoadedCallback<Refueling> callback) {
        DB_EXECUTOR.execute(() -> {
            var refueling = refuelingDao.getPrevious(tankId, date);
            callback.onLoaded(refueling);
        });
    }

    public void getNextRefueling(long tankId, Date date, OnLoadedCallback<Refueling> callback) {
        DB_EXECUTOR.execute(() -> {
            var refueling = refuelingDao.getNext(tankId, date);
            callback.onLoaded(refueling);
        });
    }

    public interface OnLoadedCallback<T> {
        void onLoaded(T result);
    }

    public record PriceEntryData(String volume, String price) {}

    public PriceEntryData getPriceEntryData(RefuelingWithDetails refueling, PriceEntryMode mode) {
        float perUnit = (refueling.volume() > 0) ? refueling.price() / refueling.volume() : 0.0f;

        return switch (mode) {
            case TOTAL_AND_VOLUME -> new PriceEntryData(
                    String.valueOf(refueling.volume()),
                    (refueling.price() != 0.0f) ? String.valueOf(refueling.price()) : ""
            );
            case PER_UNIT_AND_TOTAL -> new PriceEntryData(
                    String.valueOf(perUnit),
                    String.valueOf(refueling.price())
            );
            case PER_UNIT_AND_VOLUME -> new PriceEntryData(
                    String.valueOf(refueling.volume()),
                    (refueling.price() != 0.0f) ? String.valueOf(perUnit) : ""
            );
        };
    }
}
