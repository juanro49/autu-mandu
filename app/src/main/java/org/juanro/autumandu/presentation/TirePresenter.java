/*
 * Copyright 2025 Juanro49
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
package org.juanro.autumandu.presentation;

import android.content.Context;

import org.juanro.autumandu.model.AutuManduDatabase;
import org.juanro.autumandu.model.entity.TireList;

import java.util.HashMap;
import java.util.Map;

public class TirePresenter {

    private Context mContext;
    private AutuManduDatabase mDB;

    private TirePresenter(Context context) {
        mContext = context;
        mDB = AutuManduDatabase.getInstance(mContext);
    }

    public static TirePresenter getInstance(Context context) {
        return new TirePresenter(context);
    }

    public Map<Long, Integer> getTiresAndDistances(long carId) {
        Map<Long, Integer> tires = new HashMap<>();
        for (TireList t: mDB.getTireDao().getAllForCar(carId)) {
            int mounted = mDB.getTireDao().getIsTireMounted(t.getId());
            int distance = mDB.getTireDao().getTireDistance(t.getId());

            if (mounted == 1)
            {
                int distanceLastMount = mDB.getTireDao().getTireLastMountDistance(t.getId());
                int carDistance = CarPresenter.getInstance(mContext).getLatestMileage(carId);

                tires.put(t.getId(), distance + (carDistance - distanceLastMount));
            }
            else
            {
                tires.put(t.getId(), distance);
            }
        }

        return tires;
    }

    public int getTireDistance(long tireId) {
        int mounted = mDB.getTireDao().getIsTireMounted(tireId);
        int distance = mDB.getTireDao().getTireDistance(tireId);
        long carId = mDB.getTireDao().getCarOfTire(tireId);

        if (mounted == 1)
        {
            int distanceLastMount = mDB.getTireDao().getTireLastMountDistance(tireId);
            int carDistance = CarPresenter.getInstance(mContext).getLatestMileage(carId);

            distance = distance + (carDistance - distanceLastMount);
        }

        return distance;
    }

    public int getState(long tireId) {
        int isMounted = mDB.getTireDao().getIsTireMounted(tireId);
        int isTrashed = mDB.getTireDao().getIsTireTrashed(tireId);

        if (isMounted == 1)
        {
            return 1;
        } else if (isTrashed == 1)
        {
            return 2;
        }

        return 0;
    }
}
