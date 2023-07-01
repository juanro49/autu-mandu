package org.juanro.autumandu.model.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import org.juanro.autumandu.model.entity.Car;

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
