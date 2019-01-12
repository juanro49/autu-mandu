package me.kuehle.carreport.model.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import me.kuehle.carreport.model.entity.FuelType;

@Dao
public interface FuelTypeDAO {
    @Query("SELECT * FROM fuel_type ORDER BY fuel_type__name")
    List<FuelType> getAll();

    @Query("SELECT * FROM fuel_type WHERE _id = :id")
    FuelType getById(long id);

    @Insert
    long[] insert(FuelType... fuelTypes);

    @Update
    void update(FuelType... fuelTypes);

    @Delete
    void delete(FuelType... fuelTypes);
}
