package org.juanro.autumandu.model.entity;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "refueling", indices = {
        @Index("fuel_type_id"),
        @Index("station_id"),
        @Index("car_id"),
        @Index("tank_id")
}, foreignKeys = {
        @ForeignKey(
                parentColumns = { "_id" },
                childColumns = { "fuel_type_id" },
                entity = FuelType.class,
                onDelete = ForeignKey.CASCADE
        ),
        @ForeignKey(
                parentColumns = { "_id" },
                childColumns = { "station_id" },
                entity = Station.class,
                onDelete = ForeignKey.CASCADE
        ),
        @ForeignKey(
                parentColumns = { "_id" },
                childColumns = { "car_id" },
                entity = Car.class,
                onDelete = ForeignKey.CASCADE
        ),
        @ForeignKey(
                parentColumns = { "_id" },
                childColumns = { "tank_id" },
                entity = Tank.class,
                onDelete = ForeignKey.CASCADE
        ),
})
public class Refueling {
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

    @ColumnInfo(name = "station_id")
    private long stationId;

    @ColumnInfo(name = "car_id")
    private long carId;

    @ColumnInfo(name = "start_level")
    private float startLevel;

    @ColumnInfo(name = "end_level")
    private float endLevel;

    @ColumnInfo(name = "tank_id")
    private long tankId;

    /**
     * Empty constructor required by Room for entity instantiation.
     */
    public Refueling() {
        // Required by Room
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @NonNull
    public Date getDate() {
        return date;
    }

    public void setDate(@NonNull Date date) {
        this.date = date;
    }

    public int getMileage() {
        return mileage;
    }

    public void setMileage(int mileage) {
        this.mileage = mileage;
    }

    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    public boolean isPartial() {
        return partial;
    }

    public void setPartial(boolean partial) {
        this.partial = partial;
    }

    @NonNull
    public String getNote() {
        return note;
    }

    public void setNote(@NonNull String note) {
        this.note = note;
    }

    public long getFuelTypeId() {
        return fuelTypeId;
    }

    public void setFuelTypeId(long fuelTypeId) {
        this.fuelTypeId = fuelTypeId;
    }

    public long getCarId() {
        return carId;
    }

    public void setCarId(long carId) {
        this.carId = carId;
    }

    public long getStationId() {
        return stationId;
    }

    public void setStationId(long stationId) {
        this.stationId = stationId;
    }

    public float getStartLevel() {
        return startLevel;
    }

    public void setStartLevel(float startLevel) {
        this.startLevel = startLevel;
    }

    public float getEndLevel() {
        return endLevel;
    }

    public void setEndLevel(float endLevel) {
        this.endLevel = endLevel;
    }

    public long getTankId() {
        return tankId;
    }

    public void setTankId(long tankId) {
        this.tankId = tankId;
    }
}
