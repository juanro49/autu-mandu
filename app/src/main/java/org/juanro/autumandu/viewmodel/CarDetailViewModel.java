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
import org.juanro.autumandu.model.dao.TankDao;
import org.juanro.autumandu.model.entity.Car;
import org.juanro.autumandu.model.entity.Tank;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class CarDetailViewModel extends ViewModel {
    private final CarDao carDao;
    private final TankDao tankDao;
    private final MutableLiveData<Long> carId = new MutableLiveData<>();

    private static final Executor DB_EXECUTOR = Executors.newSingleThreadExecutor();

    @Inject
    public CarDetailViewModel(CarDao carDao, TankDao tankDao) {
        this.carDao = carDao;
        this.tankDao = tankDao;
    }

    public void setCarId(long id) {
        carId.setValue(id);
    }

    public LiveData<Car> getCar() {
        return Transformations.switchMap(carId, id -> {
            if (id == -1) {
                return new MutableLiveData<>(null);
            }
            return carDao.getByIdLiveData(id);
        });
    }

    public LiveData<List<Tank>> getTanks() {
        return Transformations.switchMap(carId, id -> {
            if (id == -1) {
                return new MutableLiveData<>(new ArrayList<>());
            }
            return tankDao.getTanksForCarLiveData(id);
        });
    }

    public LiveData<Integer> getCarCount() {
        return carDao.getCountLiveData();
    }

    public void save(Car car, List<Tank> tanks, List<Tank> deletedTanks, Runnable onSaved) {
        DB_EXECUTOR.execute(() -> {
            if (car.getId() != null && car.getId() > 0) {
                carDao.update(car);
            } else {
                long[] ids = carDao.insert(car);
                car.setId(ids[0]);
            }

            if (car.getId() != null) {
                for (Tank tank : tanks) {
                    tank.setCarId(car.getId());
                    if (tank.getId() != null && tank.getId() > 0) {
                        tankDao.update(tank);
                    } else {
                        tankDao.insert(tank);
                    }
                }
                for (Tank tank : deletedTanks) {
                    if (tank.getId() != null && tank.getId() > 0) {
                        tankDao.delete(tank);
                    }
                }
            }

            if (onSaved != null) {
                onSaved.run();
            }
        });
    }

    public void delete(long id, Runnable onDeleted) {
        DB_EXECUTOR.execute(() -> {
            carDao.deleteById(id);
            if (onDeleted != null) {
                onDeleted.run();
            }
        });
    }
}
