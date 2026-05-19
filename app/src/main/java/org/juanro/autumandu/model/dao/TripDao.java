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

import java.time.LocalDate;
import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RewriteQueriesToDropUnusedColumns;
import androidx.room.Update;
import org.juanro.autumandu.model.dto.TripWithDetails;
import org.juanro.autumandu.model.entity.Trip;

@Dao
public interface TripDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Trip trip);

    @Update
    int update(Trip trip);

    @Delete
    int delete(Trip trip);

    @Query("SELECT * FROM trip WHERE _id = :id")
    Trip getTripById(long id);

    @Query("SELECT * FROM trip WHERE _id = :id")
    LiveData<Trip> getTripByIdLive(long id);

    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT t.*, c.car__name as carName, s.station__name as stationName,
               r.price as refuelingPrice, r.volume as refuelingVolume
        FROM trip t
        JOIN car c ON t.car_id = c._id
        LEFT JOIN refueling r ON t.refueling_id = r._id
        LEFT JOIN station s ON r.station_id = s._id
        WHERE t.car_id = :carId
        ORDER BY t.date DESC, t.time_start DESC
    """)
    LiveData<List<TripWithDetails>> getTripsWithDetailsForCarLive(long carId);

    @Query("SELECT * FROM trip WHERE car_id = :carId ORDER BY date DESC, time_start DESC")
    List<Trip> getTripsForCar(long carId);

    @Query("SELECT * FROM trip WHERE car_id = :carId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC, time_start DESC")
    List<Trip> getTripsInDateRange(long carId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT * FROM trip WHERE car_id = :carId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC, time_start DESC")
    LiveData<List<Trip>> getTripsInDateRangeLive(long carId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT COUNT(*) FROM trip WHERE car_id = :carId")
    int getTripCountForCar(long carId);

    @Query("SELECT SUM(km_end - km_start) FROM trip WHERE car_id = :carId")
    Integer getTotalDistanceForCar(long carId);

    @Query("SELECT SUM(km_business) FROM trip WHERE car_id = :carId")
    Integer getTotalBusinessKm(long carId);

    @Query("SELECT SUM(km_private) FROM trip WHERE car_id = :carId")
    Integer getTotalPrivateKm(long carId);

    @Query("SELECT SUM(km_home_work) FROM trip WHERE car_id = :carId")
    Integer getTotalHomeWorkKm(long carId);

    @Query("SELECT SUM(COALESCE(fuel_cost, 0) + COALESCE(other_costs_amount, 0)) FROM trip WHERE car_id = :carId")
    Double getTotalCostsForCar(long carId);

    @Query("SELECT MAX(km_end) FROM trip WHERE car_id = :carId AND date <= :date ORDER BY date DESC, time_end DESC LIMIT 1")
    Integer getLastKmEndBefore(long carId, LocalDate date);

    @Query("SELECT * FROM trip WHERE car_id = :carId ORDER BY date DESC, time_end DESC LIMIT 1")
    Trip getLastTripForCar(long carId);

    @Query("SELECT * FROM trip WHERE refueling_id = :refuelingId ORDER BY date DESC, time_start DESC")
    LiveData<List<Trip>> getTripsForRefuelingLive(long refuelingId);

    @Query("SELECT * FROM trip ORDER BY date DESC, time_start DESC")
    List<Trip> getAll();

    @Query("DELETE FROM trip")
    void deleteAll();
}
