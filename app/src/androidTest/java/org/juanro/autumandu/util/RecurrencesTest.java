/*
 * Copyright 2017 Jan Kühle
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Test written by Michael Wodniok
 */

package org.juanro.autumandu.util;


import org.junit.Before;
import org.junit.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import androidx.test.filters.SmallTest;
import org.juanro.autumandu.provider.othercost.RecurrenceInterval;

import static org.junit.Assert.*;

@SmallTest
public class RecurrencesTest {
    private DateFormat parser;

    @Before
    public void setUp() {
        parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
    }

    /**
     * A default test to ensure behaviour does not change.
     */
    @Test
    public void testGetRecurrencesBetweenDefaultTimeframe() throws Exception {
        assertEquals(1, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.DAY,
                1,
                parser.parse("2017-10-14T00:00:00+00:00"),
                parser.parse("2017-10-14T00:00:00+00:00")));

        assertEquals(2, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.DAY,
                1,
                parser.parse("2017-10-14T00:00:00+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00")));

        assertEquals(1, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.DAY,
                2,
                parser.parse("2017-10-14T00:00:00+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00")));

        assertEquals(1, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.MONTH,
                1,
                parser.parse("2017-10-14T00:00:00+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00")));

        assertEquals(2, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.MONTH,
                1,
                parser.parse("2017-09-15T00:00:00+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00")));

        assertEquals(2, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.QUARTER,
                1,
                parser.parse("2017-07-15T00:00:00+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00")));

        assertEquals(2, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.YEAR,
                1,
                parser.parse("2015-11-16T00:00:00+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00")));

        assertEquals(0, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.YEAR,
                1,
                parser.parse("2017-10-15T23:59:59+00:00"),
                parser.parse("2015-11-16T00:00:00+00:00")));
    }

    /**
     * This test should behave same as {@link #testGetRecurrencesBetweenDefaultTimeframe()} as the
     * time frame is always extended in front of the start.
     */
    @Test
    public void testGetRecurrencesBetweenWithLongerTimeFrame() throws Exception {
        assertEquals(1, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.DAY,
                1,
                parser.parse("2017-10-14T00:00:00+00:00"),
                parser.parse("2017-10-14T00:00:00+00:00"),
                parser.parse("2017-10-13T23:58:20+00:00"),
                parser.parse("2017-10-14T00:00:00+00:00")));

        assertEquals(2, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.DAY,
                1,
                parser.parse("2017-10-14T00:00:00+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00"),
                parser.parse("2017-10-13T23:58:20+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00")));

        assertEquals(1, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.DAY,
                2,
                parser.parse("2017-10-14T00:00:00+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00"),
                parser.parse("2017-10-13T23:58:20+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00")));

        assertEquals(1, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.MONTH,
                1,
                parser.parse("2017-10-14T00:00:00+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00"),
                parser.parse("2017-10-13T23:58:20+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00")));

        assertEquals(2, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.MONTH,
                1,
                parser.parse("2017-09-15T00:00:00+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00"),
                parser.parse("2017-09-14T23:58:20+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00")));

        assertEquals(2, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.QUARTER,
                1,
                parser.parse("2017-07-15T00:00:00+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00"),
                parser.parse("2017-07-14T23:58:20+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00")));

        assertEquals(2, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.YEAR,
                1,
                parser.parse("2015-11-16T00:00:00+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00"),
                parser.parse("2015-11-15T23:43:20+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00")));

        assertEquals(0, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.YEAR,
                1,
                parser.parse("2017-10-15T23:59:59+00:00"),
                parser.parse("2015-11-16T00:00:00+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00"),
                parser.parse("2015-11-15T23:43:20+00:00")));
    }

    /**
     * This test should behave on other way than
     * {@link #testGetRecurrencesBetweenDefaultTimeframe()} as the time frame is always shortened in
     * front of the start. The positive asserts shall return always 1 recurrence less.
     */
    @Test
    public void testGetRecurrencesBetweenShortenedStart() throws Exception {
        assertEquals(0, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.DAY,
                1,
                parser.parse("2017-10-14T00:00:00+00:00"),
                parser.parse("2017-10-14T00:00:00+00:00"),
                parser.parse("2017-10-14T00:01:40+00:00"),
                parser.parse("2017-10-14T00:00:00+00:00")));

        assertEquals(1, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.DAY,
                1,
                parser.parse("2017-10-14T00:00:00+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00"),
                parser.parse("2017-10-14T00:01:40+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00")));

        assertEquals(0, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.DAY,
                2,
                parser.parse("2017-10-14T00:00:00+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00"),
                parser.parse("2017-10-14T00:01:40+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00")));

        assertEquals(0, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.MONTH,
                1,
                parser.parse("2017-10-14T00:00:00+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00"),
                parser.parse("2017-10-14T00:01:40+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00")));

        assertEquals(1, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.MONTH,
                1,
                parser.parse("2017-08-15T00:00:00+00:00"),
                parser.parse("2017-10-16T23:59:59+00:00"),
                parser.parse("2017-09-15T00:01:40+00:00"),
                parser.parse("2017-10-16T23:59:59+00:00")));

        assertEquals(1, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.MONTH,
                1,
                parser.parse("2017-09-15T00:00:00+00:00"),
                parser.parse("2017-10-16T23:59:59+00:00"),
                parser.parse("2017-09-15T00:01:40+00:00"),
                parser.parse("2017-10-16T23:59:59+00:00")));

        assertEquals(1, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.QUARTER,
                1,
                parser.parse("2017-07-15T00:00:00+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00"),
                parser.parse("2017-07-15T00:01:40+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00")));

        assertEquals(4, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.QUARTER,
                1,
                parser.parse("2016-01-01T00:00:00+00:00"),
                parser.parse("2017-10-16T00:00:00+00:00"),
                parser.parse("2017-01-01T00:00:00+00:00"),
                parser.parse("2017-10-16T00:00:00+00:00")));

        assertEquals(1, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.YEAR,
                1,
                parser.parse("2015-11-16T00:00:00+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00"),
                parser.parse("2015-11-16T00:16:40+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00")));

        // negative assert - should not change.
        assertEquals(0, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.YEAR,
                1,
                parser.parse("2017-10-15T23:59:59+00:00"),
                parser.parse("2015-11-16T00:00:00+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00"),
                parser.parse("2015-11-16T00:16:40+00:00")));
    }

    /**
     * This test should behave different as {@link #testGetRecurrencesBetweenDefaultTimeframe()} as
     * the time frame was shortened in the end.
     */
    @Test
    public void testGetRecurrencesBetweenShortenedEnd() throws Exception {
        assertEquals(0, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.DAY,
                1,
                parser.parse("2017-10-14T00:00:00+00:00"),
                parser.parse("2017-10-14T00:00:00+00:00"),
                parser.parse("2017-10-14T00:00:00+00:00"),
                parser.parse("2017-10-13T23:58:20+00:00")));

        assertEquals(2, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.DAY,
                1,
                parser.parse("2017-10-14T00:00:00+00:00"),
                parser.parse("2017-10-16T00:00:00+00:00"),
                parser.parse("2017-10-14T00:00:00+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00")));

        assertEquals(1, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.DAY,
                2,
                parser.parse("2017-10-14T00:00:00+00:00"),
                parser.parse("2017-10-16T00:00:00+00:00"),
                parser.parse("2017-10-14T00:00:00+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00")));

        assertEquals(1, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.MONTH,
                1,
                parser.parse("2017-10-14T00:00:00+00:00"),
                parser.parse("2017-10-16T00:00:00+00:00"),
                parser.parse("2017-10-14T00:00:00+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00")));

        assertEquals(2, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.MONTH,
                1,
                parser.parse("2017-09-15T00:00:00+00:00"),
                parser.parse("2017-10-16T00:00:00+00:00"),
                parser.parse("2017-09-15T00:00:00+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00")));

        assertEquals(2, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.QUARTER,
                1,
                parser.parse("2017-07-15T00:00:00+00:00"),
                parser.parse("2017-10-16T00:00:00+00:00"),
                parser.parse("2017-07-15T00:00:00+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00")));

        assertEquals(2, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.YEAR,
                1,
                parser.parse("2015-11-16T00:00:00+00:00"),
                parser.parse("2017-10-16T00:00:00+00:00"),
                parser.parse("2015-11-16T00:00:00+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00")));

        assertEquals(0, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.YEAR,
                1,
                parser.parse("2017-10-16T00:00:00+00:00"),
                parser.parse("2015-11-16T00:00:00+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00"),
                parser.parse("2015-11-16T00:00:00+00:00")));
    }

    /**
     * This test test if the time frame behavior works for single events.
     */
    @Test
    public void testGetRecurrencesBetweenSingleEvent() throws Exception {
        assertEquals(1, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.ONCE,
                1,
                parser.parse("2017-10-16T00:00:00+00:00"),
                parser.parse("2017-10-16T00:00:00+00:00"),
                parser.parse("2017-10-16T00:00:00+00:00"),
                parser.parse("2017-10-16T00:00:00+00:00")));

        assertEquals(1, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.ONCE,
                1,
                parser.parse("2017-10-16T00:00:00+00:00"),
                parser.parse("2017-10-16T00:00:00+00:00"),
                parser.parse("2017-10-15T00:00:00+00:00"),
                parser.parse("2017-10-17T00:00:00+00:00")));

        assertEquals(0, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.ONCE,
                1,
                parser.parse("2017-10-16T00:00:00+00:00"),
                parser.parse("2017-10-16T00:00:00+00:00"),
                parser.parse("2017-10-17T00:00:00+00:00"),
                parser.parse("2017-10-18T00:00:00+00:00")));

        assertEquals(0, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.ONCE,
                1,
                parser.parse("2017-10-16T00:00:00+00:00"),
                parser.parse("2017-10-16T00:00:00+00:00"),
                parser.parse("2017-10-14T00:00:00+00:00"),
                parser.parse("2017-10-15T00:00:00+00:00")));
    }

    /**
     * These checks are done with moved timeframe.
     */
    @Test
    public void testGetRecurrencesBetweenMovedTimeframe() throws Exception {
        assertEquals(1, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.DAY,
                2,
                parser.parse("2016-01-01T00:00:00+00:00"),
                parser.parse("2017-10-16T00:00:00+00:00"),
                parser.parse("2016-01-02T01:00:00+00:00"),
                parser.parse("2016-01-03T00:00:00+00:00")));

        assertEquals(2, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.DAY,
                2,
                parser.parse("2015-12-01T00:00:00+00:00"),
                parser.parse("2017-10-16T00:00:00+00:00"),
                parser.parse("2016-01-02T00:00:00+00:00"),
                parser.parse("2016-01-04T00:00:00+00:00")));

        assertEquals(2, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.DAY,
                1,
                parser.parse("2016-01-01T00:00:00+00:00"),
                parser.parse("2017-10-16T00:00:00+00:00"),
                parser.parse("2016-01-02T00:00:00+00:00"),
                parser.parse("2016-01-03T01:00:00+00:00")));

        assertEquals(0, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.DAY,
                1,
                parser.parse("2017-10-14T00:00:00+00:00"),
                parser.parse("2017-10-16T00:00:00+00:00"),
                parser.parse("2017-10-12T00:00:00+00:00"),
                parser.parse("2017-10-13T00:00:00+00:00")));

        assertEquals(0, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.DAY,
                1,
                parser.parse("2017-10-14T00:00:00+00:00"),
                parser.parse("2017-10-16T00:00:00+00:00"),
                parser.parse("2017-10-17T00:00:00+00:00"),
                parser.parse("2017-10-18T00:00:00+00:00")));

        assertEquals(1, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.MONTH,
                2,
                parser.parse("2016-01-01T00:00:00+00:00"),
                parser.parse("2017-10-16T00:00:00+00:00"),
                parser.parse("2016-01-02T01:00:00+00:00"),
                parser.parse("2016-03-02T00:00:00+00:00")));

        assertEquals(2, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.MONTH,
                2,
                parser.parse("2015-01-01T00:00:00+00:00"),
                parser.parse("2017-10-16T00:00:00+00:00"),
                parser.parse("2016-01-01T00:00:00+00:00"),
                parser.parse("2016-03-02T00:00:00+00:00")));

        assertEquals(2, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.MONTH,
                1,
                parser.parse("2016-01-01T00:00:00+00:00"),
                parser.parse("2017-10-16T00:00:00+00:00"),
                parser.parse("2016-01-02T00:00:00+00:00"),
                parser.parse("2016-03-02T01:00:00+00:00")));

        assertEquals(0, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.MONTH,
                1,
                parser.parse("2017-08-16T00:00:00+00:00"),
                parser.parse("2017-10-16T00:00:00+00:00"),
                parser.parse("2017-07-15T00:00:00+00:00"),
                parser.parse("2017-08-15T00:00:00+00:00")));

        assertEquals(0, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.MONTH,
                1,
                parser.parse("2017-08-16T00:00:00+00:00"),
                parser.parse("2017-10-16T00:00:00+00:00"),
                parser.parse("2017-10-17T00:00:00+00:00"),
                parser.parse("2017-11-17T00:00:00+00:00")));

        assertEquals(1, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.QUARTER,
                2,
                parser.parse("2016-01-01T00:00:00+00:00"),
                parser.parse("2017-10-16T00:00:00+00:00"),
                parser.parse("2016-01-02T01:00:00+00:00"),
                parser.parse("2016-07-02T00:00:00+00:00")));

        assertEquals(2, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.QUARTER,
                2,
                parser.parse("2015-01-01T00:00:00+00:00"),
                parser.parse("2017-10-16T00:00:00+00:00"),
                parser.parse("2016-01-01T00:00:00+00:00"),
                parser.parse("2016-07-02T00:00:00+00:00")));

        assertEquals(2, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.QUARTER,
                1,
                parser.parse("2016-01-01T00:00:00+00:00"),
                parser.parse("2017-10-16T00:00:00+00:00"),
                parser.parse("2016-01-02T00:00:00+00:00"),
                parser.parse("2016-09-02T01:00:00+00:00")));

        assertEquals(0, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.QUARTER,
                1,
                parser.parse("2017-04-16T00:00:00+00:00"),
                parser.parse("2017-10-16T00:00:00+00:00"),
                parser.parse("2017-01-01T00:00:00+00:00"),
                parser.parse("2017-04-01T00:00:00+00:00")));

        assertEquals(0, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.QUARTER,
                1,
                parser.parse("2017-04-16T00:00:00+00:00"),
                parser.parse("2017-10-16T00:00:00+00:00"),
                parser.parse("2017-10-17T00:00:00+00:00"),
                parser.parse("2018-04-17T00:00:00+00:00")));

        assertEquals(1, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.YEAR,
                2,
                parser.parse("2013-01-01T00:00:00+00:00"),
                parser.parse("2017-10-01T00:00:00+00:00"),
                parser.parse("2014-12-31T01:00:00+00:00"),
                parser.parse("2015-12-31T00:00:00+00:00")));

        assertEquals(2, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.YEAR,
                3,
                parser.parse("2008-01-01T00:00:00+00:00"),
                parser.parse("2020-01-01T00:00:00+00:00"),
                parser.parse("2011-12-31T01:00:00+00:00"),
                parser.parse("2017-12-31T00:00:00+00:00")));

        assertEquals(2, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.YEAR,
                1,
                parser.parse("2013-01-01T00:00:00+00:00"),
                parser.parse("2017-10-01T00:00:00+00:00"),
                parser.parse("2013-01-02T00:00:00+00:00"),
                parser.parse("2015-01-02T01:00:00+00:00")));

        assertEquals(0, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.YEAR,
                1,
                parser.parse("2013-01-01T00:00:00+00:00"),
                parser.parse("2017-10-16T00:00:00+00:00"),
                parser.parse("2011-01-01T00:00:00+00:00"),
                parser.parse("2012-12-20T00:00:00+00:00")));

        assertEquals(0, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.YEAR,
                1,
                parser.parse("2013-01-01T00:00:00+00:00"),
                parser.parse("2017-10-16T00:00:00+00:00"),
                parser.parse("2017-10-17T00:00:00+00:00"),
                parser.parse("2019-10-17T00:00:00+00:00")));
    }
}
