package me.kuehle.carreport.model.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import me.kuehle.carreport.model.entity.Reminder;

@Dao
public interface ReminderDAO {
    @Query("SELECT * FROM reminder ORDER BY title")
    List<Reminder> getAll();

    @Query("SELECT * FROM reminder WHERE _id = :id")
    Reminder getById(long id);

    @Insert
    long[] insert(Reminder... reminders);

    @Update
    void update(Reminder... reminders);

    @Delete
    void delete(Reminder... reminders);
}
