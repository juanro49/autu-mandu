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
import org.juanro.autumandu.model.entity.OtherCost;

import java.util.List;

public class OtherListViewModel extends AndroidViewModel {
    private final AutuManduDatabase db;
    private final MutableLiveData<Long> carId = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isExpenditure = new MutableLiveData<>(true);

    public OtherListViewModel(@NonNull Application application) {
        super(application);
        db = AutuManduDatabase.getInstance(application);
    }

    public void setCarId(long id) {
        if (carId.getValue() == null || carId.getValue() != id) {
            carId.setValue(id);
        }
    }

    public void setExpenditure(boolean expenditure) {
        isExpenditure.setValue(expenditure);
    }

    public LiveData<List<OtherCost>> getOtherCosts() {
        return Transformations.switchMap(carId, id -> {
            if (id == null || id == -1) {
                return new MutableLiveData<>(null);
            }
            return Transformations.switchMap(isExpenditure, expenditure -> {
                if (Boolean.TRUE.equals(expenditure)) {
                    return db.getOtherCostDao().getExpendituresForCarDescending(id);
                } else {
                    return db.getOtherCostDao().getIncomesForCarDescending(id);
                }
            });
        });
    }

    public void delete(long id) {
        AutuManduDatabase.DB_EXECUTOR.execute(() -> db.getOtherCostDao().deleteById(id));
    }
}
