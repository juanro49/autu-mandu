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

import java.util.Date;

import me.kuehle.carreport.provider.othercost.RecurrenceInterval;

import static org.junit.Assert.*;

public class RecurrencesTest extends TestCase {
    /**
     * A default test to ensure behaviour does not change.
     * @throws Exception
     */
    public void testGetRecurrencesBetweenDefaultTimeframe() throws Exception {
        assertEquals(1, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.DAY,
                1,
                new Date(1507939200000L),
                new Date(1507939200000L)));

        assertEquals(2, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.DAY,
                1,
                new Date(1507939200000L),
                new Date(1508111999999L)));

        assertEquals(1, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.DAY,
                2,
                new Date(1507939200000L),
                new Date(1508111999999L)));

        assertEquals(1, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.MONTH,
                1,
                new Date(1507939200000L),
                new Date(1508111999999L)));

        assertEquals(2, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.MONTH,
                1,
                new Date(1505433600000L),
                new Date(1508111999999L)));

        assertEquals(2, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.QUARTER,
                1,
                new Date(1500076800000L),
                new Date(1508111999999L)));

        assertEquals(2, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.YEAR,
                1,
                new Date(1447632000000L),
                new Date(1508111999999L)));

        assertEquals(0, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.YEAR,
                1,
                new Date(1508111999999L),
                new Date(1447632000000L)));
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
                new Date(1507939200000L),
                new Date(1507939200000L),
                new Date(1507939100000L),
                new Date(1507939200000L)));

        assertEquals(2, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.DAY,
                1,
                new Date(1507939200000L),
                new Date(1508111999999L),
                new Date(1507939100000L),
                new Date(1508111999999L)));

        assertEquals(1, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.DAY,
                2,
                new Date(1507939200000L),
                new Date(1508111999999L),
                new Date(1507939100000L),
                new Date(1508111999999L)));

        assertEquals(1, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.MONTH,
                1,
                new Date(1507939200000L),
                new Date(1508111999999L),
                new Date(1507939100000L),
                new Date(1508111999999L)));

        assertEquals(2, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.MONTH,
                1,
                new Date(1505433600000L),
                new Date(1508111999999L),
                new Date(1505433500000L),
                new Date(1508111999999L)));

        assertEquals(2, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.QUARTER,
                1,
                new Date(1500076800000L),
                new Date(1508111999999L),
                new Date(1500076700000L),
                new Date(1508111999999L)));

        assertEquals(2, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.YEAR,
                1,
                new Date(1447632000000L),
                new Date(1508111999999L),
                new Date(1447631000000L),
                new Date(1508111999999L)));

        assertEquals(0, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.YEAR,
                1,
                new Date(1508111999999L),
                new Date(1447632000000L),
                new Date(1508111999999L),
                new Date(1447631000000L)));
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
                new Date(1507939200000L),
                new Date(1507939200000L),
                new Date(1507939300000L),
                new Date(1507939200000L)));

        assertEquals(1, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.DAY,
                1,
                new Date(1507939200000L),
                new Date(1508111999999L),
                new Date(1507939300000L),
                new Date(1508111999999L)));

        assertEquals(0, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.DAY,
                2,
                new Date(1507939200000L),
                new Date(1508111999999L),
                new Date(1507939300000L),
                new Date(1508111999999L)));

        assertEquals(0, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.MONTH,
                1,
                new Date(1507939200000L),
                new Date(1508111999999L),
                new Date(1507939300000L),
                new Date(1508111999999L)));

        assertEquals(1, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.MONTH,
                1,
                new Date(1505433600000L),
                new Date(1508111999999L),
                new Date(1505433700000L),
                new Date(1508111999999L)));

        assertEquals(1, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.QUARTER,
                1,
                new Date(1500076800000L),
                new Date(1508111999999L),
                new Date(1500076900000L),
                new Date(1508111999999L)));

        assertEquals(1, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.YEAR,
                1,
                new Date(1447632000000L),
                new Date(1508111999999L),
                new Date(1447633000000L),
                new Date(1508111999999L)));

        // negative assert - should not change.
        assertEquals(0, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.YEAR,
                1,
                new Date(1508111999999L),
                new Date(1447632000000L),
                new Date(1508111999999L),
                new Date(1447633000000L)));
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
                new Date(1507939200000L),
                new Date(1507939200000L),
                new Date(1507939200000L),
                new Date(1507939100000L)));

        assertEquals(2, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.DAY,
                1,
                new Date(1507939200000L),
                new Date(1508112000000L),
                new Date(1507939200000L),
                new Date(1508111999999L)));

        assertEquals(1, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.DAY,
                2,
                new Date(1507939200000L),
                new Date(1508112000000L),
                new Date(1507939200000L),
                new Date(1508111999999L)));

        assertEquals(1, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.MONTH,
                1,
                new Date(1507939200000L),
                new Date(1508112000000L),
                new Date(1507939200000L),
                new Date(1508111999999L)));

        assertEquals(2, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.MONTH,
                1,
                new Date(1505433600000L),
                new Date(1508112000000L),
                new Date(1505433600000L),
                new Date(1508111999999L)));

        assertEquals(2, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.QUARTER,
                1,
                new Date(1500076800000L),
                new Date(1508112000000L),
                new Date(1500076800000L),
                new Date(1508111999999L)));

        assertEquals(2, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.YEAR,
                1,
                new Date(1447632000000L),
                new Date(1508112000000L),
                new Date(1447632000000L),
                new Date(1508111999999L)));

        assertEquals(0, Recurrences.getRecurrencesBetween(
                RecurrenceInterval.YEAR,
                1,
                new Date(1508112000000L),
                new Date(1447632000000L),
                new Date(1508111999999L),
                new Date(1447632000000L)));
    }
}