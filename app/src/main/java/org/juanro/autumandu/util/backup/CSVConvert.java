package org.juanro.autumandu.util.backup;

import java.text.NumberFormat;
import java.text.ParseException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
    private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN, Locale.US);

    private static final NumberFormat DEFAULT_FLOAT_FORMAT = NumberFormat.getInstance(Locale.US);

    private CSVConvert() {
        // Clase de utilidades
    }

    @Nullable
    static Date toDate(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            var odt = OffsetDateTime.parse(value, DATE_FORMATTER);
            return Date.from(odt.toInstant());
        } catch (Exception e) {
            try {
                // Fallback to Instant parse for some formats
                return Date.from(Instant.parse(value));
            } catch (Exception e1) {
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
    static Boolean isBoolean(String value) {
        if (value == null || value.isEmpty()) return null;
        return Boolean.parseBoolean(value);
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
            var odt = OffsetDateTime.ofInstant(value.toInstant(), ZoneId.systemDefault());
            return DATE_FORMATTER.format(odt);
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
