package org.juanro.autumandu.util;

import static org.junit.Assert.assertEquals;
import org.juanro.autumandu.model.entity.helper.TimeSpanUnit;
import org.junit.Test;
import java.util.Date;
import java.util.Calendar;

public class TimeSpanTest {

    @Test
    public void testToString() {
        TimeSpan timeSpan = new TimeSpan(TimeSpanUnit.DAY, 5);
        assertEquals("5 DAY", timeSpan.toString());
    }

    @Test
    public void testFromString() {
        TimeSpan timeSpan = TimeSpan.fromString("10 MONTH", null);
        assertEquals(TimeSpanUnit.MONTH, timeSpan.unit());
        assertEquals(10, timeSpan.count());
    }

    @Test
    public void testFromStringInvalid() {
        TimeSpan defaultValue = new TimeSpan(TimeSpanUnit.YEAR, 1);
        assertEquals(defaultValue, TimeSpan.fromString("invalid", defaultValue));
        assertEquals(defaultValue, TimeSpan.fromString("10 INVALID", defaultValue));
        assertEquals(defaultValue, TimeSpan.fromString(null, defaultValue));
    }

    @Test
    public void testFromMillis() {
        // 365 days in millis
        long yearMillis = 365L * 24 * 60 * 60 * 1000;
        TimeSpan timeSpan = TimeSpan.fromMillis(yearMillis);
        assertEquals(TimeSpanUnit.YEAR, timeSpan.unit());
        assertEquals(1, timeSpan.count());

        // 30 days in millis
        long monthMillis = 30L * 24 * 60 * 60 * 1000;
        timeSpan = TimeSpan.fromMillis(monthMillis);
        assertEquals(TimeSpanUnit.MONTH, timeSpan.unit());
        assertEquals(1, timeSpan.count());

        // 5 days in millis
        long daysMillis = 5L * 24 * 60 * 60 * 1000;
        timeSpan = TimeSpan.fromMillis(daysMillis);
        assertEquals(TimeSpanUnit.DAY, timeSpan.unit());
        assertEquals(5, timeSpan.count());
    }

    @Test
    public void testAddTo() {
        Calendar cal = Calendar.getInstance();
        cal.set(2023, Calendar.JANUARY, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date start = cal.getTime();

        TimeSpan timeSpan = new TimeSpan(TimeSpanUnit.DAY, 10);
        Date result = timeSpan.addTo(start);

        cal.setTime(result);
        assertEquals(2023, cal.get(Calendar.YEAR));
        assertEquals(Calendar.JANUARY, cal.get(Calendar.MONTH));
        assertEquals(11, cal.get(Calendar.DAY_OF_MONTH));
    }
}
