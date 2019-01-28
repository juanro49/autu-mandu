package me.kuehle.carreport.model.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import me.kuehle.carreport.model.entity.Refueling;

@Dao
public interface RefuelingDAO {
    @Query("SELECT * FROM refueling WHERE car_id = :car_id ORDER BY date")
    List<Refueling> getAllForCar(long car_id);

    @Query("SELECT * FROM refueling ORDER BY date")
    List<Refueling> getAll();

    @Query("SELECT * FROM refueling WHERE _id = :id")
    Refueling getById(long id);

    @Query("SELECT * FROM refueling WHERE car_id = :car_id ORDER BY mileage DESC LIMIT 1")
    Refueling getLastForCar(long car_id);

    @Insert
    long[] insert(Refueling... refuelings);

    @Update
    void update(Refueling... refuelings);

    @Delete
    void delete(Refueling... refuelings);
}
