package org.juanro.autumandu.model.entity;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import org.juanro.autumandu.model.ICar;

@Entity(tableName = "car")
public class Car implements ICar {
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
    private Date suspension;

    @ColumnInfo(name = "buying_price")
    private double buyingprice = 0;

    /*@ColumnInfo(name = "make")
    private String make;

    @ColumnInfo(name = "model")
    private String model;

    @ColumnInfo(name = "year")
    private int year;

    @ColumnInfo(name = "license_plate")
    private String licensePlate;

    @ColumnInfo(name = "buying_date")
    private Date buyingdate;*/

    public Car() {}

    @Ignore
    public Car(@NonNull String name, int color, int initialMileage, Date suspension, double buyingPrice/*, Date buyingDate, String make, String model, String licensePlate, int year*/) {
        this.setName(name);
        this.setColor(color);
        this.setInitialMileage(initialMileage);
        this.setSuspension(suspension);
        this.setBuyingprice(buyingPrice);
        /*this.setBuyingdate(buyingDate);
        this.setMake(make);
        this.setModel(model);
        this.setLicensePlate(licensePlate);
        this.setYear(year);*/
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    @Override
    public int getInitialMileage() {
        return initialMileage;
    }

    public void setInitialMileage(int initialMileage) {
        this.initialMileage = initialMileage;
    }

    @NonNull
    @Override
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    @Override
    public Date getSuspension() {
        return suspension;
    }

    public void setSuspension(Date suspension) {
        this.suspension = suspension;
    }

    @Override
    public double getBuyingprice()
    {
        return buyingprice;
    }

    public void setBuyingprice(double buyingprice)
    {
        this.buyingprice = buyingprice;
    }

    /*@Override
    public String getMake()
    {
        return make;
    }

    public void setMake(String make)
    {
        this.make = make;
    }

    @Override
    public String getModel()
    {
        return model;
    }

    public void setModel(String model)
    {
        this.model = model;
    }

    @Override
    public int getYear()
    {
        return year;
    }

    public void setYear(int year)
    {
        this.year = year;
    }

    @Override
    public String getLicensePlate()
    {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate)
    {
        this.licensePlate = licensePlate;
    }

    @Override
    public Date getBuyingdate()
    {
        return buyingdate;
    }

    public void setBuyingdate(Date buyingdate)
    {
        this.buyingdate = buyingdate;
    }*/
}
