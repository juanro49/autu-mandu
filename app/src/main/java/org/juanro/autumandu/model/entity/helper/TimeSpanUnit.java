package org.juanro.autumandu.model.entity.helper;

import androidx.annotation.NonNull;

/**
 * Representa las unidades de tiempo para los periodos en recordatorios.
 * Optimizada para persistencia en Room usando IDs fijos.
 */
public enum TimeSpanUnit {
    DAY(0),
    MONTH(1),
    YEAR(2);

    private final int id;

    TimeSpanUnit(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @NonNull
    public static TimeSpanUnit fromId(int id) {
        for (TimeSpanUnit unit : values()) {
            if (unit.id == id) {
                return unit;
            }
        }
        return DAY;
    }
}
