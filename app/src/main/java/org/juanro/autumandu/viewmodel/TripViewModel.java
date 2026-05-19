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
import androidx.lifecycle.ViewModelProvider;

import org.juanro.autumandu.model.AutuManduDatabase;
import org.juanro.autumandu.model.dto.TripWithDetails;
import org.juanro.autumandu.model.entity.Car;
import org.juanro.autumandu.model.entity.Trip;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TripViewModel extends AndroidViewModel {
    private final AutuManduDatabase db;
    private final MutableLiveData<Long> carId = new MutableLiveData<>();

    private static final Executor DB_EXECUTOR = Executors.newSingleThreadExecutor();

    public TripViewModel(@NonNull Application application) {
        this(application, AutuManduDatabase.getInstance(application));
    }

    public TripViewModel(@NonNull Application application, @NonNull AutuManduDatabase database) {
        super(application);
        this.db = database;
    }

    public record Factory(@NonNull Application application) implements ViewModelProvider.Factory {
        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends androidx.lifecycle.ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(TripViewModel.class)) {
                return (T) new TripViewModel(application);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }

    public void setCarId(long id) {
        carId.setValue(id);
    }

    public LiveData<List<Car>> getCars() {
        return db.getCarDao().getAllLiveData();
    }

    public LiveData<List<TripWithDetails>> getTrips() {
        return Transformations.switchMap(carId, id -> {
            if (id == null || id == -1) {
                return new MutableLiveData<>(new ArrayList<>());
            }
            return db.getTripDao().getTripsWithDetailsForCarLive(id);
        });
    }

    public List<Trip> getTripsForCar(long carId) {
        return db.getTripDao().getTripsForCar(carId);
    }

    public void deleteTrip(Trip trip, Runnable onDeleted) {
        DB_EXECUTOR.execute(() -> {
            db.getTripDao().delete(trip);
            if (onDeleted != null) {
                onDeleted.run();
            }
        });
    }

    public List<String> validateTrip(Trip trip) {
        List<String> errors = new ArrayList<>();

        if (trip.getRouteTarget().isEmpty())
            errors.add("Route/Target is required");
        if (trip.getPurpose().isEmpty())
            errors.add("Purpose is required");

        if (trip.getKmStart() < 0) errors.add("Start km must be positive");
        if (trip.getKmEnd() < trip.getKmStart())
            errors.add("End km must be greater than start km");

        LocalDateTime start = LocalDateTime.of(trip.getDate(), trip.getTimeStart());
        LocalDateTime end = LocalDateTime.of(trip.getDateEnd(), trip.getTimeEnd());
        if (end.isBefore(start)) {
            errors.add("End time must be after start time");
        }

        return errors;
    }
}
