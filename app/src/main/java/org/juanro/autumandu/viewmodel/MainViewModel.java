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
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;

import org.juanro.autumandu.model.dao.CarDao;
import org.juanro.autumandu.model.entity.Car;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class MainViewModel extends ViewModel {
    private final CarDao carDao;
    private final MediatorLiveData<List<Car>> cars = new MediatorLiveData<>();
    private final MediatorLiveData<List<Car>> notSuspendedCars = new MediatorLiveData<>();
    private final MediatorLiveData<Integer> carCount = new MediatorLiveData<>();
    private LiveData<List<Car>> currentCarsSource;
    private LiveData<List<Car>> currentNotSuspendedCarsSource;
    private LiveData<Integer> currentCarCountSource;

    @Inject
    public MainViewModel(CarDao carDao) {
        this.carDao = carDao;
        refreshSources();
    }

    public void refreshSources() {
        if (currentCarsSource != null) cars.removeSource(currentCarsSource);
        if (currentNotSuspendedCarsSource != null) notSuspendedCars.removeSource(currentNotSuspendedCarsSource);
        if (currentCarCountSource != null) carCount.removeSource(currentCarCountSource);

        currentCarsSource = carDao.getAllLiveData();
        currentNotSuspendedCarsSource = carDao.getNotSuspendedLiveData();
        currentCarCountSource = carDao.getCountLiveData();

        cars.addSource(currentCarsSource, cars::setValue);
        notSuspendedCars.addSource(currentNotSuspendedCarsSource, notSuspendedCars::setValue);
        carCount.addSource(currentCarCountSource, carCount::setValue);
    }

    public LiveData<List<Car>> getCars() {
        return cars;
    }

    public LiveData<List<Car>> getNotSuspendedCars() {
        return notSuspendedCars;
    }

    public LiveData<Integer> getCarCount() {
        return carCount;
    }
}
