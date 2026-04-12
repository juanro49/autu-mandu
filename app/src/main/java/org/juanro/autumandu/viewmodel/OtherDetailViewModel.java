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

import java.util.List;

import org.juanro.autumandu.model.AutuManduDatabase;
import org.juanro.autumandu.model.entity.Car;
import org.juanro.autumandu.model.entity.OtherCost;

/**
 * Modernized ViewModel for OtherCost details using Java 21 and Room.
 */
public class OtherDetailViewModel extends AndroidViewModel {
    private final AutuManduDatabase db;
    private final MutableLiveData<Long> id = new MutableLiveData<>();
    private final MutableLiveData<Integer> type = new MutableLiveData<>();

    public OtherDetailViewModel(@NonNull Application application) {
        super(application);
        this.db = AutuManduDatabase.getInstance(application);
    }

    public void setId(long id) {
        this.id.setValue(id);
    }

    public void setType(int type) {
        this.type.setValue(type);
    }

    public LiveData<OtherCost> getOtherCost() {
        return Transformations.switchMap(id, otherCostId ->
                otherCostId == -1 ? new MutableLiveData<>(null) : db.getOtherCostDao().getByIdLiveData(otherCostId));
    }

    public LiveData<List<String>> getTitles() {
        return Transformations.switchMap(type, t -> {
            if (t == 1) { // Income
                return db.getOtherCostDao().getPositiveCostTitlesLiveData();
            } else { // Expenditure
                return db.getOtherCostDao().getNegativeCostTitlesLiveData();
            }
        });
    }

    public LiveData<List<Car>> getCars() {
        return db.getCarDao().getNotSuspendedLiveData();
    }

    public void delete(OtherCost otherCost, Runnable onDeleted) {
        AutuManduDatabase.DB_EXECUTOR.execute(() -> {
            db.getOtherCostDao().delete(otherCost);
            if (onDeleted != null) {
                onDeleted.run();
            }
        });
    }

    public void save(OtherCost otherCost, Runnable onSaved) {
        AutuManduDatabase.DB_EXECUTOR.execute(() -> {
            if (otherCost.getId() != null && otherCost.getId() > 0) {
                db.getOtherCostDao().update(otherCost);
            } else {
                long[] ids = db.getOtherCostDao().insert(otherCost);
                otherCost.setId(ids[0]);
            }
            if (onSaved != null) {
                onSaved.run();
            }
        });
    }
}
