package me.kuehle.carreport.model.entity;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import me.kuehle.carreport.model.IRefueling;

@Entity(tableName = "refueling", foreignKeys = {
        @ForeignKey(
                parentColumns = { "_id" },
                childColumns = { "fuel_type_id" },
                entity = FuelType.class,
                onDelete = ForeignKey.CASCADE
        ),
        @ForeignKey(
                parentColumns = { "_id" },
                childColumns = { "car_id" },
                entity = Car.class,
                onDelete = ForeignKey.CASCADE
        ),
})
public class Refueling implements IRefueling {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    private Long id;

    @NonNull
    @ColumnInfo(name = "date")
    private Date date = new Date();

    @ColumnInfo(name = "mileage")
    private int mileage;

    @ColumnInfo(name = "volume")
    private float volume;

    @ColumnInfo(name = "price")
    private float price;

    @ColumnInfo(name = "partial")
    private boolean partial;

    @NonNull
    @ColumnInfo(name = "note")
    private String note = "";

    @ColumnInfo(name = "fuel_type_id")
    private long fuelTypeId;

    @ColumnInfo(name = "car_id")
    private long carId;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @NonNull
    @Override
    public Date getDate() {
        return date;
    }

    public void setDate(@NonNull Date date) {
        this.date = date;
    }

    @Override
    public int getMileage() {
        return mileage;
    }

    public void setMileage(int mileage) {
        this.mileage = mileage;
    }

    @Override
    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }

    @Override
    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    @Override
    public boolean getPartial() {
        return partial;
    }

    public void setPartial(boolean partial) {
        this.partial = partial;
    }

    @NonNull
    @Override
    public String getNote() {
        return note;
    }

    public void setNote(@NonNull String note) {
        this.note = note;
    }

    @Override
    public long getFuelTypeId() {
        return fuelTypeId;
    }

    public void setFuelTypeId(long fuelTypeId) {
        this.fuelTypeId = fuelTypeId;
    }

    @Override
    public long getCarId() {
        return carId;
    }

    public void setCarId(long carId) {
        this.carId = carId;
    }
}
