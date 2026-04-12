package org.juanro.autumandu.model.entity;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "car")
public class Car {
    @ColumnInfo(name = "_id")
    @PrimaryKey(autoGenerate = true)
    private Long id;

    @ColumnInfo(name = "color")
    private int color;

    @ColumnInfo(name = "initial_mileage")
    private int initialMileage = 0;

    @NonNull
    @ColumnInfo(name = "car__name")
    private String name = "";

    @ColumnInfo(name = "suspended_since")
    private Date suspendedSince;

    @ColumnInfo(name = "buying_price")
    private double buyingPrice = 0;

    @ColumnInfo(name = "num_tires")
    private int numTires = 4;

    public Car() {}

    @Ignore
    public Car(@NonNull String name, int color, int initialMileage, @Nullable Date suspendedSince, double buyingPrice, int numTires) {
        this.name = name;
        this.color = color;
        this.initialMileage = initialMileage;
        this.suspendedSince = suspendedSince;
        this.buyingPrice = buyingPrice;
        this.numTires = numTires;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getInitialMileage() {
        return initialMileage;
    }

    public void setInitialMileage(int initialMileage) {
        this.initialMileage = initialMileage;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    @Nullable
    public Date getSuspendedSince() {
        return suspendedSince;
    }

    public void setSuspendedSince(@Nullable Date suspendedSince) {
        this.suspendedSince = suspendedSince;
    }

    public double getBuyingPrice() {
        return buyingPrice;
    }

    public void setBuyingPrice(double buyingPrice) {
        this.buyingPrice = buyingPrice;
    }

    public int getNumTires() {
        return numTires;
    }

    public void setNumTires(int numTires) {
        this.numTires = numTires;
    }
}
