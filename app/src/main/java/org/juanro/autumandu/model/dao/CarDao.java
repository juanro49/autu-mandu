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
import org.juanro.autumandu.model.entity.Car;

@Dao
public interface CarDao {
    @Query("SELECT * FROM car ORDER BY car__name ASC")
    List<Car> getAll();

    @Query("SELECT * FROM car ORDER BY car__name ASC")
    LiveData<List<Car>> getAllLiveData();

    @Query("SELECT * FROM car WHERE _id = :id")
    Car getById(long id);

    @Query("SELECT * FROM car WHERE _id = :id")
    LiveData<Car> getByIdLiveData(long id);

    @Query("SELECT COUNT(*) FROM car")
    int getCount();

    @Query("SELECT COUNT(*) FROM car")
    LiveData<Integer> getCountLiveData();

    @Query("SELECT * FROM car WHERE suspended_since IS NULL ORDER BY car__name ASC")
    List<Car> getNotSuspended();

    @Query("SELECT * FROM car WHERE suspended_since IS NULL ORDER BY car__name ASC")
    LiveData<List<Car>> getNotSuspendedLiveData();

    @Query("SELECT num_tires FROM car WHERE _id = :id")
    LiveData<Integer> getCarNumTiresLiveData(long id);

    @Query("SELECT MAX(mileage) FROM (" +
            "SELECT initial_mileage AS mileage FROM car WHERE _id = :carId " +
            "UNION SELECT mileage FROM refueling WHERE car_id = :carId " +
            "UNION SELECT mileage FROM other_cost WHERE car_id = :carId " +
            "UNION SELECT distance_mount FROM tire_usage tu JOIN tire_list tl ON tu.tire_id = tl._id WHERE tl.car_id = :carId " +
            "UNION SELECT distance_umount FROM tire_usage tu JOIN tire_list tl ON tu.tire_id = tl._id WHERE tl.car_id = :carId" +
            ")")
    int getLatestMileage(long carId);

    @Query("SELECT MIN(mileage) FROM (" +
            "SELECT initial_mileage AS mileage FROM car WHERE _id = :carId " +
            "UNION SELECT mileage FROM refueling WHERE car_id = :carId AND mileage > 0 " +
            "UNION SELECT mileage FROM other_cost WHERE car_id = :carId AND mileage > 0 " +
            "UNION SELECT distance_mount FROM tire_usage tu JOIN tire_list tl ON tu.tire_id = tl._id WHERE tl.car_id = :carId AND distance_mount > 0 " +
            "UNION SELECT distance_umount FROM tire_usage tu JOIN tire_list tl ON tu.tire_id = tl._id WHERE tl.car_id = :carId AND distance_umount > 0" +
            ")")
    Integer getEarliestMileage(long carId);

    @Query("SELECT MAX(mileage) FROM (" +
            "SELECT initial_mileage AS mileage FROM car WHERE _id = :carId " +
            "UNION SELECT mileage FROM refueling WHERE car_id = :carId " +
            "UNION SELECT mileage FROM other_cost WHERE car_id = :carId " +
            "UNION SELECT distance_mount FROM tire_usage tu JOIN tire_list tl ON tu.tire_id = tl._id WHERE tl.car_id = :carId " +
            "UNION SELECT distance_umount FROM tire_usage tu JOIN tire_list tl ON tu.tire_id = tl._id WHERE tl.car_id = :carId" +
            ")")
    LiveData<Integer> getLatestMileageLiveData(long carId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insert(Car... car);

    @Update
    void update(Car... car);

    @Delete
    void delete(Car... car);

    @Query("DELETE FROM car WHERE _id = :id")
    void deleteById(long id);

    @Query("DELETE FROM car")
    void deleteAll();
}
