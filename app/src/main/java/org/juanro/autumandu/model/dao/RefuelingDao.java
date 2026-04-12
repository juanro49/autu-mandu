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

import org.juanro.autumandu.model.dto.RefuelingWithDetails;
import org.juanro.autumandu.model.entity.Refueling;

@Dao
public interface RefuelingDao {

    @Query("""
        SELECT r._id as id, r.date, r.mileage, r.volume, r.price, r.partial, r.note,
               r.fuel_type_id as fuelTypeId, r.station_id as stationId, r.car_id as carId,
               f.fuel_type__name as fuelTypeName, f.category as fuelTypeCategory,
               s.station__name as stationName,
               c.car__name as carName, c.color as carColor, c.initial_mileage as carInitialMileage,
               c.suspended_since as carSuspendedSince, c.buying_price as carBuyingPrice,
               c.num_tires as carNumTires
        FROM refueling r
        LEFT JOIN fuel_type f ON r.fuel_type_id = f._id
        LEFT JOIN station s ON r.station_id = s._id
        JOIN car c ON r.car_id = c._id
        ORDER BY r.date DESC, r.mileage DESC
    """)
    List<RefuelingWithDetails> getAllWithDetails();

    @Query("SELECT * FROM refueling ORDER BY date DESC, mileage DESC")
    List<Refueling> getAll();

    @Query("""
        SELECT r._id as id, r.date, r.mileage, r.volume, r.price, r.partial, r.note,
               r.fuel_type_id as fuelTypeId, r.station_id as stationId, r.car_id as carId,
               f.fuel_type__name as fuelTypeName, f.category as fuelTypeCategory,
               s.station__name as stationName,
               c.car__name as carName, c.color as carColor, c.initial_mileage as carInitialMileage,
               c.suspended_since as carSuspendedSince, c.buying_price as carBuyingPrice,
               c.num_tires as carNumTires
        FROM refueling r
        LEFT JOIN fuel_type f ON r.fuel_type_id = f._id
        LEFT JOIN station s ON r.station_id = s._id
        JOIN car c ON r.car_id = c._id
        WHERE r.car_id = :carId
        ORDER BY r.date DESC, r.mileage DESC
    """)
    List<RefuelingWithDetails> getWithDetailsForCar(long carId);

    @Query("""
        SELECT r._id as id, r.date, r.mileage, r.volume, r.price, r.partial, r.note,
               r.fuel_type_id as fuelTypeId, r.station_id as stationId, r.car_id as carId,
               f.fuel_type__name as fuelTypeName, f.category as fuelTypeCategory,
               s.station__name as stationName,
               c.car__name as carName, c.color as carColor, c.initial_mileage as carInitialMileage,
               c.suspended_since as carSuspendedSince, c.buying_price as carBuyingPrice,
               c.num_tires as carNumTires
        FROM refueling r
        LEFT JOIN fuel_type f ON r.fuel_type_id = f._id
        LEFT JOIN station s ON r.station_id = s._id
        JOIN car c ON r.car_id = c._id
        WHERE r.car_id = :carId
        ORDER BY r.date DESC, r.mileage DESC
    """)
    LiveData<List<RefuelingWithDetails>> getWithDetailsForCarLiveData(long carId);

    @Query("""
        SELECT r._id as id, r.date, r.mileage, r.volume, r.price, r.partial, r.note,
               r.fuel_type_id as fuelTypeId, r.station_id as stationId, r.car_id as carId,
               f.fuel_type__name as fuelTypeName, f.category as fuelTypeCategory,
               s.station__name as stationName,
               c.car__name as carName, c.color as carColor, c.initial_mileage as carInitialMileage,
               c.suspended_since as carSuspendedSince, c.buying_price as carBuyingPrice,
               c.num_tires as carNumTires
        FROM refueling r
        LEFT JOIN fuel_type f ON r.fuel_type_id = f._id
        LEFT JOIN station s ON r.station_id = s._id
        JOIN car c ON r.car_id = c._id
        WHERE r.car_id = :carId AND f.category = :category
        ORDER BY r.date DESC, r.mileage DESC
    """)
    List<RefuelingWithDetails> getWithDetailsForCarAndCategory(long carId, String category);

    @Query("""
        SELECT r._id as id, r.date, r.mileage, r.volume, r.price, r.partial, r.note,
               r.fuel_type_id as fuelTypeId, r.station_id as stationId, r.car_id as carId,
               f.fuel_type__name as fuelTypeName, f.category as fuelTypeCategory,
               s.station__name as stationName,
               c.car__name as carName, c.color as carColor, c.initial_mileage as carInitialMileage,
               c.suspended_since as carSuspendedSince, c.buying_price as carBuyingPrice,
               c.num_tires as carNumTires
        FROM refueling r
        LEFT JOIN fuel_type f ON r.fuel_type_id = f._id
        LEFT JOIN station s ON r.station_id = s._id
        JOIN car c ON r.car_id = c._id
        WHERE r.car_id = :carId AND f.category = :category
        ORDER BY r.date DESC, r.mileage DESC
    """)
    LiveData<List<RefuelingWithDetails>> getWithDetailsForCarAndCategoryLiveData(long carId, String category);

    @Query("""
        SELECT r._id as id, r.date, r.mileage, r.volume, r.price, r.partial, r.note,
               r.fuel_type_id as fuelTypeId, r.station_id as stationId, r.car_id as carId,
               f.fuel_type__name as fuelTypeName, f.category as fuelTypeCategory,
               s.station__name as stationName,
               c.car__name as carName, c.color as carColor, c.initial_mileage as carInitialMileage,
               c.suspended_since as carSuspendedSince, c.buying_price as carBuyingPrice,
               c.num_tires as carNumTires
        FROM refueling r
        LEFT JOIN fuel_type f ON r.fuel_type_id = f._id
        LEFT JOIN station s ON r.station_id = s._id
        JOIN car c ON r.car_id = c._id
        WHERE r._id = :id
    """)
    LiveData<RefuelingWithDetails> getByIdWithDetailsLiveData(long id);

    @Query("""
        SELECT r._id as id, r.date, r.mileage, r.volume, r.price, r.partial, r.note,
               r.fuel_type_id as fuelTypeId, r.station_id as stationId, r.car_id as carId,
               f.fuel_type__name as fuelTypeName, f.category as fuelTypeCategory,
               s.station__name as stationName,
               c.car__name as carName, c.color as carColor, c.initial_mileage as carInitialMileage,
               c.suspended_since as carSuspendedSince, c.buying_price as carBuyingPrice,
               c.num_tires as carNumTires
        FROM refueling r
        LEFT JOIN fuel_type f ON r.fuel_type_id = f._id
        LEFT JOIN station s ON r.station_id = s._id
        JOIN car c ON r.car_id = c._id
        WHERE r._id = :id
    """)
    RefuelingWithDetails getByIdWithDetails(long id);

    @Query("""
        SELECT * FROM refueling
        WHERE car_id = :carId AND date < :date
        ORDER BY date DESC, mileage DESC
        LIMIT 1
    """)
    Refueling getPrevious(long carId, java.util.Date date);

    @Query("""
        SELECT * FROM refueling
        WHERE car_id = :carId AND date > :date
        ORDER BY date ASC, mileage ASC
        LIMIT 1
    """)
    Refueling getNext(long carId, java.util.Date date);

    @Query("SELECT * FROM refueling ORDER BY date DESC, mileage DESC")
    LiveData<List<Refueling>> getAllLiveData();

    @Query("SELECT * FROM refueling WHERE car_id = :carId ORDER BY date DESC, mileage DESC")
    List<Refueling> getByCarId(long carId);

    @Query("SELECT * FROM refueling WHERE car_id = :carId ORDER BY date DESC, mileage DESC")
    LiveData<List<Refueling>> getByCarIdLiveData(long carId);

    @Query("SELECT * FROM refueling WHERE _id = :id")
    Refueling getById(long id);

    @Query("SELECT * FROM refueling WHERE _id = :id")
    LiveData<Refueling> getByIdLiveData(long id);

    @Query("SELECT * FROM refueling WHERE car_id = :carId ORDER BY date DESC, mileage DESC LIMIT 1")
    LiveData<Refueling> getLastForCarLiveData(long carId);

    @Query("SELECT * FROM refueling ORDER BY date ASC, mileage ASC LIMIT 1")
    Refueling getFirst();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insert(Refueling... refueling);

    @Update
    void update(Refueling... refueling);

    @Delete
    void delete(Refueling... refueling);

    @Query("DELETE FROM refueling WHERE _id = :id")
    void deleteById(long id);

    @Query("DELETE FROM refueling")
    void deleteAll();
}
