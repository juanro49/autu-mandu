package org.juanro.autumandu.model;

import androidx.annotation.NonNull;

public interface IFuelType {
    Long getId();

    /**
     * @return Name of the fuel type, e.g. Diesel.
     */
    @NonNull
    String getName();

    /**
     * An optional category like fuel or gas. Fuel types may be grouped by this category in reports.
     * @return A category name.
     */
    String getCategory();
}
