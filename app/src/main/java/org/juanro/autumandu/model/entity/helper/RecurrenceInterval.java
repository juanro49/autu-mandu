package org.juanro.autumandu.model.entity.helper;

import androidx.annotation.NonNull;

/**
 * Representa los intervalos de recurrencia para gastos y recordatorios.
 * Optimizada para persistencia en Room usando IDs fijos en lugar de ordinales.
 */
public enum RecurrenceInterval {
    ONCE(0),
    DAY(1),
    MONTH(2),
    QUARTER(3),
    YEAR(4);

    private final int id;

    RecurrenceInterval(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @NonNull
    public static RecurrenceInterval fromId(int id) {
        for (RecurrenceInterval interval : values()) {
            if (interval.id == id) {
                return interval;
            }
        }
        return ONCE;
    }
}
