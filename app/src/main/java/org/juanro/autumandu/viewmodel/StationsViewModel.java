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
import org.juanro.autumandu.model.dto.StationWithVolume;
import org.juanro.autumandu.model.entity.Station;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class StationsViewModel extends AndroidViewModel {
    private final AutuManduDatabase db;
    private final LiveData<List<StationWithVolume>> stations;
    private static final Executor DB_EXECUTOR = Executors.newSingleThreadExecutor();

    public StationsViewModel(@NonNull Application application) {
        super(application);
        db = AutuManduDatabase.getInstance(application);
        stations = db.getStationDao().getAllWithVolumeLiveData();
    }

    public LiveData<List<StationWithVolume>> getStations() {
        return stations;
    }

    public void saveStation(Station station) {
        DB_EXECUTOR.execute(() -> {
            if (station.getId() != null && station.getId() > 0) {
                db.getStationDao().update(station);
            } else {
                db.getStationDao().insert(station);
            }
        });
    }

    public void checkUsage(long[] ids, UsageCheckCallback callback) {
        DB_EXECUTOR.execute(() -> {
            boolean isUsed = false;
            for (long id : ids) {
                if (db.getStationDao().getUsageCount(id) > 0) {
                    isUsed = true;
                    break;
                }
            }
            callback.onResult(isUsed);
        });
    }

    public void deleteStations(long[] ids) {
        DB_EXECUTOR.execute(() -> {
            for (long id : ids) {
                db.getStationDao().deleteById(id);
            }
        });
    }

    public interface UsageCheckCallback {
        void onResult(boolean isUsed);
    }
}
