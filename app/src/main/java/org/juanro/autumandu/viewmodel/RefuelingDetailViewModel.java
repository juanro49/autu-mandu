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
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import org.juanro.autumandu.DistanceEntryMode;
import org.juanro.autumandu.PriceEntryMode;
import org.juanro.autumandu.model.AutuManduDatabase;
import org.juanro.autumandu.model.dto.RefuelingWithDetails;
import org.juanro.autumandu.model.entity.Car;
import org.juanro.autumandu.model.entity.FuelType;
import org.juanro.autumandu.model.entity.Refueling;
import org.juanro.autumandu.model.entity.Station;

import org.juanro.autumandu.model.entity.Trip;

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

    public LiveData<List<Trip>> getLinkedTrips() {
        return Transformations.switchMap(refuelingId, id ->
                id == -1 ? new MutableLiveData<>(new java.util.ArrayList<>()) : db.getTripDao().getTripsForRefuelingLive(id)
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

    public void getDisplayMileage(RefuelingWithDetails refueling, DistanceEntryMode mode, OnLoadedCallback<Integer> callback) {
        if (mode == DistanceEntryMode.TOTAL) {
            callback.onLoaded(refueling.mileage());
        } else {
            getPreviousRefueling(refueling.carId(), refueling.date(), previous -> {
                if (previous != null) {
                    callback.onLoaded(refueling.mileage() - previous.getMileage());
                } else {
                    callback.onLoaded(refueling.mileage() - refueling.carInitialMileage());
                }
            });
        }
    }

    public void validateMileage(int mileage, long carId, Date date, DistanceEntryMode entryMode, OnLoadedCallback<Boolean> callback) {
        getPreviousRefueling(carId, date, previousRefueling ->
                getNextRefueling(carId, date, nextRefueling -> {
                    boolean showWarning;
                    if (entryMode == DistanceEntryMode.TOTAL) {
                        showWarning = (previousRefueling != null && previousRefueling.getMileage() >= mileage) ||
                                (nextRefueling != null && nextRefueling.getMileage() <= mileage);
                    } else {
                        showWarning = previousRefueling != null &&
                                nextRefueling != null &&
                                previousRefueling.getMileage() + mileage >= nextRefueling.getMileage();
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
            float volumeInput,
            float priceInput,
            DistanceEntryMode distanceEntryMode,
            PriceEntryMode priceEntryMode,
            Runnable onSaved
    ) {}

    public void save(SaveParams params) {
        getPreviousRefueling(params.carId(), params.date(), previousRefueling -> {
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

            if (params.priceEntryMode() == PriceEntryMode.TOTAL_AND_VOLUME) {
                refueling.setVolume(params.volumeInput());
                refueling.setPrice(params.priceInput());
            } else if (params.priceEntryMode() == PriceEntryMode.PER_UNIT_AND_TOTAL) {
                refueling.setVolume(params.priceInput() / params.volumeInput());
                refueling.setPrice(params.priceInput());
            } else if (params.priceEntryMode() == PriceEntryMode.PER_UNIT_AND_VOLUME) {
                refueling.setVolume(params.volumeInput());
                refueling.setPrice(params.volumeInput() * params.priceInput());
            }

            save(refueling, params.onSaved());
        });
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

    public record PriceEntryData(String volume, String price) {}

    public PriceEntryData getPriceEntryData(RefuelingWithDetails refueling, PriceEntryMode mode) {
        float perUnit = (refueling.volume() > 0) ? refueling.price() / refueling.volume() : 0.0f;

        final String volumeStr;
        final String priceStr;

        if (mode == PriceEntryMode.TOTAL_AND_VOLUME) {
            volumeStr = String.valueOf(refueling.volume());
            priceStr = (refueling.price() != 0.0f) ? String.valueOf(refueling.price()) : "";
        } else if (mode == PriceEntryMode.PER_UNIT_AND_TOTAL) {
            volumeStr = String.valueOf(perUnit);
            priceStr = String.valueOf(refueling.price());
        } else if (mode == PriceEntryMode.PER_UNIT_AND_VOLUME) {
            volumeStr = String.valueOf(refueling.volume());
            priceStr = (refueling.price() != 0.0f) ? String.valueOf(perUnit) : "";
        } else {
            volumeStr = "";
            priceStr = "";
        }

        return new PriceEntryData(volumeStr, priceStr);
    }
}
