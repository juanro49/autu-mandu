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

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import org.juanro.autumandu.model.entity.Tank;

import java.util.List;

@Dao
public interface TankDao {
    @Query("SELECT * FROM tank")
    List<Tank> getAll();

    @Query("SELECT * FROM tank WHERE car_id = :carId ORDER BY fuel_category ASC")
    List<Tank> getTanksForCar(long carId);

    @Query("SELECT * FROM tank WHERE car_id = :carId ORDER BY fuel_category ASC")
    LiveData<List<Tank>> getTanksForCarLiveData(long carId);

    @Query("SELECT * FROM tank WHERE car_id = :carId AND fuel_category = :category LIMIT 1")
    Tank getTankForCarByCategory(long carId, String category);

    @Query("SELECT * FROM tank WHERE _id = :id")
    Tank getById(long id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insert(Tank... tank);

    @Update
    void update(Tank... tank);

    @Delete
    void delete(Tank... tank);

    @Query("DELETE FROM tank WHERE _id = :id")
    void deleteById(long id);
}
