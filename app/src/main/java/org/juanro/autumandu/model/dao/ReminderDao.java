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

import org.juanro.autumandu.model.dto.ReminderWithCar;
import org.juanro.autumandu.model.entity.Reminder;

@Dao
public interface ReminderDao {
    @Query("SELECT * FROM reminder ORDER BY _id ASC")
    List<Reminder> getAll();

    @Query("SELECT * FROM reminder ORDER BY _id ASC")
    LiveData<List<Reminder>> getAllLiveData();

    @Query("SELECT * FROM reminder WHERE car_id = :carId")
    List<Reminder> getByCarId(long carId);

    @Query("SELECT * FROM reminder WHERE car_id = :carId")
    LiveData<List<Reminder>> getByCarIdLiveData(long carId);

    @Query("SELECT * FROM reminder WHERE _id = :id")
    Reminder getById(long id);

    @Query("SELECT * FROM reminder WHERE _id = :id")
    LiveData<Reminder> getByIdLiveData(long id);

    @Query("SELECT r.*, c.car__name AS carName FROM reminder r JOIN car c ON r.car_id = c._id")
    LiveData<List<ReminderWithCar>> getAllWithCarLiveData();

    @Query("SELECT r.*, c.car__name AS carName FROM reminder r JOIN car c ON r.car_id = c._id")
    List<ReminderWithCar> getAllWithCar();

    @Query("SELECT r.*, c.car__name AS carName FROM reminder r JOIN car c ON r.car_id = c._id WHERE r._id = :id")
    ReminderWithCar getByIdWithCar(long id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insert(Reminder... reminder);

    @Update
    void update(Reminder... reminder);

    @Delete
    void delete(Reminder... reminder);

    @Query("DELETE FROM reminder WHERE _id = :id")
    void deleteById(long id);

    @Query("DELETE FROM reminder")
    void deleteAll();
}
