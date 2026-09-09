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
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "tank", indices = {
        @Index("car_id")
}, foreignKeys = {
        @ForeignKey(
                parentColumns = { "_id" },
                childColumns = { "car_id" },
                entity = Car.class,
                onDelete = ForeignKey.CASCADE
        )
})
public class Tank {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    private Long id;

    @ColumnInfo(name = "car_id")
    private long carId;

    @NonNull
    @ColumnInfo(name = "fuel_category")
    private String fuelCategory = "";

    @Nullable
    @ColumnInfo(name = "tank__name")
    private String name;

    @ColumnInfo(name = "capacity")
    private float capacity;

    @ColumnInfo(name = "is_manually_set")
    private boolean isManuallySet;

    public Tank() {}

    @Ignore
    public Tank(long carId, @NonNull String fuelCategory, @Nullable String name, float capacity, boolean isManuallySet) {
        this.carId = carId;
        this.fuelCategory = fuelCategory;
        this.name = name;
        this.capacity = capacity;
        this.isManuallySet = isManuallySet;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getCarId() {
        return carId;
    }

    public void setCarId(long carId) {
        this.carId = carId;
    }

    @NonNull
    public String getFuelCategory() {
        return fuelCategory;
    }

    public void setFuelCategory(@NonNull String fuelCategory) {
        this.fuelCategory = fuelCategory;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    public float getCapacity() {
        return capacity;
    }

    public void setCapacity(float capacity) {
        this.capacity = capacity;
    }

    public boolean isManuallySet() {
        return isManuallySet;
    }

    public void setManuallySet(boolean manuallySet) {
        isManuallySet = manuallySet;
    }
}
