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
package org.juanro.autumandu.model.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import org.juanro.autumandu.model.entity.TireList;
import org.juanro.autumandu.model.entity.TireUsage;

import java.util.List;

@Dao
public interface TireDAO {

    @Query("SELECT * FROM tire_list ORDER BY buy_date DESC")
    List<TireList> getAllTires();

    @Query("SELECT * FROM tire_usage ORDER BY _id DESC")
    List<TireUsage> getAllTireUsages();

    @Query("SELECT * FROM tire_list WHERE car_id = :car")
    List<TireList> getAllForCar(long car);

    @Query("SELECT SUM(tl.quantity) FROM tire_usage tu, tire_list tl WHERE tu.tire_id == tl._id AND car_id = :car AND tu.date_umount IS NULL")
    int getNumTiresMounted(long car);

    @Query("select sum(distance_umount - distance_mount) from tire_usage WHERE tire_id = :tire_id")
    int getTireDistance(long tire_id);

    @Query("select distance_mount from tire_usage WHERE tire_id = :tire_id AND date_umount IS NULL")
    int getTireLastMountDistance(long tire_id);

    @Query("SELECT count(*) FROM tire_usage WHERE tire_id = :tire_id AND date_umount IS NULL")
    int getIsTireMounted(long tire_id);

    @Query("SELECT count(*) FROM tire_list WHERE _id = :tire_id AND trash_date IS NOT NULL")
    int getIsTireTrashed(long tire_id);

    @Query("select car_id from tire_list WHERE _id = :tire_id")
    int getCarOfTire(long tire_id);

    @Insert
    long[] insert(TireUsage... tire_usage);

    @Update
    void update(TireUsage... tire_usage);

    @Delete
    void delete(TireUsage... tire_usage);

    @Insert
    long[] insert(TireList... tire_list);

    @Update
    void update(TireList... tire_list);

    @Delete
    void delete(TireList... tire_list);
}
