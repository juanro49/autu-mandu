/*
 * Copyright 2023 Juanro49
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

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import org.juanro.autumandu.model.entity.Station;

@Dao
public interface StationDAO {
    @Query("SELECT * FROM station ORDER BY station__name")
    List<Station> getAll();

    @Query("SELECT * FROM station WHERE _id = :id")
    Station getById(long id);

    @Query("SELECT s.* FROM refueling r INNER JOIN station s ON r.station_id = s._id " +
        "WHERE r.car_id = :car_id GROUP BY r.station_id ORDER BY count(r._id) DESC")
    List<Station> getUsedForCar(long car_id);

    @Query("SELECT s.* FROM refueling r INNER JOIN station s ON r.station_id = s._id " +
        "WHERE r.car_id = :car_id GROUP BY r.station_id ORDER BY count(r._id) DESC LIMIT 1")
    Station getMostUsedForCar(long car_id);

    @Query("SELECT sum(r.volume) AS volume FROM refueling r INNER JOIN station s ON r.station_id = s._id " +
        "WHERE r.car_id = :car_id AND r.station_id = :station_id")
    double getVolumeForStationAndCar(long car_id, long station_id);

    @Query("SELECT sum(r.volume) AS volume FROM refueling r INNER JOIN station s ON r.station_id = s._id " +
        "WHERE r.station_id = :station_id")
    double getVolumeForStation(long station_id);

    @Query("SELECT DISTINCT s.* FROM refueling r INNER JOIN station s ON " +
        "r.station_id = s._id WHERE r.car_id = :car_id")
    List<Station> getStationsForCar(long car_id);

    @Query("SELECT count(_id) FROM refueling WHERE station_id = :id")
    int getUsageCount(long id);

    @Insert
    long[] insert(Station... stations);

    @Update
    void update(Station... station);

    @Delete
    void delete(Station... stations);
}
