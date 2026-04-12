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

import org.juanro.autumandu.model.dto.TireWithDetails;
import org.juanro.autumandu.model.entity.TireList;
import org.juanro.autumandu.model.entity.TireUsage;

@Dao
public interface TireDao {
    @Query("SELECT * FROM tire_list ORDER BY buy_date DESC")
    List<TireList> getAllTireLists();

    @Query("SELECT * FROM tire_list ORDER BY buy_date DESC")
    LiveData<List<TireList>> getAllTireListsLiveData();

    @Query("SELECT * FROM tire_list WHERE _id = :id")
    TireList getTireListById(long id);

    @Query("SELECT * FROM tire_list WHERE _id = :id")
    LiveData<TireList> getTireListByIdLiveData(long id);

    @Query("SELECT * FROM tire_usage ORDER BY date_mount DESC")
    List<TireUsage> getAllTireUsages();

    @Query("SELECT * FROM tire_usage WHERE tire_id = :tireId ORDER BY date_mount DESC")
    List<TireUsage> getUsageByTireId(long tireId);

    @Query("SELECT * FROM tire_usage WHERE tire_id = :tireId ORDER BY date_mount DESC")
    LiveData<List<TireUsage>> getUsageByTireIdLiveData(long tireId);

    @Query("""
        SELECT l.*,
               (SELECT COUNT(*) > 0 FROM tire_usage u WHERE u.tire_id = l._id AND u.date_umount IS NULL) as isMounted,
               IFNULL((
                   SELECT SUM(tu.distance_umount - tu.distance_mount)
                   FROM tire_usage tu
                   WHERE tu.tire_id = l._id AND tu.date_umount IS NOT NULL
               ), 0) + (
                   CASE WHEN EXISTS(SELECT 1 FROM tire_usage u WHERE u.tire_id = l._id AND u.date_umount IS NULL)
                        THEN (
                            SELECT MAX(mileage) FROM (
                                SELECT initial_mileage AS mileage FROM car WHERE _id = l.car_id
                                UNION SELECT mileage FROM refueling WHERE car_id = l.car_id
                                UNION SELECT mileage FROM other_cost WHERE car_id = l.car_id
                                UNION SELECT distance_mount FROM tire_usage tu JOIN tire_list tl ON tu.tire_id = tl._id WHERE tl.car_id = l.car_id
                                UNION SELECT distance_umount FROM tire_usage tu JOIN tire_list tl ON tu.tire_id = tl._id WHERE tl.car_id = l.car_id
                            )
                        ) - (
                            SELECT distance_mount FROM tire_usage u WHERE u.tire_id = l._id AND u.date_umount IS NULL
                        )
                        ELSE 0
                   END
               ) as distance
        FROM tire_list l
        WHERE l.car_id = :carId
        ORDER BY l.buy_date DESC
    """)
    LiveData<List<TireWithDetails>> getAllForCarWithDetailsLiveData(long carId);

    @Query("SELECT EXISTS(SELECT 1 FROM tire_usage u JOIN tire_list l ON u.tire_id = l._id WHERE l._id = :id AND u.date_umount IS NULL)")
    LiveData<Boolean> isTireMountedLiveData(long id);

    @Query("SELECT IFNULL(SUM(l.quantity), 0) FROM tire_usage u JOIN tire_list l ON u.tire_id = l._id WHERE l.car_id = :carId AND u.date_umount IS NULL")
    LiveData<Integer> getNumTiresMountedLiveData(long carId);

    @Query("SELECT * FROM tire_usage WHERE tire_id = :tireId AND date_umount IS NULL LIMIT 1")
    TireUsage getUsageByTireIdNotUmount(long tireId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insert(TireList... tireList);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insert(TireUsage... tireUsage);

    @Update
    void update(TireList... tireList);

    @Update
    void update(TireUsage... tireUsage);

    @Delete
    void delete(TireList... tireList);

    @Delete
    void delete(TireUsage... tireUsage);

    @Query("DELETE FROM tire_list WHERE _id = :id")
    void deleteTireListById(long id);

    @Query("DELETE FROM tire_usage WHERE _id = :id")
    void deleteTireUsageById(long id);

    @Query("DELETE FROM tire_list")
    void deleteAllTireLists();

    @Query("DELETE FROM tire_usage")
    void deleteAllTireUsages();
}
