package me.kuehle.carreport.util.backup;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import me.kuehle.carreport.provider.othercost.RecurrenceInterval;
import me.kuehle.carreport.provider.reminder.TimeSpanUnit;

public class CSVConvert {
    private static final DateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
    private static final NumberFormat DEFAULT_FLOAT_FORMAT = NumberFormat.getInstance(Locale.US);

    @Nullable
    static Date toDate(String value) {
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
        try {
            return DEFAULT_FLOAT_FORMAT.parse(value).floatValue();
        } catch (ParseException e) {
            NumberFormat fallbackFormat = NumberFormat.getInstance();
            try {
                return fallbackFormat.parse(value).floatValue();
            } catch (ParseException e1) {
                return null;
            }
        }
    }

    @Nullable
    static Integer toInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    static Long toLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    static Boolean toBoolean(@NonNull String value) {
        if (value.isEmpty()) {
            return null;
        } else {
            return Boolean.parseBoolean(value);
        }
    }

    @Nullable
    static RecurrenceInterval toRecurrenceInterval(String value) {
        Integer intValue = toInteger(value);
        if (intValue != null) {
            return RecurrenceInterval.values()[intValue];
        } else {
            return null;
        }
    }

    @Nullable
    static TimeSpanUnit toTimeSpanUnit(String value) {
        Integer intValue = toInteger(value);
        if (intValue != null) {
            return TimeSpanUnit.values()[intValue];
        } else {
            return null;
        }
    }

    @Nullable
    static String toString(Enum value) {
        try {
            return String.valueOf(value.ordinal());
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    static String toString(Date value) {
        try {
            return DEFAULT_DATE_FORMAT.format(value);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    static String toString(Float value) {
        try {
            return DEFAULT_FLOAT_FORMAT.format(value);
        } catch (Exception e) {
            return null;
        }
    }
}
