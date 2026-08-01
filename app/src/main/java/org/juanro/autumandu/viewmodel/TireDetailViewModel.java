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
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import org.juanro.autumandu.model.AutuManduDatabase;
import org.juanro.autumandu.model.entity.Car;
import org.juanro.autumandu.model.entity.Refueling;
import org.juanro.autumandu.model.entity.TireList;

import java.util.List;

import org.juanro.autumandu.model.entity.TireUsage;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TireDetailViewModel extends AndroidViewModel {
    private final AutuManduDatabase db;
    private final MutableLiveData<Long> tireId = new MutableLiveData<>();
    private final MutableLiveData<Long> carId = new MutableLiveData<>();

    private static final Executor DB_EXECUTOR = Executors.newSingleThreadExecutor();

    public TireDetailViewModel(@NonNull Application application) {
        super(application);
        db = AutuManduDatabase.getInstance(application);
    }

    public void setTireId(long id) {
        tireId.setValue(id);
    }

    public void setCarId(long id) {
        carId.setValue(id);
    }

    public LiveData<TireList> getTire() {
        return Transformations.switchMap(tireId, id -> {
            if (id == -1) {
                return new MutableLiveData<>(null);
            }
            return db.getTireDao().getTireListByIdLiveData(id);
        });
    }

    public LiveData<Boolean> isTireMounted() {
        return Transformations.switchMap(tireId, id -> {
            if (id == -1) {
                return new MutableLiveData<>(false);
            }
            return db.getTireDao().isTireMountedLiveData(id);
        });
    }

    public LiveData<Integer> getNumTiresMounted() {
        return Transformations.switchMap(carId, id -> db.getTireDao().getNumTiresMountedLiveData(id));
    }

    public LiveData<Integer> getCarNumTires() {
        return Transformations.switchMap(carId, id -> db.getCarDao().getCarNumTiresLiveData(id));
    }

    public LiveData<Integer> getLatestMileage() {
        return Transformations.switchMap(carId, id -> db.getCarDao().getLatestMileageLiveData(id));
    }

    public LiveData<Refueling> getLastRefueling() {
        return Transformations.switchMap(carId, id -> db.getRefuelingDao().getLastForCarLiveData(id));
    }

    public LiveData<List<Car>> getCars() {
        return db.getCarDao().getAllLiveData();
    }

    public void save(TireList tireList, TireUsage newUsage, TireUsage updateUsage, Runnable onSaved) {
        DB_EXECUTOR.execute(() -> {
            db.runInTransaction(() -> {
                if (tireList.getId() != null && tireList.getId() > 0) {
                    db.getTireDao().update(tireList);
                } else {
                    long[] ids = db.getTireDao().insert(tireList);
                    tireList.setId(ids[0]);
                }

                if (newUsage != null) {
                    newUsage.setTireId(tireList.getId());
                    db.getTireDao().insert(newUsage);
                }

                if (updateUsage != null) {
                    db.getTireDao().update(updateUsage);
                }
            });

            if (onSaved != null) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(onSaved);
            }
        });
    }

    public void split(long tireId, int quantityToMove, Runnable onFinished) {
        DB_EXECUTOR.execute(() -> {
            db.runInTransaction(() -> {
                TireList originalTire = db.getTireDao().getTireListById(tireId);
                if (originalTire == null || originalTire.getQuantity() <= quantityToMove) {
                    return;
                }

                // Update original tire quantity
                originalTire.setQuantity(originalTire.getQuantity() - quantityToMove);
                db.getTireDao().update(originalTire);

                // Create new tire entry
                TireList newTire = getTireList(quantityToMove, originalTire);

                long[] newIds = db.getTireDao().insert(newTire);
                long newTireId = newIds[0];

                // Duplicate usages
                List<TireUsage> usages = db.getTireDao().getUsagesForTire(tireId);
                for (TireUsage usage : usages) {
                    TireUsage newUsage = new TireUsage();
                    newUsage.setTireId(newTireId);
                    newUsage.setDistanceMount(usage.getDistanceMount());
                    newUsage.setDateMount(usage.getDateMount());
                    newUsage.setDistanceUmount(usage.getDistanceUmount());
                    newUsage.setDateUmount(usage.getDateUmount());
                    db.getTireDao().insert(newUsage);
                }
            });

            if (onFinished != null) {
                new Handler(Looper.getMainLooper()).post(onFinished);
            }
        });
    }

    @NonNull
    private static TireList getTireList(int quantityToMove, TireList originalTire) {
        TireList newTire = new TireList();
        newTire.setBuyDate(originalTire.getBuyDate());
        newTire.setTrashDate(originalTire.getTrashDate());
        newTire.setPrice(originalTire.getPrice());
        newTire.setQuantity(quantityToMove);
        newTire.setManufacturer(originalTire.getManufacturer());
        newTire.setModel(originalTire.getModel());
        newTire.setNote(originalTire.getNote());
        newTire.setCarId(originalTire.getCarId());
        return newTire;
    }

    public void delete(long id, Runnable onDeleted) {
        DB_EXECUTOR.execute(() -> {
            db.getTireDao().deleteTireListById(id);
            if (onDeleted != null) {
                onDeleted.run();
            }
        });
    }

    public void getUsageByTireIdNotUmount(long tireId, OnLoadedCallback<TireUsage> callback) {
        DB_EXECUTOR.execute(() -> {
            TireUsage usage = db.getTireDao().getUsageByTireIdNotUmount(tireId);
            callback.onLoaded(usage);
        });
    }

    public interface OnLoadedCallback<T> {
        void onLoaded(T result);
    }
}
