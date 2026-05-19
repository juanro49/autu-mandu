/*
 * Copyright 2026 Juanro49
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
 */

package org.juanro.autumandu.model.entity;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TripTest {

    @Test
    public void testGetTotalDistance() {
        Trip trip = new Trip();
        trip.setKmStart(1000);
        trip.setKmEnd(1050);
        assertEquals(Integer.valueOf(50), trip.getTotalDistance());
    }

    @Test
    public void testGetTotalCost() {
        Trip trip = new Trip();
        trip.setFuelCost(45.50);
        trip.setOtherCostsAmount(10.00);
        assertEquals(Double.valueOf(55.50), trip.getTotalCost());
    }

    @Test
    public void testGetTotalCost_Nulls() {
        Trip trip = new Trip();
        trip.setFuelCost(null);
        trip.setOtherCostsAmount(null);
        assertEquals(Double.valueOf(0.0), trip.getTotalCost());

        trip.setFuelCost(20.0);
        assertEquals(Double.valueOf(20.0), trip.getTotalCost());
    }
}
