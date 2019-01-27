package me.kuehle.carreport.model.entity.helper;

import java.util.Date;

import androidx.room.TypeConverter;

public class SQLTypeConverters {

    @TypeConverter
    public static Date toDate(Long timestamp) {
        if (timestamp == null) {
            return null;
        } else {
            return new Date(timestamp);
        }
    }

    @TypeConverter
    public static Long fromDate(Date date) {
        if (date == null) {
            return null;
        } else {
            return date.getTime();
        }
    }

    @TypeConverter
    public static TimeSpanUnit toTimeSpanUnit(Integer numericUnit) {
        if (numericUnit == null) {
            return null;
        } else {
            return TimeSpanUnit.values()[numericUnit];
        }
    }

    @TypeConverter
    public static Integer fromTimeSpanUnit(TimeSpanUnit unit) {
        if (unit == null) {
            return null;
        } else {
            return unit.ordinal();
        }
    }

    @TypeConverter
    public static RecurrenceInterval toRecurrenceInterval(Integer numericInterval) {
        if (numericInterval == null) {
            return null;
        } else {
            return RecurrenceInterval.values()[numericInterval];
        }
    }

    @TypeConverter
    public static Integer fromRecurrenceInterval(RecurrenceInterval interval) {
        if (interval == null) {
            return null;
        } else {
            return interval.ordinal();
        }
    }
}
