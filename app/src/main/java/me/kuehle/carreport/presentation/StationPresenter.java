/*
 * Copyright 2015 Jan Kühle
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.kuehle.carreport.presentation;

import android.content.Context;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import me.kuehle.carreport.model.CarReportDatabase;
import me.kuehle.carreport.model.entity.Station;
import me.kuehle.carreport.provider.station.StationColumns;
import me.kuehle.carreport.provider.station.StationContentValues;
import me.kuehle.carreport.provider.station.StationCursor;
import me.kuehle.carreport.provider.station.StationSelection;

public class StationPresenter {

    private Context mContext;
    private CarReportDatabase mDB;

    private StationPresenter(Context context) {
        mContext = context;
        mDB = CarReportDatabase.getInstance(mContext);
    }

    public static StationPresenter getInstance(Context context) {
        return new StationPresenter(context);
    }

    public Map<String, Double> getStationsAndVolumes(long carId) {
        Map<String, Double> stations = new HashMap<>();
        for (Station s: mDB.getStationDao().getAll()) {
            double volume = mDB.getStationDao().getVolumeForStationAndCar(carId, s.getId());
            stations.put(s.getName(), volume);
        }

        return stations;
    }

    public String[] getAllVolumes() {
        Set<String> volumes = new HashSet<>();

        for (Station s: mDB.getStationDao().getAll()) {
            volumes.add(String.valueOf(mDB.getStationDao().getVolumeForStation(s.getId())));
        }

        return volumes.toArray(new String[0]);
    }

    public double getVolumeForStation(long stationId) {
        double volume = 0;
        volume = mDB.getStationDao().getVolumeForStation(stationId);

        return volume;
    }

    public void ensureAtLeastOne() {
        StationCursor cursor = new StationSelection().query(mContext.getContentResolver(), new String[]{StationColumns._ID});
        if (cursor.getCount() == 0) {
            StationContentValues values = new StationContentValues();
            values.putName("Default");
            values.insert(mContext.getContentResolver());
        }
    }

    public long getMostUsedId(long carId) {
        Station mostUsed = mDB.getStationDao().getMostUsedForCar(carId);
        if (mostUsed != null) {
            return mostUsed.getId();
        } else {
            return 0;
        }
    }

    public boolean isUsed(long stationId) {
        return mDB.getStationDao().getUsageCount(stationId) > 0;
    }
}
