package org.juanro.autumandu.model.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import org.juanro.autumandu.model.IFuelType;

@Entity(tableName = "fuel_type")
public class FuelType implements IFuelType {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    private Long id;

    @NonNull
    @ColumnInfo(name = "fuel_type__name")
    private String name = "";

    @ColumnInfo(name = "category")
    private String category;

    public FuelType(){
    }

    @Ignore
    public FuelType(@NonNull String name, String category) {
        this.setName(name);
        this.setCategory(category);
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
