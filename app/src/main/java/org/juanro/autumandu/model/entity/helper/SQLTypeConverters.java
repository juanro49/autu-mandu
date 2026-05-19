package org.juanro.autumandu.model.entity.helper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import androidx.room.TypeConverter;

/**
 * Conversores de tipos optimizados para Room.
 * Reemplaza la antigua lógica de mapeo manual de los Content Providers.
 */
public class SQLTypeConverters {

    private SQLTypeConverters() {
        // Utility class
    }

    @TypeConverter
    public static Date toDate(Long timestamp) {
        return timestamp == null ? null : new Date(timestamp);
    }

    @TypeConverter
    public static Long fromDate(Date date) {
        return date == null ? null : date.getTime();
    }

    @TypeConverter
    public static LocalDate toLocalDate(String dateString) {
        return dateString == null ? null : LocalDate.parse(dateString);
    }

    @TypeConverter
    public static String fromLocalDate(LocalDate date) {
        return date == null ? null : date.toString();
    }

    @TypeConverter
    public static LocalTime toLocalTime(String timeString) {
        return timeString == null ? null : LocalTime.parse(timeString);
    }

    @TypeConverter
    public static String fromLocalTime(LocalTime time) {
        return time == null ? null : time.toString();
    }

    @TypeConverter
    public static LocalDateTime toLocalDateTime(String dateTimeString) {
        return dateTimeString == null ? null : LocalDateTime.parse(dateTimeString);
    }

    @TypeConverter
    public static String fromLocalDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toString();
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
