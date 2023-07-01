package org.juanro.autumandu.model;

import androidx.annotation.NonNull;

public interface IStation {
    Long getId();

    /**
     * @return Name of the station, e.g. Iberdoex.
     */
    @NonNull
    String getName();
}
