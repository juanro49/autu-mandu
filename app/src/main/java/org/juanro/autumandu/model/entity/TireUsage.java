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
package org.juanro.autumandu.model.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import org.juanro.autumandu.model.ITireUsage;

import java.util.Date;

@Entity(tableName = "tire_usage", foreignKeys = {
    @ForeignKey(
        parentColumns = { "_id" },
        childColumns = { "tire_id" },
        entity = TireList.class,
        onDelete = ForeignKey.CASCADE
    ),
})
public class TireUsage implements ITireUsage {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    private Long id;
    @ColumnInfo(name = "distance_mount")
    private int distanceMount;

    @NonNull
    @ColumnInfo(name = "date_mount")
    private Date dateMount = new Date();

    @ColumnInfo(name = "distance_umount")
    private int distanceUmount;

    @ColumnInfo(name = "date_umount")
    private Date dateUmount;

    @ColumnInfo(name = "tire_id")
    private long tireId;

    public TireUsage()
    {
    }

    @Ignore
    public TireUsage(int distanceMount, @NonNull  Date dateMount, int distanceUmount, Date dateUmount, long tireId)
    {
        this.setDistanceMount(distanceMount);
        this.setDateMount(dateMount);
        this.setDistanceUmount(distanceUmount);
        this.setDateUmount(dateUmount);
        this.setTireId(tireId);
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getDistanceMount() {
        return distanceMount;
    }

    public void setDistanceMount(int distanceMount) {
        this.distanceMount = distanceMount;
    }

    @NonNull
    public Date getDateMount() {
        return dateMount;
    }

    public void setDateMount(@NonNull Date dateMount) {
        this.dateMount = dateMount;
    }

    public int getDistanceUmount() {
        return distanceUmount;
    }

    public void setDistanceUmount(int distanceUmount) {
        this.distanceUmount = distanceUmount;
    }

    public Date getDateUmount() {
        return dateUmount;
    }

    public void setDateUmount(Date dateUmount) {
        this.dateUmount = dateUmount;
    }

    public long getTireId() {
        return tireId;
    }

    public void setTireId(long tireId) {
        this.tireId = tireId;
    }
}
