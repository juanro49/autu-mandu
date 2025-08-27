package org.juanro.autumandu.presentation;

import android.content.Context;

import org.juanro.autumandu.model.AutuManduDatabase;
import org.juanro.autumandu.model.dao.StationDAO;
import org.juanro.autumandu.model.entity.Station;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.platform.app.InstrumentationRegistry;

import org.juanro.autumandu.model.dao.CarDAO;
import org.juanro.autumandu.model.dao.FuelTypeDAO;
import org.juanro.autumandu.model.dao.RefuelingDAO;
import org.juanro.autumandu.model.entity.Car;
import org.juanro.autumandu.model.entity.FuelType;
import org.juanro.autumandu.model.entity.Refueling;

import static org.junit.Assert.*;

public class FuelTypePresenterTest {

    private static final String PECULIARITY = "2edc5803";

    private Context mContext;
    private AutuManduDatabase mDB;
    private long mFuelType1Id;
    private long mFuelType2Id;
    private long mStation1Id;
    private long mStation2Id;
    private long mFuelType3Id;
    private long mFuelType4Id;
    private long mCar1Id;
    private long mCar2Id;


    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mDB = AutuManduDatabase.getInstance(mContext);

        CarDAO carDAO = mDB.getCarDao();
        FuelTypeDAO ftDAO = mDB.getFuelTypeDao();
        RefuelingDAO refDAO = mDB.getRefuelingDao();
        StationDAO stDAO = mDB.getStationDao();

        FuelType ft1 = new FuelType("NoPB 95 - "+PECULIARITY, "Gasoline-"+PECULIARITY);
        FuelType ft2 = new FuelType("NoPB 98 - "+PECULIARITY, "Gasoline-"+PECULIARITY);
        FuelType ft3 = new FuelType("Diesel - "+PECULIARITY, "Diesel-"+PECULIARITY);
        FuelType ft4 = new FuelType("Gas - "+PECULIARITY, "Diesel-"+PECULIARITY);

        Station st1 = new Station("Iberdoex");
        Station st2 = new Station("Campoex");

        Car car1 = new Car("Gasoline Car - "+PECULIARITY, 0, 0, null, 0, 4);
        Car car2 = new Car("Mixed Car - "+PECULIARITY, 0, 0, null,0, 4);

        long[] ftIds = ftDAO.insert(ft1, ft2, ft3, ft4);
        mFuelType1Id = ftIds[0];
        mFuelType2Id = ftIds[1];
        mFuelType3Id = ftIds[2];
        mFuelType4Id = ftIds[3];

        long[] stIds = stDAO.insert(st1, st2);
        mStation1Id = stIds[1];
        mStation2Id = stIds[2];

        long[] carIds = carDAO.insert(car1, car2);
        mCar1Id = carIds[0];
        mCar2Id = carIds[1];

        Refueling ref1 = new Refueling(mCar1Id, mFuelType1Id, mStation1Id, new Date(), 55, 4, 5.2f, false, "ref1-"+PECULIARITY);
        Refueling ref2 = new Refueling(mCar1Id, mFuelType2Id, mStation2Id, new Date(), 121, 4, 5.8f, false, "ref2-"+PECULIARITY);
        Refueling ref3 = new Refueling(mCar1Id, mFuelType1Id, mStation1Id, new Date(), 180, 6, 9.45f, false, "ref3-"+PECULIARITY);
        Refueling ref4 = new Refueling(mCar2Id, mFuelType3Id, mStation2Id, new Date(), 68, 4, 5.2f, false, "ref4-"+PECULIARITY);
        Refueling ref5 = new Refueling(mCar2Id, mFuelType3Id, mStation1Id, new Date(), 110, 4.2f, 4.6f, false, "ref5"+PECULIARITY);
        Refueling ref6 = new Refueling(mCar2Id, mFuelType2Id, mStation2Id, new Date(), 207, 6, 9.45f, false, "ref6-"+PECULIARITY);
        refDAO.insert(ref1, ref2, ref3, ref4, ref5, ref6);
    }

    @After
    public void tearDown() throws Exception {
        SupportSQLiteDatabase cleanupDB = mDB.getOpenHelper().getWritableDatabase();
        cleanupDB.beginTransaction();
        cleanupDB.execSQL("DELETE FROM fuel_type WHERE fuel_type__name LIKE ?", new String[]{ "%"+PECULIARITY });
        cleanupDB.execSQL("DELETE FROM refueling WHERE note LIKE ?", new String[]{ "%"+PECULIARITY });
        cleanupDB.execSQL("DELETE FROM car WHERE car__name LIKE ?", new String[]{ "%"+PECULIARITY });
        cleanupDB.setTransactionSuccessful();
        cleanupDB.endTransaction();
    }

    @Test
    public void getAllCategories() {
        FuelTypePresenter ft = FuelTypePresenter.getInstance(mContext);

        String[] fuelTypeCategories = ft.getAllCategories();
        assertTrue(fuelTypeCategories.length >= 2);
        ArrayList<String> requierdFtCategories = new ArrayList<>(2);
        requierdFtCategories.add("Gasoline-"+PECULIARITY);
        requierdFtCategories.add("Diesel-"+PECULIARITY);
        for (String category: fuelTypeCategories) {
            requierdFtCategories.remove(category);
        }
        assertEquals(0, requierdFtCategories.size());

        FuelTypeDAO ftDAO = mDB.getFuelTypeDao();
        ftDAO.insert(new FuelType("LPG - "+PECULIARITY, "Gas-"+PECULIARITY));
        String[] fuelTypeCategories2 = ft.getAllCategories();
        assertTrue(fuelTypeCategories2.length >= 3);
        ArrayList<String> requiredFtCategories2 = new ArrayList<>(3);
        requiredFtCategories2.add("Gasoline-"+PECULIARITY);
        requiredFtCategories2.add("Diesel-"+PECULIARITY);
        requiredFtCategories2.add("Gas-"+PECULIARITY);
        for (String category: fuelTypeCategories2) {
            requiredFtCategories2.remove(category);
        }
        assertEquals(0, requiredFtCategories2.size());

    }

    @Test
    public void getMostUsedId() {
        FuelTypePresenter ft = FuelTypePresenter.getInstance(mContext);

        assertEquals(mFuelType1Id, ft.getMostUsedId(mCar1Id));
        assertEquals(mFuelType3Id, ft.getMostUsedId(mCar2Id));

        // following should have no effect on car1 but on car2 (also should not cache.
        mDB.getRefuelingDao().insert(
                new Refueling(mCar2Id, mFuelType2Id, mStation1Id, new Date(), 280, 7, 8.9f, false, "ref7-"+PECULIARITY),
                new Refueling(mCar2Id, mFuelType2Id, mStation1Id, new Date(), 355, 6.8f, 8.2f, false, "ref8-"+PECULIARITY));
        assertEquals(mFuelType1Id, ft.getMostUsedId(mCar1Id));
        assertEquals(mFuelType2Id, ft.getMostUsedId(mCar2Id));
    }

    @Test
    public void isUsed() {
        FuelTypePresenter ft = FuelTypePresenter.getInstance(mContext);

        assertTrue(ft.isUsed(mFuelType1Id));
        assertTrue(ft.isUsed(mFuelType2Id));
        assertTrue(ft.isUsed(mFuelType3Id));
        assertFalse(ft.isUsed(mFuelType4Id));

        // folowing should make Fuel Type 4 be used.
        mDB.getRefuelingDao().insert(
                new Refueling(mCar2Id, mFuelType4Id, mStation1Id, new Date(), 240, 4, 3.2f, false, "ref7-"+PECULIARITY));
        assertTrue(ft.isUsed(mFuelType4Id));
    }
}
