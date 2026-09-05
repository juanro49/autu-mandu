package org.juanro.autumandu.model;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import org.juanro.autumandu.FuelConsumption;
import org.juanro.autumandu.model.entity.*;
import org.juanro.autumandu.model.dto.BalancedRefueling;
import org.juanro.autumandu.model.dto.RefuelingWithDetails;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class DualFuelTest {
    private AutuManduDatabase db;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = AutuManduDatabase.getInstance(context);
        db.runInTransaction(() -> {
            db.getRefuelingDao().deleteAll();
            db.getFuelTypeDao().deleteAll();
            db.getStationDao().deleteAll();
            db.getCarDao().deleteAll();
        });
    }

    @After
    public void tearDown() {
        AutuManduDatabase.resetInstance();
    }

    @Test
    public void testDualFuelSameMileageValidation() {
        final long[] carId = new long[1];
        final long[] fuelTypeId1 = new long[1];
        final long[] fuelTypeId2 = new long[1];
        final long[] stationId = new long[1];

        db.runInTransaction(() -> {
            Car car = new Car();
            car.setName("Bifuel Car");
            carId[0] = db.getCarDao().insert(car)[0];

            FuelType ft1 = new FuelType();
            ft1.setName("Petrol");
            ft1.setCategory("gasoline");
            fuelTypeId1[0] = db.getFuelTypeDao().insert(ft1)[0];

            FuelType ft2 = new FuelType();
            ft2.setName("LPG");
            ft2.setCategory("gas");
            fuelTypeId2[0] = db.getFuelTypeDao().insert(ft2)[0];

            Station station = new Station();
            station.setName("Station");
            stationId[0] = db.getStationDao().insert(station)[0];

            // Add two refuelings at same mileage
            Refueling r1 = new Refueling();
            r1.setCarId(carId[0]);
            r1.setFuelTypeId(fuelTypeId1[0]);
            r1.setStationId(stationId[0]);
            r1.setMileage(10000);
            r1.setVolume(20f);
            r1.setDate(new Date(1000000));
            db.getRefuelingDao().insert(r1);

            Refueling r2 = new Refueling();
            r2.setCarId(carId[0]);
            r2.setFuelTypeId(fuelTypeId2[0]);
            r2.setStationId(stationId[0]);
            r2.setMileage(10000);
            r2.setVolume(30f);
            r2.setDate(new Date(2000000));
            db.getRefuelingDao().insert(r2);
        });

        List<RefuelingWithDetails> input = db.getRefuelingDao().getWithDetailsForCar(carId[0]);
        List<BalancedRefueling> balanced = BalancedRefueling.balance(input, FuelConsumption.Type.VOL_FOR_DIST, false, true);

        assertEquals(2, balanced.size());
        assertTrue("Both refuelings should be valid", balanced.get(0).isValid());
        assertTrue("Both refuelings should be valid", balanced.get(1).isValid());
    }

    @Test
    public void testDualFuelConsumptionCalculation() {
        final long[] carId = new long[1];
        final long[] fuelTypeId1 = new long[1]; // Petrol
        final long[] fuelTypeId2 = new long[1]; // LPG
        final long[] stationId = new long[1];

        db.runInTransaction(() -> {
            Car car = new Car();
            car.setName("Bifuel Car");
            carId[0] = db.getCarDao().insert(car)[0];

            FuelType ft1 = new FuelType();
            ft1.setName("Petrol");
            ft1.setCategory("gasoline");
            fuelTypeId1[0] = db.getFuelTypeDao().insert(ft1)[0];

            FuelType ft2 = new FuelType();
            ft2.setName("LPG");
            ft2.setCategory("gas");
            fuelTypeId2[0] = db.getFuelTypeDao().insert(ft2)[0];

            Station station = new Station();
            station.setName("Station");
            stationId[0] = db.getStationDao().insert(station)[0];

            // Petrol sequence
            addRefueling(carId[0], fuelTypeId1[0], stationId[0], 10000, 10f, 1000);
            addRefueling(carId[0], fuelTypeId1[0], stationId[0], 11000, 15f, 3000);

            // LPG sequence (interleaved)
            addRefueling(carId[0], fuelTypeId2[0], stationId[0], 10000, 20f, 2000);
            addRefueling(carId[0], fuelTypeId2[0], stationId[0], 10500, 25f, 4000);
        });

        List<RefuelingWithDetails> input = db.getRefuelingDao().getWithDetailsForCar(carId[0]);
        List<BalancedRefueling> balanced = BalancedRefueling.balance(input, FuelConsumption.Type.VOL_FOR_DIST, false, true);

        // LPG at 10500 km
        BalancedRefueling lpgLater = balanced.stream()
                .filter(br -> br.getMileage() == 10500 && br.getFuelTypeId() == fuelTypeId2[0])
                .findFirst().orElseThrow();

        // LPG consumption: 25L / (10500 - 10000) * 100 = 5.0 L/100km
        assertNotNull(lpgLater.getConsumption());
        assertEquals(5.0f, lpgLater.getConsumption(), 0.001f);
        assertEquals(Integer.valueOf(500), lpgLater.getMileageDifference());

        // Petrol at 11000 km
        BalancedRefueling petrolLater = balanced.stream()
                .filter(br -> br.getMileage() == 11000 && br.getFuelTypeId() == fuelTypeId1[0])
                .findFirst().orElseThrow();

        // Petrol consumption: 15L / (11000 - 10000) * 100 = 1.5 L/100km
        assertNotNull(petrolLater.getConsumption());
        assertEquals(1.5f, petrolLater.getConsumption(), 0.001f);
        assertEquals(Integer.valueOf(1000), petrolLater.getMileageDifference());
    }

    @Test
    public void testStationVolumeAggregation() {
        final long[] carId = new long[1];
        final long[] fuelTypeId1 = new long[1];
        final long[] fuelTypeId2 = new long[1];
        final long[] stationId = new long[1];

        db.runInTransaction(() -> {
            Car car = new Car();
            car.setName("Bifuel Car");
            carId[0] = db.getCarDao().insert(car)[0];

            FuelType ft1 = new FuelType();
            ft1.setName("Petrol");
            ft1.setCategory("gasoline");
            fuelTypeId1[0] = db.getFuelTypeDao().insert(ft1)[0];

            FuelType ft2 = new FuelType();
            ft2.setName("LPG");
            ft2.setCategory("gas");
            fuelTypeId2[0] = db.getFuelTypeDao().insert(ft2)[0];

            Station station = new Station();
            station.setName("Multi Station");
            stationId[0] = db.getStationDao().insert(station)[0];

            addRefueling(carId[0], fuelTypeId1[0], stationId[0], 10000, 50f, 1000);
            addRefueling(carId[0], fuelTypeId2[0], stationId[0], 10000, 30f, 2000);
        });

        List<org.juanro.autumandu.model.dto.StationCategoryVolume> categoryVolumes =
                db.getStationDao().getStationCategoryVolumesForCar(carId[0]);

        assertEquals(2, categoryVolumes.size());

        long gasolineCount = categoryVolumes.stream().filter(cv -> "gasoline".equals(cv.category())).count();
        long gasCount = categoryVolumes.stream().filter(cv -> "gas".equals(cv.category())).count();

        assertEquals(1, gasolineCount);
        assertEquals(1, gasCount);
    }

    private void addRefueling(long carId, long fuelTypeId, long stationId, int mileage, float volume, long time) {
        Refueling r = new Refueling();
        r.setCarId(carId);
        r.setFuelTypeId(fuelTypeId);
        r.setStationId(stationId);
        r.setMileage(mileage);
        r.setVolume(volume);
        r.setDate(new Date(time));
        r.setPartial(false);
        db.getRefuelingDao().insert(r);
    }
}
