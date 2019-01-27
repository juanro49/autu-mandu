package me.kuehle.carreport.model;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;
import me.kuehle.carreport.model.dao.CarDAO;
import me.kuehle.carreport.model.entity.Car;
import me.kuehle.carreport.provider.DataSQLiteOpenHelper;
import me.kuehle.carreport.provider.car.CarContentValues;
import me.kuehle.carreport.provider.car.CarCursor;
import me.kuehle.carreport.provider.car.CarSelection;

import static org.junit.Assert.*;

@SmallTest
public class RoomMigrationTest {
    private static final String TEST_CAR_NAME = "Magirus-7249c1c0";
    private static final int TEST_CAR_INITIAL_MILEAGE = 170000;
    private static final int TEST_CAR_COLOR = Color.CYAN;

    private Context mContext;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
    }

    @After
    public void tearDown() {
        SQLiteDatabase db = DataSQLiteOpenHelper.getInstance(mContext).getWritableDatabase();
        db.execSQL("DELETE FROM car WHERE car__name = ?", new String[] {TEST_CAR_NAME});
        db.close();
    }

    /**
     * Tests whether Room-based modifications are reflected to content provider in normal use.
     */
    @Test
    public void testSyncedDataFromRoom() {
        CarCursor preExistingCar = new CarSelection().name(TEST_CAR_NAME).
                query(mContext.getContentResolver());
        preExistingCar.moveToFirst();
        assertTrue(preExistingCar.isAfterLast());

        CarReportDatabase roomBasedDB = CarReportDatabase.getInstance(mContext);
        Car testCar = new Car();
        testCar.setName(TEST_CAR_NAME);
        testCar.setInitialMileage(TEST_CAR_INITIAL_MILEAGE);
        testCar.setColor(TEST_CAR_COLOR);
        CarDAO cardao = roomBasedDB.getCarDao();
        cardao.insert(testCar);

        CarCursor postCreatedCar = new CarSelection().name(TEST_CAR_NAME).
                query(mContext.getContentResolver());
        postCreatedCar.moveToFirst();
        assertFalse(postCreatedCar.isAfterLast());
        assertEquals(TEST_CAR_NAME, postCreatedCar.getName());
        assertEquals(TEST_CAR_INITIAL_MILEAGE, postCreatedCar.getInitialMileage());
        assertEquals(TEST_CAR_COLOR, postCreatedCar.getColor());
        assertNull(postCreatedCar.getSuspendedSince());
        postCreatedCar.moveToNext();
        assertTrue(postCreatedCar.isAfterLast());
    }

    /**
     * Tests whether a modification via provider reflects to room without reopening the database
     * explicitly.
     */
    @Test
    public void testSyncedDataFromProvider() {
        CarReportDatabase roomBasedDB = CarReportDatabase.getInstance(mContext);
        CarDAO cdao = roomBasedDB.getCarDao();
        for (Car c: cdao.getAll()) {
            assertNotEquals(TEST_CAR_NAME, c.getName());
        }

        new CarContentValues().
                putName(TEST_CAR_NAME).
                putColor(TEST_CAR_COLOR).
                putInitialMileage(TEST_CAR_INITIAL_MILEAGE).insert(mContext.getContentResolver());

        boolean found = false;
        for (Car c: cdao.getAll()) {
            if (c.getName().equals(TEST_CAR_NAME)) {
                assertEquals(TEST_CAR_INITIAL_MILEAGE, c.getInitialMileage());
                assertEquals(TEST_CAR_COLOR, c.getColor());
                assertNull(c.getSuspension());
                found = true;
            }
        }
        assertTrue("Did not find created car by Content provider", found);
    }


}
