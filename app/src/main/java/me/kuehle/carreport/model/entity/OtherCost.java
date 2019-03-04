package me.kuehle.carreport.model.entity;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import me.kuehle.carreport.model.IOtherCost;
import me.kuehle.carreport.model.entity.helper.RecurrenceInterval;

@Entity(tableName = "other_cost", foreignKeys = {
        @ForeignKey(
                childColumns = { "car_id" },
                parentColumns = { "_id" },
                entity = Car.class,
                onDelete = ForeignKey.CASCADE
        )
})
public class OtherCost implements IOtherCost {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    private Long id;

    @NonNull
    @ColumnInfo(name = "title")
    private String title = "";

    @NonNull
    @ColumnInfo(name = "date")
    private Date date = new Date();

    @ColumnInfo(name = "mileage")
    private Integer mileage;

    @ColumnInfo(name = "price")
    private float price;

    @NonNull
    @ColumnInfo(name = "recurrence_interval")
    private RecurrenceInterval recurrenceInterval = RecurrenceInterval.values()[0];

    @ColumnInfo(name = "recurrence_multiplier")
    private int recurrenceMultiplier;

    @ColumnInfo(name = "end_date")
    private Date endDate;

    @NonNull
    @ColumnInfo(name = "note")
    private String note = "";

    @ColumnInfo(name = "car_id")
    private long carId;

    public OtherCost() {}

    public OtherCost(@NonNull String title, long carId, @NonNull Date date, Integer mileage,
                     float price, @NonNull RecurrenceInterval recurrenceInterval,
                     int recurrenceMultiplier, Date endDate, @NonNull String note) {
        this.setTitle(title);
        this.setCarId(carId);
        this.setDate(date);
        this.setMileage(mileage);
        this.setPrice(price);
        this.setRecurrenceInterval(recurrenceInterval);
        this.setRecurrenceMultiplier(recurrenceMultiplier);
        this.setEndDate(endDate);
        this.setNote(note);
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
    public String getTitle() {
        return title;
    }

    public void setTitle(@NonNull String title) {
        this.title = title;
    }

    @NonNull
    @Override
    public Date getDate() {
        return date;
    }

    public void setDate(@NonNull Date date) {
        this.date = date;
    }

    @Override
    public Integer getMileage() {
        return mileage;
    }

    public void setMileage(Integer mileage) {
        this.mileage = mileage;
    }

    @Override
    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    @NonNull
    @Override
    public RecurrenceInterval getRecurrenceInterval() {
        return recurrenceInterval;
    }

    public void setRecurrenceInterval(@NonNull RecurrenceInterval recurrenceInterval) {
        this.recurrenceInterval = recurrenceInterval;
    }

    @Override
    public int getRecurrenceMultiplier() {
        return recurrenceMultiplier;
    }

    public void setRecurrenceMultiplier(int recurrenceMultiplier) {
        this.recurrenceMultiplier = recurrenceMultiplier;
    }

    @Override
    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    @NonNull
    @Override
    public String getNote() {
        return note;
    }

    public void setNote(@NonNull String note) {
        this.note = note;
    }

    @Override
    public long getCarId() {
        return carId;
    }

    public void setCarId(long carId) {
        this.carId = carId;
    }
}
