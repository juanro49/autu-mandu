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
import androidx.room.InvalidationTracker;
import org.juanro.autumandu.data.calculation.AbstractCalculation;
import org.juanro.autumandu.data.calculation.CalculationItem;
import org.juanro.autumandu.data.calculation.DistancePriceCalculation;
import org.juanro.autumandu.data.calculation.DistanceVolumeCalculation;
import org.juanro.autumandu.data.calculation.PriceVolumeCalculation;
import org.juanro.autumandu.model.AutuManduDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CalculatorViewModel extends AndroidViewModel {
    private final AbstractCalculation[] mCalculations;
    private final MutableLiveData<CalculationItem[]> mResults = new MutableLiveData<>();
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final InvalidationTracker.Observer mRoomObserver;

    public CalculatorViewModel(@NonNull Application application) {
        super(application);
        mCalculations = new AbstractCalculation[]{
                new DistanceVolumeCalculation(application, DistanceVolumeCalculation.Direction.VOLUME_TO_DISTANCE),
                new DistanceVolumeCalculation(application, DistanceVolumeCalculation.Direction.DISTANCE_TO_VOLUME),
                new PriceVolumeCalculation(application, PriceVolumeCalculation.Direction.VOLUME_TO_PRICE),
                new PriceVolumeCalculation(application, PriceVolumeCalculation.Direction.PRICE_TO_VOLUME),
                new DistancePriceCalculation(application, DistancePriceCalculation.Direction.DISTANCE_TO_PRICE),
                new DistancePriceCalculation(application, DistancePriceCalculation.Direction.PRICE_TO_DISTANCE),
                new DistancePriceCalculation(application, DistancePriceCalculation.Direction.DISTANCE_TO_FUEL_PRICE),
                new DistancePriceCalculation(application, DistancePriceCalculation.Direction.FUEL_PRICE_TO_DISTANCE)};

        mRoomObserver = new InvalidationTracker.Observer(new String[]{"refueling", "other_cost", "car"}) {
            @Override
            public void onInvalidated(@NonNull java.util.Set<String> tables) {
                for (AbstractCalculation calculation : mCalculations) {
                    calculation.notifyDataChanged();
                }
            }
        };
        AutuManduDatabase.getInstance(application).getInvalidationTracker().addObserver(mRoomObserver);
    }

    public AbstractCalculation[] getCalculations() {
        return mCalculations;
    }

    public LiveData<CalculationItem[]> getResults() {
        return mResults;
    }

    public void calculate(AbstractCalculation calculation, double input) {
        mExecutor.execute(() -> {
            try {
                mResults.postValue(calculation.calculate(input));
            } catch (Exception e) {
                mResults.postValue(null);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        AutuManduDatabase.getInstance(getApplication()).getInvalidationTracker().removeObserver(mRoomObserver);
        mExecutor.shutdown();
    }
}
