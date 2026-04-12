package org.juanro.autumandu.model.entity.helper;

import java.util.Date;
import androidx.room.TypeConverter;

/**
 * Conversores de tipos optimizados para Room.
 * Reemplaza la antigua lógica de mapeo manual de los Content Providers.
 */
public class SQLTypeConverters {

    @TypeConverter
    public static Date toDate(Long timestamp) {
        return timestamp == null ? null : new Date(timestamp);
    }

    @TypeConverter
    public static Long fromDate(Date date) {
        return date == null ? null : date.getTime();
    }

    @TypeConverter
    public static TimeSpanUnit toTimeSpanUnit(Integer numericUnit) {
        return numericUnit == null ? null : TimeSpanUnit.fromId(numericUnit);
    }

    @TypeConverter
    public static Integer fromTimeSpanUnit(TimeSpanUnit unit) {
        return unit == null ? null : unit.getId();
    }

    @TypeConverter
    public static RecurrenceInterval toRecurrenceInterval(Integer numericInterval) {
        return numericInterval == null ? null : RecurrenceInterval.fromId(numericInterval);
    }

    @TypeConverter
    public static Integer fromRecurrenceInterval(RecurrenceInterval interval) {
        return interval == null ? null : interval.getId();
    }
}
