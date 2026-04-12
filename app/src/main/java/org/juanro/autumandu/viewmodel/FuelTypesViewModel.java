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
import org.juanro.autumandu.model.entity.FuelType;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class FuelTypesViewModel extends AndroidViewModel {
    private final AutuManduDatabase db;
    private final LiveData<List<FuelType>> fuelTypes;
    private static final Executor DB_EXECUTOR = Executors.newSingleThreadExecutor();

    public FuelTypesViewModel(@NonNull Application application) {
        super(application);
        db = AutuManduDatabase.getInstance(application);
        fuelTypes = db.getFuelTypeDao().getAllLiveData();
    }

    public LiveData<List<FuelType>> getFuelTypes() {
        return fuelTypes;
    }

    public void getFuelType(long id, FuelTypeCallback callback) {
        DB_EXECUTOR.execute(() -> {
            FuelType fuelType = db.getFuelTypeDao().getById(id);
            callback.onResult(fuelType);
        });
    }

    public void saveFuelType(FuelType fuelType) {
        DB_EXECUTOR.execute(() -> {
            if (fuelType.getId() != null && fuelType.getId() > 0) {
                db.getFuelTypeDao().update(fuelType);
            } else {
                db.getFuelTypeDao().insert(fuelType);
            }
        });
    }

    public void checkUsage(long[] ids, UsageCheckCallback callback) {
        DB_EXECUTOR.execute(() -> {
            boolean isUsed = false;
            for (long id : ids) {
                if (db.getFuelTypeDao().getUsageCount(id) > 0) {
                    isUsed = true;
                    break;
                }
            }
            callback.onResult(isUsed);
        });
    }

    public void deleteFuelTypes(long[] ids) {
        DB_EXECUTOR.execute(() -> {
            for (long id : ids) {
                db.getFuelTypeDao().deleteById(id);
            }
        });
    }

    public interface UsageCheckCallback {
        void onResult(boolean isUsed);
    }

    public interface FuelTypeCallback {
        void onResult(FuelType fuelType);
    }
}
