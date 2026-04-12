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
import org.juanro.autumandu.model.entity.OtherCost;

@Dao
public interface OtherCostDao {
    @Query("SELECT * FROM other_cost ORDER BY date DESC, mileage DESC")
    List<OtherCost> getAll();

    @Query("SELECT * FROM other_cost ORDER BY date DESC, mileage DESC")
    LiveData<List<OtherCost>> getAllLiveData();

    @Query("SELECT * FROM other_cost WHERE car_id = :carId ORDER BY date DESC, mileage DESC")
    List<OtherCost> getByCarId(long carId);

    @Query("SELECT * FROM other_cost WHERE car_id = :carId ORDER BY date DESC, mileage DESC")
    LiveData<List<OtherCost>> getByCarIdLiveData(long carId);

    @Query("SELECT * FROM other_cost WHERE _id = :id")
    OtherCost getById(long id);

    @Query("SELECT * FROM other_cost WHERE _id = :id")
    LiveData<OtherCost> getByIdLiveData(long id);

    @Query("SELECT * FROM other_cost WHERE car_id = :carId ORDER BY date DESC LIMIT 1")
    LiveData<OtherCost> getLastForCarLiveData(long carId);

    @Query("SELECT title FROM other_cost WHERE price > 0 GROUP BY title")
    LiveData<List<String>> getPositiveCostTitlesLiveData();

    @Query("SELECT title FROM other_cost WHERE price < 0 GROUP BY title")
    LiveData<List<String>> getNegativeCostTitlesLiveData();

    @Query("SELECT * FROM other_cost WHERE car_id = :carId AND price > 0 ORDER BY date DESC")
    LiveData<List<OtherCost>> getExpendituresForCarDescending(long carId);

    @Query("SELECT * FROM other_cost WHERE car_id = :carId AND price < 0 ORDER BY date DESC")
    LiveData<List<OtherCost>> getIncomesForCarDescending(long carId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insert(OtherCost... otherCost);

    @Update
    void update(OtherCost... otherCost);

    @Delete
    void delete(OtherCost... otherCost);

    @Query("DELETE FROM other_cost WHERE _id = :id")
    void deleteById(long id);

    @Query("DELETE FROM other_cost")
    void deleteAll();
}
