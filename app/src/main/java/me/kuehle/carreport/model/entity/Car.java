package me.kuehle.carreport.model.entity;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import me.kuehle.carreport.model.ICar;

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

    public Car() {}

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
}
