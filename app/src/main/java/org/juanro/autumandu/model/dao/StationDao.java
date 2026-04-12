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

package org.juanro.autumandu.model.dao;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.OnConflictStrategy;
import org.juanro.autumandu.model.entity.Station;

@Dao
public interface StationDao {
    @Query("SELECT * FROM station ORDER BY station__name ASC")
    List<Station> getAll();

    @Query("SELECT * FROM station ORDER BY station__name ASC")
    LiveData<List<Station>> getAllLiveData();

    @Query("SELECT * FROM station WHERE _id = :id")
    Station getById(long id);

    @Query("SELECT * FROM station WHERE _id = :id")
    LiveData<Station> getByIdLiveData(long id);

    @Query("SELECT COUNT(*) FROM refueling WHERE station_id = :id")
    int getUsageCount(long id);

    @androidx.room.RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT s.*, COUNT(r._id) as count
        FROM station s
        LEFT JOIN refueling r ON s._id = r.station_id
        WHERE r.car_id = :carId
        GROUP BY s._id
        ORDER BY count DESC
        LIMIT 1
    """)
    LiveData<Station> getMostUsedForCarLiveData(long carId);

    @Query("""
        SELECT s.*, SUM(r.volume) AS totalVolume
        FROM station s
        LEFT JOIN refueling r ON s._id = r.station_id
        WHERE r.car_id = :carId OR r.car_id IS NULL
        GROUP BY s._id
        ORDER BY totalVolume DESC, s.station__name ASC
    """)
    LiveData<List<org.juanro.autumandu.model.dto.StationWithVolume>> getStationsWithVolumeForCarLiveData(long carId);

    @Query("""
        SELECT s.*, SUM(r.volume) AS totalVolume
        FROM station s
        LEFT JOIN refueling r ON s._id = r.station_id
        GROUP BY s._id
        ORDER BY totalVolume DESC, s.station__name ASC
    """)
    LiveData<List<org.juanro.autumandu.model.dto.StationWithVolume>> getAllWithVolumeLiveData();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insert(Station... station);

    @Update
    void update(Station... station);

    @Delete
    void delete(Station... station);

    @Query("DELETE FROM station WHERE _id = :id")
    void deleteById(long id);

    @Query("DELETE FROM station")
    void deleteAll();
}
