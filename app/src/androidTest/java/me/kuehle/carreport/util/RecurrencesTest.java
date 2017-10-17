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

package me.kuehle.carreport.util;

import junit.framework.TestCase;

import org.junit.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import me.kuehle.carreport.provider.othercost.RecurrenceInterval;

import static org.junit.Assert.*;

public class RecurrencesTest extends TestCase {
    private DateFormat parser;

    public void setUp() {
        parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
    }

    /**
     * A default test to ensure behaviour does not change.
     * @throws Exception
     */
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
     * @throws Exception
     */
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
     * @throws Exception
     */
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
                parser.parse("2017-09-15T00:00:00+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00"),
                parser.parse("2017-09-15T00:01:40+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00")));

        assertEquals(1, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.QUARTER,
                1,
                parser.parse("2017-07-15T00:00:00+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00"),
                parser.parse("2017-07-15T00:01:40+00:00"),
                parser.parse("2017-10-15T23:59:59+00:00")));

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
     * @throws Exception
     */
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
}