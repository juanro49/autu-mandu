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

import java.util.List;

public class StationListViewModel extends AndroidViewModel {
    private final AutuManduDatabase db;

    public StationListViewModel(@NonNull Application application) {
        super(application);
        db = AutuManduDatabase.getInstance(application);
    }

    public LiveData<List<StationWithVolume>> getStationsWithVolume() {
        return db.getStationDao().getAllWithVolumeLiveData();
    }

    public LiveData<List<StationWithVolume>> getStationsWithVolume(long carId) {
        return db.getStationDao().getStationsWithVolumeForCarLiveData(carId);
    }

    public void delete(long id) {
        AutuManduDatabase.DB_EXECUTOR.execute(() -> db.getStationDao().deleteById(id));
    }
}
