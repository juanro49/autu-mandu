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

package org.juanro.autumandu.viewmodel;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import android.app.Application;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.juanro.autumandu.model.AutuManduDatabase;
import org.juanro.autumandu.model.dao.TripDao;
import org.juanro.autumandu.model.dao.TripPrefabDao;
import org.juanro.autumandu.model.entity.Trip;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class TripViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private Application application;
    @Mock
    private AutuManduDatabase database;
    @Mock
    private TripDao tripDao;
    @Mock
    private TripPrefabDao tripPrefabDao;

    private TripViewModel viewModel;
    private AutoCloseable closeable;

    @Before
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        when(database.getTripDao()).thenReturn(tripDao);
        when(database.getTripPrefabDao()).thenReturn(tripPrefabDao);
        viewModel = new TripViewModel(application, database);
    }

    @After
    public void tearDown() throws Exception {
        if (closeable != null) {
            closeable.close();
        }
    }

    @Test
    public void testValidateTrip_Valid() {
        Trip trip = new Trip();
        trip.setDate(LocalDate.now());
        trip.setTimeStart(LocalTime.of(10, 0));
        trip.setTimeEnd(LocalTime.of(11, 0));
        trip.setRouteTarget("Home to Work");
        trip.setPurpose("Commute");
        trip.setKmStart(100);
        trip.setKmEnd(120);

        List<String> errors = viewModel.validateTrip(trip);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testValidateTrip_InvalidOdometer() {
        Trip trip = new Trip();
        trip.setKmStart(100);
        trip.setKmEnd(90); // End < Start

        List<String> errors = viewModel.validateTrip(trip);
        assertTrue(errors.contains("End km must be greater than start km"));
    }

    @Test
    public void testValidateTrip_MissingFields() {
        Trip trip = new Trip();
        trip.setRouteTarget("");
        trip.setPurpose("");

        List<String> errors = viewModel.validateTrip(trip);
        assertTrue(errors.contains("Route/Target is required"));
        assertTrue(errors.contains("Purpose is required"));
    }

    @Test
    public void testValidateTrip_InvalidTime() {
        Trip trip = new Trip();
        trip.setDate(LocalDate.now());
        trip.setTimeStart(LocalTime.of(11, 0));
        trip.setTimeEnd(LocalTime.of(10, 0)); // End < Start
        trip.setRouteTarget("Home to Work");
        trip.setPurpose("Commute");
        trip.setKmStart(100);
        trip.setKmEnd(120);

        List<String> errors = viewModel.validateTrip(trip);
        assertTrue(errors.contains("End time must be after start time"));
    }
}
