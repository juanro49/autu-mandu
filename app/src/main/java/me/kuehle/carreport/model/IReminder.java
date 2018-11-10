package me.kuehle.carreport.model;

import java.util.Date;

import androidx.annotation.NonNull;
import me.kuehle.carreport.model.entity.helper.TimeSpanUnit;

public interface IReminder {

    Long getId();

    /**
     * @return Display title of the reminder.
     */
    @NonNull
    String getTitle();

    /**
     * @return Time span after which the reminder should go off. Together with
     * {@link #getAfterTimeSpanCount()} this gives a time span like 3 days.
     */
    TimeSpanUnit getAfterTimeSpanUnit();

    /**
     * @return Time span after which the reminder should go off. Together with
     * {@link #getAfterTimeSpanUnit()} this gives a time span like every 3 days.
     */
    Integer getAfterTimeSpanCount();

    /**
     * @return Distance after which the reminder should go off.
     */
    Integer getAfterDistance();

    /**
     * @return Date on which the reminder starts to count.
     */
    @NonNull
    Date getStartDate();

    /**
     * @return Mileage on which the reminder starts to count.
     */
    int getStartMileage();

    /**
     * @return Indicates if the reminder has gone off, but the notification has been dismissed.
     */
    boolean getNotificationDismissed();

    /**
     * @return When the reminder goes off, the user can snooze it. In this case the field contains
     * the date on which the reminder will go off again.
     */
    Date getSnoozedUntil();

    /**
     * @return The car this reminder belongs to.
     */
    long getCarId();

}
