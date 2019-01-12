package me.kuehle.carreport.model.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import me.kuehle.carreport.model.entity.Car;

@Dao
public interface CarDAO {
    @Query("SELECT * FROM car ORDER BY car__name")
    List<Car> getAll();

    @Query("SELECT * FROM car WHERE _id = :id")
    Car getById(long id);

    @Insert
    long[] insert(Car... car);

    @Update
    void update(Car... car);

    @Delete
    void delete(Car... car);
}
