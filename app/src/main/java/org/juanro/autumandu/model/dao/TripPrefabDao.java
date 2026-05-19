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

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import org.juanro.autumandu.model.entity.TripPrefab;

@Dao
public interface TripPrefabDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(TripPrefab prefab);

    @Update
    int update(TripPrefab prefab);

    @Delete
    int delete(TripPrefab prefab);

    @Query("SELECT * FROM trip_prefab WHERE car_id = :carId AND type = :type ORDER BY usage_count DESC, value ASC")
    List<TripPrefab> getPrefabsByType(long carId, String type);

    @Query("SELECT * FROM trip_prefab WHERE car_id = :carId AND type = :type ORDER BY usage_count DESC, value ASC")
    androidx.lifecycle.LiveData<List<TripPrefab>> getPrefabsByTypeLive(long carId, String type);

    @Query("SELECT * FROM trip_prefab WHERE car_id = :carId AND type = :type AND value = :value LIMIT 1")
    TripPrefab getPrefabByTypeAndValue(long carId, String type, String value);

    @Query("SELECT * FROM trip_prefab WHERE car_id = :carId AND type = :type AND value LIKE '%' || :query || '%' ORDER BY usage_count DESC")
    List<TripPrefab> searchPrefabs(long carId, String type, String query);

    @Query("UPDATE trip_prefab SET usage_count = usage_count + 1 WHERE _id = :id")
    void incrementUsageCount(long id);

    @Query("SELECT * FROM trip_prefab ORDER BY usage_count DESC")
    List<TripPrefab> getAll();

    @Query("DELETE FROM trip_prefab")
    void deleteAll();
}
