package org.juanro.autumandu.model;

import android.content.Context;
import android.graphics.Color;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
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
public class DataRoundTripTest {
    private Context context;
    private AutuManduDatabase db;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        db = AutuManduDatabase.getInstance(context);
        db.runInTransaction(() -> {
            db.getTireDao().deleteAllTireUsages();
            db.getTireDao().deleteAllTireLists();
            db.getReminderDao().deleteAll();
            db.getRefuelingDao().deleteAll();
            db.getOtherCostDao().deleteAll();
            db.getStationDao().deleteAll();
            db.getFuelTypeDao().deleteAll();
            db.getCarDao().deleteAll();
        });
    }

    @After
    public void tearDown() {
        // En lugar de cerrar directamente, reseteamos la instancia para evitar conflictos entre tests
        AutuManduDatabase.resetInstance();
    }

    @Test
    public void testBalancedRefuelingIntegrity() {
        // 1. Prepare data for a car with multiple refuelings
        final long[] carId = new long[1];
        final long[] fuelTypeId = new long[1];
        final long[] stationId = new long[1];

        db.runInTransaction(() -> {
            Car car = new Car();
            car.setName("Integrity Test Car");
            car.setColor(Color.BLUE);
            car.setInitialMileage(10000);
            car.setBuyingPrice(15000.0);
            car.setNumTires(4);
            carId[0] = db.getCarDao().insert(car)[0];

            FuelType fuelType = new FuelType();
            fuelType.setName("Diesel");
            fuelType.setCategory("Diesel");
            fuelTypeId[0] = db.getFuelTypeDao().insert(fuelType)[0];

            Station station = new Station();
            station.setName("Shell");
            stationId[0] = db.getStationDao().insert(station)[0];

            // Add some refuelings
            // Refueling 1: Full
            Refueling r1 = new Refueling();
            r1.setCarId(carId[0]);
            r1.setFuelTypeId(fuelTypeId[0]);
            r1.setStationId(stationId[0]);
            r1.setDate(new Date(System.currentTimeMillis() - 1000000));
            r1.setMileage(10500);
            r1.setVolume(30.0f);
            r1.setPrice(45.0f);
            r1.setPartial(false);
            r1.setNote("");
            db.getRefuelingDao().insert(r1);

            // Refueling 2: Partial
            Refueling r2 = new Refueling();
            r2.setCarId(carId[0]);
            r2.setFuelTypeId(fuelTypeId[0]);
            r2.setStationId(stationId[0]);
            r2.setDate(new Date(System.currentTimeMillis() - 500000));
            r2.setMileage(10800);
            r2.setVolume(15.0f);
            r2.setPrice(22.5f);
            r2.setPartial(true);
            r2.setNote("");
            db.getRefuelingDao().insert(r2);

            // Refueling 3: Full
            Refueling r3 = new Refueling();
            r3.setCarId(carId[0]);
            r3.setFuelTypeId(fuelTypeId[0]);
            r3.setStationId(stationId[0]);
            r3.setDate(new Date());
            r3.setMileage(11100);
            r3.setVolume(20.0f);
            r3.setPrice(30.0f);
            r3.setPartial(false);
            r3.setNote("");
            db.getRefuelingDao().insert(r3);
        });

        // 2. Fetch with details
        List<RefuelingWithDetails> refuelingsWithDetails = db.getRefuelingDao().getWithDetailsForCar(carId[0]);
        assertEquals(3, refuelingsWithDetails.size());

        // 3. Balance
        List<BalancedRefueling> balanced = BalancedRefueling.balance(refuelingsWithDetails, true, true);

        // 4. Verify Balancing Logic
        // In BalancedRefueling, calculateBalancedRefuelings calculates the consumption.
        // It processes them in ascending order of date.
        // Let's find the 11100 mileage entry and check consumption.

        BalancedRefueling target = null;
        for (BalancedRefueling br : balanced) {
            if (br.getMileage() == 11100) {
                target = br;
                break;
            }
        }

        assertNotNull("Refueling with mileage 11100 should be found", target);
        assertNotNull("Consumption should be calculated for 11100", target.getConsumption());
        // (15 + 20) / (11100 - 10500) * 100 = 35 / 600 * 100 = 5.8333333
        assertEquals(5.8333335f, target.getConsumption(), 0.001f);
    }

    @Test
    public void testFullSchemaIntegrity() {
        // Test new fields in v14: buying_price, num_tires, station_id
        db.runInTransaction(() -> {
            Car car = new Car();
            car.setName("Schema V14 Car");
            car.setBuyingPrice(12345.67);
            car.setNumTires(6);
            long carId = db.getCarDao().insert(car)[0];

            Station station = new Station();
            station.setName("V14 Station");
            long stationId = db.getStationDao().insert(station)[0];

            FuelType fuelType = new FuelType();
            fuelType.setName("V14 Fuel");
            fuelType.setCategory("Benzin");
            long fuelTypeId = db.getFuelTypeDao().insert(fuelType)[0];

            Refueling refueling = new Refueling();
            refueling.setCarId(carId);
            refueling.setStationId(stationId);
            refueling.setFuelTypeId(fuelTypeId);
            refueling.setVolume(10f);
            refueling.setDate(new Date());
            refueling.setNote("");
            db.getRefuelingDao().insert(refueling);

            Car fetchedCar = db.getCarDao().getById(carId);
            assertEquals(12345.67, fetchedCar.getBuyingPrice(), 0.001);
            assertEquals(6, fetchedCar.getNumTires());

            List<Refueling> refuelings = db.getRefuelingDao().getByCarId(carId);
            assertEquals(1, refuelings.size());
            assertEquals(stationId, refuelings.get(0).getStationId());
            assertEquals(fuelTypeId, refuelings.get(0).getFuelTypeId());
        });
    }
}
