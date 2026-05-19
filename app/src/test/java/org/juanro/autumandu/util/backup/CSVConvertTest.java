package org.juanro.autumandu.util.backup;

import org.juanro.autumandu.model.entity.helper.RecurrenceInterval;
import org.juanro.autumandu.model.entity.helper.TimeSpanUnit;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.*;

public class CSVConvertTest {

    @Test
    public void testToDate() {
        String dateStr = "2023-05-20T10:30:00.000+0000";
        Date date = CSVConvert.toDate(dateStr);
        assertNotNull(date);

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(date);
        assertEquals(2023, cal.get(Calendar.YEAR));
        assertEquals(Calendar.MAY, cal.get(Calendar.MONTH));
        assertEquals(20, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testToDateInvalid() {
        assertNull(CSVConvert.toDate(null));
        assertNull(CSVConvert.toDate(""));
        assertNull(CSVConvert.toDate("invalid-date"));
    }

    @Test
    public void testToFloat() {
        Float val = CSVConvert.toFloat("123.45");
        assertNotNull(val);
        assertEquals(123.45f, val, 0.001f);
        assertNull(CSVConvert.toFloat(null));
        assertNull(CSVConvert.toFloat("abc"));
    }

    @Test
    public void testToInteger() {
        assertEquals(Integer.valueOf(123), CSVConvert.toInteger("123"));
        assertNull(CSVConvert.toInteger(null));
        assertNull(CSVConvert.toInteger("12.3"));
    }

    @Test
    public void testToLong() {
        assertEquals(Long.valueOf(123456789L), CSVConvert.toLong("123456789"));
        assertNull(CSVConvert.toLong(null));
    }

    @Test
    public void testToBoolean() {
        assertEquals(Boolean.TRUE, CSVConvert.toBoolean("true"));
        assertEquals(Boolean.FALSE, CSVConvert.toBoolean("false"));
        assertNull(CSVConvert.toBoolean(null));
    }

    @Test
    public void testToRecurrenceInterval() {
        assertEquals(RecurrenceInterval.MONTH, CSVConvert.toRecurrenceInterval("2"));
        // fromId defaults to ONCE if not found or 99 is not mapped
        assertEquals(RecurrenceInterval.ONCE, CSVConvert.toRecurrenceInterval("99"));
    }

    @Test
    public void testToTimeSpanUnit() {
        assertEquals(TimeSpanUnit.MONTH, CSVConvert.toTimeSpanUnit("1"));
        // fromId defaults to DAY if not found
        assertEquals(TimeSpanUnit.DAY, CSVConvert.toTimeSpanUnit("99"));
    }

    @Test
    public void testToStringEnum() {
        assertEquals("2", CSVConvert.toString(RecurrenceInterval.MONTH));
        assertEquals("1", CSVConvert.toString(TimeSpanUnit.MONTH));
        assertNull(CSVConvert.toString((Enum<?>) null));
    }

    @Test
    public void testToStringDate() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2023, Calendar.MAY, 20, 10, 30, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();

        String dateStr = CSVConvert.toString(date);
        assertNotNull(dateStr);
        assertTrue(dateStr.contains("2023-05-20"));
    }

    @Test
    public void testToStringFloat() {
        assertEquals("123.45", CSVConvert.toString(123.45f));
        assertNull(CSVConvert.toString((Float) null));
    }

    @Test
    public void testToStringDouble() {
        assertEquals("123.45", CSVConvert.toString(123.45));
        assertNull(CSVConvert.toString((Double) null));
    }
}
