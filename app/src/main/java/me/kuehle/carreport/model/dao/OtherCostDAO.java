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

    @Insert
    long[] insert(OtherCost... otherCosts);

    @Update
    void update(OtherCost... otherCosts);

    @Delete
    void delete(OtherCost... otherCosts);
}
