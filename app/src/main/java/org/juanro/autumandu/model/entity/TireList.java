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

import org.juanro.autumandu.model.ITireList;

import java.util.Date;

@Entity(tableName = "tire_list", foreignKeys = {
    @ForeignKey(
        parentColumns = { "_id" },
        childColumns = { "car_id" },
        entity = Car.class,
        onDelete = ForeignKey.CASCADE
    ),
})
public class TireList implements ITireList {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    private Long id;

    @NonNull
    @ColumnInfo(name = "buy_date")
    private Date buyDate = new Date();

    @ColumnInfo(name = "trash_date")
    private Date trashDate;

    @ColumnInfo(name = "price")
    private float price = 0;

    @ColumnInfo(name = "quantity")
    private int quantity = 0;

    @NonNull
    @ColumnInfo(name = "manufacturer")
    private String manufacturer = "";

    @NonNull
    @ColumnInfo(name = "model")
    private String model = "";

    @NonNull
    @ColumnInfo(name = "note")
    private String note = "";

    @ColumnInfo(name = "car_id")
    private long carId;

    public TireList()
    {
    }

    @Ignore
    public TireList(long carId, @NonNull Date buyDate, Date trashDate, float price, int quantity,
                    @NonNull String manufacturer, @NonNull String model, @NonNull String note)
    {
        this.setCarId(carId);
        this.setBuyDate(buyDate);
        this.setTrashDate(trashDate);
        this.setPrice(price);
        this.setQuantity(quantity);
        this.setManufacturer(manufacturer);
        this.setModel(model);
        this.setNote(note);
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @NonNull
    public Date getBuyDate() {
        return buyDate;
    }

    public void setBuyDate(@NonNull Date buyDate) {
        this.buyDate = buyDate;
    }

    public Date getTrashDate() {
        return trashDate;
    }

    public void setTrashDate(Date trashDate) {
        this.trashDate = trashDate;
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @NonNull
    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(@NonNull String manufacturer) {
        this.manufacturer = manufacturer;
    }

    @NonNull
    public String getModel() {
        return model;
    }

    public void setModel(@NonNull String model) {
        this.model = model;
    }

    @NonNull
    public String getNote() {
        return note;
    }

    public void setNote(@NonNull String note) {
        this.note = note;
    }

    @Override
    public long getCarId() {
        return carId;
    }

    public void setCarId(long carId) {
        this.carId = carId;
    }
}
