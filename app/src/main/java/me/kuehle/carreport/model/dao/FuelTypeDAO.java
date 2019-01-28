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

    @Query("SELECT ft.* FROM refueling r INNER JOIN fuel_type ft ON r.fuel_type_id = ft._id " +
        "WHERE r.car_id = :car_id GROUP BY r.fuel_type_id ORDER BY count(r._id) DESC LIMIT 1")
    FuelType getMostUsedForCar(long car_id);

    @Query("SELECT DISTINCT ft.* FROM refueling r INNER JOIN fuel_type ft ON " +
        "r.fuel_type_id = ft._id WHERE r.car_id = :car_id")
    List<FuelType> getFuelTypesForCar(long car_id);

    @Query("SELECT count(_id) FROM refueling WHERE fuel_type_id = :id")
    int getUsageCount(long id);

    @Insert
    long[] insert(FuelType... fuelTypes);

    @Update
    void update(FuelType... fuelTypes);

    @Delete
    void delete(FuelType... fuelTypes);
}
