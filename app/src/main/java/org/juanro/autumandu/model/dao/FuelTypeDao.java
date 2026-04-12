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
import org.juanro.autumandu.model.entity.FuelType;

@Dao
public interface FuelTypeDao {
    @Query("SELECT * FROM fuel_type ORDER BY fuel_type__name ASC")
    List<FuelType> getAll();

    @Query("SELECT * FROM fuel_type ORDER BY fuel_type__name ASC")
    LiveData<List<FuelType>> getAllLiveData();

    @Query("SELECT * FROM fuel_type WHERE _id = :id")
    FuelType getById(long id);

    @Query("SELECT * FROM fuel_type WHERE _id = :id")
    LiveData<FuelType> getByIdLiveData(long id);

    @Query("SELECT COUNT(*) FROM refueling WHERE fuel_type_id = :id")
    int getUsageCount(long id);

    @androidx.room.RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT f.*, COUNT(r._id) as count
        FROM fuel_type f
        LEFT JOIN refueling r ON f._id = r.fuel_type_id
        WHERE r.car_id = :carId
        GROUP BY f._id
        ORDER BY count DESC
        LIMIT 1
    """)
    LiveData<FuelType> getMostUsedForCarLiveData(long carId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insert(FuelType... fuelType);

    @Update
    void update(FuelType... fuelType);

    @Delete
    void delete(FuelType... fuelType);

    @Query("DELETE FROM fuel_type WHERE _id = :id")
    void deleteById(long id);

    @Query("DELETE FROM fuel_type")
    void deleteAll();
}
