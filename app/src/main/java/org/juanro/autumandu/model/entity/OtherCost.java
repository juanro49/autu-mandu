package org.juanro.autumandu.model.entity;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import org.juanro.autumandu.model.entity.helper.RecurrenceInterval;

@Entity(tableName = "other_cost", indices = {
        @Index("car_id")
}, foreignKeys = {
        @ForeignKey(
                childColumns = { "car_id" },
                parentColumns = { "_id" },
                entity = Car.class,
                onDelete = ForeignKey.CASCADE
        )
})
public class OtherCost {
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
    @Nullable
    private Integer mileage;

    @ColumnInfo(name = "price")
    private float price;

    @NonNull
    @ColumnInfo(name = "recurrence_interval")
    private RecurrenceInterval recurrenceInterval = RecurrenceInterval.ONCE;

    @ColumnInfo(name = "recurrence_multiplier")
    private int recurrenceMultiplier;

    @ColumnInfo(name = "end_date")
    @Nullable
    private Date endDate;

    @NonNull
    @ColumnInfo(name = "note")
    private String note = "";

    @ColumnInfo(name = "car_id")
    private long carId;

    public OtherCost() {
    }

    @Ignore
    public OtherCost(@NonNull String title, long carId, @NonNull Date date, @Nullable Integer mileage,
                     float price, @NonNull RecurrenceInterval recurrenceInterval,
                     int recurrenceMultiplier, @Nullable Date endDate, @NonNull String note) {
        this.title = title;
        this.carId = carId;
        this.date = date;
        this.mileage = mileage;
        this.price = price;
        this.recurrenceInterval = recurrenceInterval;
        this.recurrenceMultiplier = recurrenceMultiplier;
        this.endDate = endDate;
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    public void setTitle(@NonNull String title) {
        this.title = title;
    }

    @NonNull
    public Date getDate() {
        return date;
    }

    public void setDate(@NonNull Date date) {
        this.date = date;
    }

    @Nullable
    public Integer getMileage() {
        return mileage;
    }

    public void setMileage(@Nullable Integer mileage) {
        this.mileage = mileage;
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    @NonNull
    public RecurrenceInterval getRecurrenceInterval() {
        return recurrenceInterval;
    }

    public void setRecurrenceInterval(@NonNull RecurrenceInterval recurrenceInterval) {
        this.recurrenceInterval = recurrenceInterval;
    }

    public int getRecurrenceMultiplier() {
        return recurrenceMultiplier;
    }

    public void setRecurrenceMultiplier(int recurrenceMultiplier) {
        this.recurrenceMultiplier = recurrenceMultiplier;
    }

    @Nullable
    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(@Nullable Date endDate) {
        this.endDate = endDate;
    }

    @NonNull
    public String getNote() {
        return note;
    }

    public void setNote(@NonNull String note) {
        this.note = note;
    }

    public long getCarId() {
        return carId;
    }

    public void setCarId(long carId) {
        this.carId = carId;
    }
}
