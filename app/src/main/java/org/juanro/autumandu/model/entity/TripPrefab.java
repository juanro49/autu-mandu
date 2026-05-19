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

package org.juanro.autumandu.model.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "trip_prefab",
    foreignKeys = @ForeignKey(
        entity = Car.class,
        parentColumns = "_id",
        childColumns = "car_id",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {
        @Index(value = {"car_id", "type"}),
        @Index(value = {"car_id", "type", "value"}, unique = true)
    }
)
public class TripPrefab {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    private long id;

    @ColumnInfo(name = "car_id")
    private long carId;

    @NonNull
    @ColumnInfo(name = "type")
    private String type = ""; // route, purpose, driver, company

    @NonNull
    @ColumnInfo(name = "value")
    private String value = "";

    @ColumnInfo(name = "usage_count")
    private int usageCount = 1;

    public TripPrefab() {
        // Required by Room
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCarId() {
        return carId;
    }

    public void setCarId(long carId) {
        this.carId = carId;
    }

    @NonNull
    public String getType() {
        return type;
    }

    public void setType(@NonNull String type) {
        this.type = type;
    }

    @NonNull
    public String getValue() {
        return value;
    }

    public void setValue(@NonNull String value) {
        this.value = value;
    }

    public int getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
    }
}
