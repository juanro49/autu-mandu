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
import org.juanro.autumandu.model.entity.helper.TimeSpanUnit;

@Entity(tableName = "reminder", indices = {
        @Index("car_id")
}, foreignKeys = {
        @ForeignKey(
                childColumns = { "car_id" },
                parentColumns = { "_id" },
                entity = Car.class,
                onDelete = ForeignKey.CASCADE
        )
})
public class Reminder {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    private Long id;

    @NonNull
    @ColumnInfo(name = "title")
    private String title = "";

    @ColumnInfo(name = "after_time_span_unit")
    @Nullable
    private TimeSpanUnit afterTimeSpanUnit;

    @ColumnInfo(name = "after_time_span_count")
    @Nullable
    private Integer afterTimeSpanCount;

    @ColumnInfo(name = "after_distance")
    @Nullable
    private Integer afterDistance;

    @NonNull
    @ColumnInfo(name = "start_date")
    private Date startDate = new Date();

    @ColumnInfo(name = "start_mileage")
    private int startMileage;

    @ColumnInfo(name = "notification_dismissed")
    private boolean notificationDismissed;

    @ColumnInfo(name = "snoozed_until")
    @Nullable
    private Date snoozedUntil;

    @ColumnInfo(name = "car_id")
    private long carId;

    public Reminder() {
    }

    @Ignore
    public Reminder(long carId, @NonNull String title, @Nullable TimeSpanUnit afterTimeSpanUnit,
                    @Nullable Integer afterTimeSpanCount, @Nullable Integer afterDistance,
                    @NonNull Date startDate, int startMileage, boolean notificationDismissed,
                    @Nullable Date snoozedUntil) {
        this.carId = carId;
        this.title = title;
        this.afterTimeSpanUnit = afterTimeSpanUnit;
        this.afterTimeSpanCount = afterTimeSpanCount;
        this.afterDistance = afterDistance;
        this.startDate = startDate;
        this.startMileage = startMileage;
        this.notificationDismissed = notificationDismissed;
        this.snoozedUntil = snoozedUntil;
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

    @Nullable
    public TimeSpanUnit getAfterTimeSpanUnit() {
        return afterTimeSpanUnit;
    }

    public void setAfterTimeSpanUnit(@Nullable TimeSpanUnit afterTimeSpanUnit) {
        this.afterTimeSpanUnit = afterTimeSpanUnit;
    }

    @Nullable
    public Integer getAfterTimeSpanCount() {
        return afterTimeSpanCount;
    }

    public void setAfterTimeSpanCount(@Nullable Integer afterTimeSpanCount) {
        this.afterTimeSpanCount = afterTimeSpanCount;
    }

    @Nullable
    public Integer getAfterDistance() {
        return afterDistance;
    }

    public void setAfterDistance(@Nullable Integer distance) {
        this.afterDistance = distance;
    }

    @NonNull
    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(@NonNull Date startDate) {
        this.startDate = startDate;
    }

    public int getStartMileage() {
        return startMileage;
    }

    public void setStartMileage(int startMileage) {
        this.startMileage = startMileage;
    }

    public boolean isNotificationDismissed() {
        return notificationDismissed;
    }

    public void setNotificationDismissed(boolean notificationDismissed) {
        this.notificationDismissed = notificationDismissed;
    }

    @Nullable
    public Date getSnoozedUntil() {
        return snoozedUntil;
    }

    public void setSnoozedUntil(@Nullable Date snoozedUntil) {
        this.snoozedUntil = snoozedUntil;
    }

    public long getCarId() {
        return carId;
    }

    public void setCarId(long carId) {
        this.carId = carId;
    }
}
