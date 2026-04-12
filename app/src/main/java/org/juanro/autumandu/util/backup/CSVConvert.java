package org.juanro.autumandu.util.backup;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.Nullable;
import org.juanro.autumandu.model.entity.helper.RecurrenceInterval;
import org.juanro.autumandu.model.entity.helper.TimeSpanUnit;

/**
 * Utilidades para la conversión de datos en exportaciones/importaciones CSV.
 * Optimizada para trabajar con los IDs fijos de Room y evitar dependencias de ordinales.
 */
public final class CSVConvert {
    private static final DateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
    private static final NumberFormat DEFAULT_FLOAT_FORMAT = NumberFormat.getInstance(Locale.US);

    private CSVConvert() {
        // Clase de utilidades
    }

    @Nullable
    static Date toDate(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return DEFAULT_DATE_FORMAT.parse(value);
        } catch (ParseException e) {
            DateFormat fallbackFormat = DateFormat.getDateTimeInstance();
            try {
                return fallbackFormat.parse(value);
            } catch (ParseException e1) {
                return null;
            }
        }
    }

    @Nullable
    static Float toFloat(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            Number number = DEFAULT_FLOAT_FORMAT.parse(value);
            return number != null ? number.floatValue() : null;
        } catch (ParseException e) {
            NumberFormat fallbackFormat = NumberFormat.getInstance();
            try {
                Number number = fallbackFormat.parse(value);
                return number != null ? number.floatValue() : null;
            } catch (ParseException e1) {
                return null;
            }
        }
    }

    @Nullable
    static Integer toInteger(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    static Long toLong(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    static Boolean toBoolean(@Nullable String value) {
        if (value == null || value.isEmpty()) {
            return null;
        } else {
            return Boolean.parseBoolean(value);
        }
    }

    @Nullable
    static RecurrenceInterval toRecurrenceInterval(String value) {
        Integer intValue = toInteger(value);
        return intValue != null ? RecurrenceInterval.fromId(intValue) : null;
    }

    @Nullable
    static TimeSpanUnit toTimeSpanUnit(String value) {
        Integer intValue = toInteger(value);
        return intValue != null ? TimeSpanUnit.fromId(intValue) : null;
    }

    @Nullable
    static String toString(Enum<?> value) {
        if (value == null) return null;
        return switch (value) {
            case RecurrenceInterval ri -> String.valueOf(ri.getId());
            case TimeSpanUnit tsu -> String.valueOf(tsu.getId());
            default -> String.valueOf(value.ordinal());
        };
    }

    @Nullable
    static String toString(Date value) {
        if (value == null) return null;
        try {
            return DEFAULT_DATE_FORMAT.format(value);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    static String toString(Float value) {
        if (value == null) return null;
        try {
            return DEFAULT_FLOAT_FORMAT.format(value);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    static String toString(Double value) {
        if (value == null) return null;
        try {
            return DEFAULT_FLOAT_FORMAT.format(value);
        } catch (Exception e) {
            return null;
        }
    }
}
