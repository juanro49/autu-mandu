package me.kuehle.carreport.model;

import java.util.Date;

import androidx.annotation.NonNull;

public interface ICar {
    Long getId();

    /**
     * @return Color of the car in android color representation.
     */
    int getColor();

    /**
     * @return Initial mileage of the car, when it starts to be used in the app.
     */
    int getInitialMileage();

    /**
     * @return Name of the car. Only for display purposes.
     */
    @NonNull
    String getName();

    /**
     * @return When the car has been suspended, this contains the start date.
     */
    Date getSuspension();
}
