package org.juanro.autumandu.model;

import java.util.Date;

import androidx.annotation.NonNull;
import org.juanro.autumandu.model.entity.helper.RecurrenceInterval;

/**
 * A cost for a car, that is not a refueling. Can also be an income, in which case the price is
 * negative.
 */
public interface IOtherCost {
    Long getId();

    /**
     * @return Display title of the cost.
     */
    @NonNull
    String getTitle();

    /**
     * @return Date on which the cost occurred.
     */
    @NonNull
    Date getDate();

    /**
     * @return Mileage on which the cost occurred. May be null.
     */
    Integer getMileage();

    /**
     * @return The price of the cost. If it is an income, the price it negative.
     */
    float getPrice();

    /**
     * @return Recurrence information. Together with the {@link #getRecurrenceMultiplier()} this
     * gives a recurrence like every 5 days.
     */
    @NonNull
    RecurrenceInterval getRecurrenceInterval();

    /**
     * @return Recurrence information. Together with {@link #getRecurrenceInterval()} this gives a
     * recurrence like every 5 days.
     */
    int getRecurrenceMultiplier();

    /**
     * @return Date on which the recurrence ends or null, if there is no known end date yet.
     */
    Date getEndDate();

    /**
     * @return A note for this cost. Just for display purposes.
     */
    @NonNull
    String getNote();

    /**
     * @return The car this cost is assigned to.
     */
    long getCarId();
}
