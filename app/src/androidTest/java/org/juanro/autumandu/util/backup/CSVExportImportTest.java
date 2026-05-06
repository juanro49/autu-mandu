package org.juanro.autumandu.util.backup;

import android.content.Context;
import android.graphics.Color;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import org.juanro.autumandu.model.AutuManduDatabase;
import org.juanro.autumandu.model.entity.Car;
import org.juanro.autumandu.model.entity.FuelType;
import org.juanro.autumandu.model.entity.Refueling;
import org.juanro.autumandu.model.entity.Station;
import org.juanro.autumandu.Preferences;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class CSVExportImportTest {
    private Context context;
    private AutuManduDatabase db;
    private File tempDir;
    private String originalBackupPath;
    private Preferences prefs;

    @Before
    public void setUp() throws IOException {
        context = ApplicationProvider.getApplicationContext();
        db = AutuManduDatabase.getInstance(context);

        // Ensure we start with a clean state
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

        prefs = new Preferences(context);
        originalBackupPath = prefs.getBackupPath();

        tempDir = new File(context.getCacheDir(), "csv_export_test_" + System.currentTimeMillis());
        assertTrue(tempDir.mkdirs());
        prefs.setBackupPath(tempDir.getAbsolutePath());
    }

    @After
    public void tearDown() {
        prefs.setBackupPath(originalBackupPath);
        AutuManduDatabase.resetInstance();
        deleteRecursive(tempDir);
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        fileOrDirectory.delete();
    }

    @Test
    public void testExportImportRoundTrip() throws Exception {
        // 1. Prepare data
        final long[] carId = new long[1];
        final long[] fuelTypeId = new long[1];
        final long[] stationId = new long[1];
        final Date date = new Date();

        db.runInTransaction(() -> {
            Car car = new Car();
            car.setName("Test Car");
            car.setColor(Color.RED);
            car.setInitialMileage(100);
            car.setBuyingPrice(20000.0);
            car.setNumTires(4);
            carId[0] = db.getCarDao().insert(car)[0];

            FuelType fuelType = new FuelType();
            fuelType.setName("Super 95");
            fuelType.setCategory("Benzin");
            fuelTypeId[0] = db.getFuelTypeDao().insert(fuelType)[0];

            Station station = new Station();
            station.setName("Test Station");
            stationId[0] = db.getStationDao().insert(station)[0];

            Refueling refueling = new Refueling();
            refueling.setCarId(carId[0]);
            refueling.setFuelTypeId(fuelTypeId[0]);
            refueling.setStationId(stationId[0]);
            refueling.setDate(date);
            refueling.setMileage(1100);
            refueling.setVolume(50.5f);
            refueling.setPrice(75.25f);
            refueling.setPartial(false);
            refueling.setNote("Test Note");
            db.getRefuelingDao().insert(refueling);

            org.juanro.autumandu.model.entity.OtherCost otherCost = new org.juanro.autumandu.model.entity.OtherCost();
            otherCost.setCarId(carId[0]);
            otherCost.setTitle("Service");
            otherCost.setDate(date);
            otherCost.setMileage(1000);
            otherCost.setPrice(150.0f);
            otherCost.setRecurrenceInterval(org.juanro.autumandu.model.entity.helper.RecurrenceInterval.YEAR);
            otherCost.setRecurrenceMultiplier(1);
            db.getOtherCostDao().insert(otherCost);

            org.juanro.autumandu.model.entity.Reminder reminder = new org.juanro.autumandu.model.entity.Reminder();
            reminder.setCarId(carId[0]);
            reminder.setTitle("Check oil");
            reminder.setAfterTimeSpanUnit(org.juanro.autumandu.model.entity.helper.TimeSpanUnit.MONTH);
            reminder.setAfterTimeSpanCount(6);
            reminder.setStartDate(date);
            db.getReminderDao().insert(reminder);

            org.juanro.autumandu.model.entity.TireList tireList = new org.juanro.autumandu.model.entity.TireList();
            tireList.setCarId(carId[0]);
            tireList.setManufacturer("Michelin");
            tireList.setModel("Pilot Sport");
            tireList.setBuyDate(date);
            tireList.setQuantity(4);
            long tireId = db.getTireDao().insert(tireList)[0];

            org.juanro.autumandu.model.entity.TireUsage tireUsage = new org.juanro.autumandu.model.entity.TireUsage();
            tireUsage.setTireId(tireId);
            tireUsage.setDistanceMount(100);
            tireUsage.setDateMount(date);
            db.getTireDao().insert(tireUsage);
        });

        // 2. Export
        CSVExportImport exportImport = new CSVExportImport(context);
        exportImport.init();
        exportImport.export();

        // 3. Clear DB
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
        assertEquals(0, db.getCarDao().getAll().size());

        // 4. Import
        exportImport.importData();

        // 5. Verify data
        List<Car> cars = db.getCarDao().getAll();
        assertEquals(1, cars.size());
        assertEquals("Test Car", cars.get(0).getName());

        List<Refueling> refuelings = db.getRefuelingDao().getAll();
        assertEquals(1, refuelings.size());
        assertEquals(1100, refuelings.get(0).getMileage());

        assertEquals(1, db.getOtherCostDao().getAll().size());
        assertEquals("Service", db.getOtherCostDao().getAll().get(0).getTitle());

        assertEquals(1, db.getReminderDao().getAll().size());
        assertEquals("Check oil", db.getReminderDao().getAll().get(0).getTitle());

        assertEquals(1, db.getTireDao().getAllTireLists().size());
        assertEquals("Michelin", db.getTireDao().getAllTireLists().get(0).getManufacturer());

        assertEquals(1, db.getTireDao().getAllTireUsages().size());
        assertEquals(100, db.getTireDao().getAllTireUsages().get(0).getDistanceMount());
    }
}
