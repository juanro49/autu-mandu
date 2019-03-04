package me.kuehle.carreport.model.entity;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import me.kuehle.carreport.model.IReminder;
import me.kuehle.carreport.model.entity.helper.TimeSpanUnit;

@Entity(tableName = "reminder", foreignKeys = {
        @ForeignKey(
                childColumns = { "car_id" },
                parentColumns = { "_id" },
                entity = Car.class,
                onDelete = ForeignKey.CASCADE
        )
})
public class Reminder implements IReminder {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    private Long id;

    @NonNull
    @ColumnInfo(name = "title")
    private String title = "";

    @ColumnInfo(name = "after_time_span_unit")
    private TimeSpanUnit afterTimeSpanUnit;

    @ColumnInfo(name = "after_time_span_count")
    private Integer afterTimeSpanCount;

    @ColumnInfo(name = "after_distance")
    private Integer afterDistance;

    @NonNull
    @ColumnInfo(name = "start_date")
    private Date startDate = new Date();

    @ColumnInfo(name = "start_mileage")
    private int startMileage;

    @ColumnInfo(name = "notification_dismissed")
    private boolean notificationDismissed;

    @ColumnInfo(name = "snoozed_until")
    private Date snoozedUntil;

    @ColumnInfo(name = "car_id")
    private long carId;

    public Reminder() {}

    @Ignore
    public Reminder(long carId, @NonNull String title, TimeSpanUnit afterTimeSpanUnit,
                    Integer afterTimeSpanCount, Integer afterDistance, @NonNull Date startDate,
                    int startMileage, boolean notificationDismissed, Date snoozedUntil) {
        this.setCarId(carId);
        this.setTitle(title);
        this.setAfterTimeSpanUnit(afterTimeSpanUnit);
        this.setAfterTimeSpanCount(afterTimeSpanCount);
        this.setAfterDistance(afterDistance);
        this.setStartDate(startDate);
        this.setStartMileage(startMileage);
        this.setNotificationDismissed(notificationDismissed);
        this.setSnoozedUntil(snoozedUntil);
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

    @Override
    public TimeSpanUnit getAfterTimeSpanUnit() {
        return afterTimeSpanUnit;
    }

    public void setAfterTimeSpanUnit(TimeSpanUnit afterTimeSpanUnit) {
        this.afterTimeSpanUnit = afterTimeSpanUnit;
    }

    @Override
    public Integer getAfterTimeSpanCount() {
        return afterTimeSpanCount;
    }

    public void setAfterTimeSpanCount(Integer afterTimeSpanCount) {
        this.afterTimeSpanCount = afterTimeSpanCount;
    }

    @Override
    public Integer getAfterDistance() {
        return afterDistance;
    }

    public void setAfterDistance(Integer distance) {
        this.afterDistance = distance;
    }

    @NonNull
    @Override
    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(@NonNull Date startDate) {
        this.startDate = startDate;
    }

    @Override
    public int getStartMileage() {
        return startMileage;
    }

    public void setStartMileage(int startMileage) {
        this.startMileage = startMileage;
    }

    @Override
    public boolean getNotificationDismissed() {
        return notificationDismissed;
    }

    public void setNotificationDismissed(boolean notificationDismissed) {
        this.notificationDismissed = notificationDismissed;
    }

    @Override
    public Date getSnoozedUntil() {
        return snoozedUntil;
    }

    public void setSnoozedUntil(Date snoozedUntil) {
        this.snoozedUntil = snoozedUntil;
    }

    @Override
    public long getCarId() {
        return carId;
    }

    public void setCarId(long carId) {
        this.carId = carId;
    }
}
