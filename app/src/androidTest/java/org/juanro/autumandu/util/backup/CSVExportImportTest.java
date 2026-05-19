package org.juanro.autumandu.util.backup;

import android.content.Context;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.model.AutuManduDatabase;
import org.juanro.autumandu.model.entity.Car;
import org.juanro.autumandu.model.entity.Refueling;
import org.juanro.autumandu.model.entity.Reminder;
import org.juanro.autumandu.model.entity.TireList;
import org.juanro.autumandu.model.entity.TireUsage;
import org.juanro.autumandu.model.entity.helper.TimeSpanUnit;
import org.juanro.autumandu.util.DemoData;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
    public void setUp() {
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
        if (prefs != null) {
            prefs.setBackupPath(originalBackupPath);
        }
        AutuManduDatabase.resetInstance();
        if (tempDir != null) {
            deleteRecursive(tempDir);
        }
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
        if (!fileOrDirectory.delete()) {
            Log.w("CSVTest", "No se pudo borrar: " + fileOrDirectory.getAbsolutePath());
        }
    }

    @Test
    public void testExportImportRoundTrip() throws Exception {
        // 1. Prepare data using DemoData
        DemoData.addDemoDataSync(context);

        // Add a reminder manually as DemoData doesn't include them
        long carId = db.getCarDao().getAll().getFirst().getId();
        Reminder reminder = new Reminder();
        reminder.setCarId(carId);
        reminder.setTitle("Test Reminder");
        reminder.setStartDate(new java.util.Date());
        reminder.setAfterTimeSpanUnit(TimeSpanUnit.MONTH);
        reminder.setAfterTimeSpanCount(1);
        db.getReminderDao().insert(reminder);

        // Add a tire usage manually
        TireList tireList = db.getTireDao().getAllTireLists().getFirst();
        TireUsage tireUsage = new TireUsage();
        tireUsage.setTireId(tireList.getId());
        tireUsage.setDateMount(new java.util.Date());
        tireUsage.setDistanceMount(1000);
        db.getTireDao().insert(tireUsage);

        // 2. Export
        Log.d("CSVTest", "Directorio temporal: " + tempDir.getAbsolutePath());
        CSVExportImport exportImport = new CSVExportImport(context);
        exportImport.init();

        Log.d("CSVTest", "Exportando datos...");
        exportImport.export();

        // 2.1 Verificar que los archivos CSV existen y tienen contenido
        File backupDir = new File(tempDir, "CSV");
        Log.d("CSVTest", "Buscando archivos CSV en: " + backupDir.getAbsolutePath());

        File carCsv = new File(backupDir, "car.csv");
        assertTrue("El archivo car.csv no existe", carCsv.exists());
        assertTrue("El archivo car.csv está vacío", carCsv.length() > 0);

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
        Log.d("CSVTest", "Importando datos...");
        exportImport.importData();

        Log.d("CSVTest", "Verificando datos importados...");

        // 5. Verify data (using some values from DemoData)
        List<Car> cars = db.getCarDao().getAll();
        assertFalse("No se importaron coches", cars.isEmpty());

        List<Refueling> refuelings = db.getRefuelingDao().getAll();
        assertFalse("No se importaron repostajes", refuelings.isEmpty());

        assertFalse("No se importaron otros costos", db.getOtherCostDao().getAll().isEmpty());
        assertFalse("No se importaron recordatorios", db.getReminderDao().getAll().isEmpty());
        assertFalse("No se importaron listas de neumáticos", db.getTireDao().getAllTireLists().isEmpty());
        assertFalse("No se importaron usos de neumáticos", db.getTireDao().getAllTireUsages().isEmpty());
    }
}
