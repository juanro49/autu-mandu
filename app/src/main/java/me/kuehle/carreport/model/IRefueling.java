package me.kuehle.carreport.model;

import java.util.Date;

import androidx.annotation.NonNull;

public interface IRefueling {

    Long getId();

    /**
     * @return Date on which the refueling occurred.
     */
    @NonNull
    Date getDate();

    /**
     * @return Mileage on which the refueling occurred.
     */
    int getMileage();

    /**
     * @return The amount of fuel, that was refilled.
     */
    float getVolume();

    /**
     * @return The price of the refueling.
     */
    float getPrice();

    /**
     * @return Indicates if the tank was filled completely or only partially.
     */
    boolean getPartial();

    /**
     * @return A note for this refueling. Just for display purposes.
     */
    @NonNull
    String getNote();

    /**
     * @return The fuel type of this refueling.
     */
    long getFuelTypeId();

    /**
     * @return The car which has been refueled.
     */
    long getCarId();
}
