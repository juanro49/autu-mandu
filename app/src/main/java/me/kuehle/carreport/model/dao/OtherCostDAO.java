package me.kuehle.carreport.model.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import me.kuehle.carreport.model.entity.OtherCost;

@Dao
public interface OtherCostDAO {
    @Query("SELECT * FROM other_cost ORDER BY date")
    List<OtherCost> getAll();

    @Query("SELECT * FROM other_cost WHERE _id = :id")
    OtherCost getById(long id);

    @Query("SELECT * FROM other_cost WHERE car_id = :car AND mileage IS NOT NULL ORDER BY mileage DESC LIMIT 1")
    OtherCost getLastForCar(long car);

    @Query("SELECT DISTINCT title FROM other_cost WHERE price >= 0 ORDER BY title")
    List<String> getPositiveCostTitles();

    @Query("SELECT DISTINCT title FROM other_cost WHERE price < 0 ORDER BY title")
    List<String> getNegativeCostTitles();

    @Insert
    long[] insert(OtherCost... otherCosts);

    @Update
    void update(OtherCost... otherCosts);

    @Delete
    void delete(OtherCost... otherCosts);
}
