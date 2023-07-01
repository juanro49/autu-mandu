package org.juanro.autumandu.presentation;

import android.content.Context;
import android.graphics.Color;

import org.juanro.autumandu.model.AutuManduDatabase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.Set;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.platform.app.InstrumentationRegistry;

import org.juanro.autumandu.model.dao.CarDAO;
import org.juanro.autumandu.model.dao.FuelTypeDAO;
import org.juanro.autumandu.model.dao.OtherCostDAO;
import org.juanro.autumandu.model.dao.RefuelingDAO;
import org.juanro.autumandu.model.entity.Car;
import org.juanro.autumandu.model.entity.FuelType;
import org.juanro.autumandu.model.entity.OtherCost;
import org.juanro.autumandu.model.entity.Refueling;

import static org.junit.Assert.*;

public class CarPresenterTest {

    private AutuManduDatabase mDB;
    private Context mContext;

    private long mGasolineCarId;
    private long mDieselCarId;
    private long mMixedCarId;
    private long mGas91FTId;
    private long mGas95FTId;
    private long mDieselFTId;
    private static final String PECULIARITY = "37404694";
    private static final String DIESEL_CATEGORY = "diesel-" + PECULIARITY;
    private static final String GASOLINE_CATEGORY = "gas-" + PECULIARITY;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mDB = AutuManduDatabase.getInstance(mContext);

        CarDAO carDao = mDB.getCarDao();
        Car car1 = new Car();
        car1.setName("Gasoline car - "+ PECULIARITY);
        car1.setColor(Color.BLUE);
        car1.setInitialMileage(0);
        Car car2 = new Car();
        car2.setName("Diesel car - "+ PECULIARITY);
        car2.setColor(Color.RED);
        car2.setInitialMileage(0);
        Car car3 = new Car();
        car3.setName("Mixed car - "+ PECULIARITY);
        car3.setColor(Color.GREEN);
        car3.setInitialMileage(0);
        long[] carIds = carDao.insert(car1, car2, car3);
        mGasolineCarId = carIds[0];
        mDieselCarId = carIds[1];
        mMixedCarId = carIds[2];

        FuelTypeDAO ftDao = mDB.getFuelTypeDao();
        FuelType ft1 = new FuelType();
        ft1.setName("NoPB 91 - "+ PECULIARITY);
        ft1.setCategory(GASOLINE_CATEGORY);
        FuelType ft2 = new FuelType();
        ft2.setName("NoPB 95"+ PECULIARITY);
        ft2.setCategory(GASOLINE_CATEGORY);
        FuelType ft3 = new FuelType();
        ft3.setName("Diesel EN"+ PECULIARITY);
        ft3.setCategory(DIESEL_CATEGORY);
        long[] ftIds = ftDao.insert(ft1, ft2, ft3);
        mGas91FTId = ftIds[0];
        mGas95FTId = ftIds[1];
        mDieselFTId = ftIds[2];

        RefuelingDAO refDAO = mDB.getRefuelingDao();
        Refueling ref1 = new Refueling();
        ref1.setCarId(mGasolineCarId);
        ref1.setFuelTypeId(mGas91FTId);
        ref1.setDate(new Date(System.currentTimeMillis() - 86400000));
        ref1.setMileage(100);
        ref1.setPrice(11);
        ref1.setVolume(10);
        ref1.setNote(PECULIARITY);
        Refueling ref2 = new Refueling();
        ref2.setCarId(mGasolineCarId);
        ref2.setFuelTypeId(mGas95FTId);
        ref2.setDate(new Date());
        ref2.setMileage(190);
        ref2.setPrice(10);
        ref2.setVolume(9);
        ref2.setNote(PECULIARITY);
        Refueling ref3 = new Refueling();
        ref3.setCarId(mDieselCarId);
        ref3.setFuelTypeId(mDieselFTId);
        ref3.setDate(new Date(System.currentTimeMillis() - 86400000));
        ref3.setMileage(100);
        ref3.setPrice(10);
        ref3.setVolume(10);
        ref3.setNote(PECULIARITY);
        Refueling ref4 = new Refueling();
        ref4.setCarId(mDieselCarId);
        ref4.setFuelTypeId(mDieselFTId);
        ref4.setDate(new Date());
        ref4.setMileage(200);
        ref4.setPrice(10);
        ref4.setVolume(10);
        ref4.setNote(PECULIARITY);
        Refueling ref5 = new Refueling();
        ref5.setCarId(mMixedCarId);
        ref5.setFuelTypeId(mDieselFTId);
        ref5.setDate(new Date());
        ref5.setMileage(95);
        ref5.setPrice(8.8f);
        ref5.setVolume(8);
        ref5.setNote(PECULIARITY);
        Refueling ref6 = new Refueling();
        ref6.setCarId(mMixedCarId);
        ref6.setFuelTypeId(mGas95FTId);
        ref6.setDate(new Date());
        ref6.setMileage(210);
        ref6.setPrice(10);
        ref6.setVolume(10);
        ref6.setNote(PECULIARITY);
        refDAO.insert(ref1, ref2, ref3, ref4, ref5, ref6);
    }

    @After
    public void tearDown() {
        SupportSQLiteDatabase cleanupInstance = mDB.getOpenHelper().getWritableDatabase();
        cleanupInstance.beginTransaction();
        cleanupInstance.execSQL("DELETE FROM car WHERE car__name LIKE ?", new String[]{"%"+PECULIARITY});
        cleanupInstance.execSQL("DELETE FROM fuel_type WHERE fuel_type__name LIKE ?", new String[]{"%"+PECULIARITY});
        cleanupInstance.execSQL("DELETE FROM other_cost WHERE title LIKE ?", new String[]{"%"+PECULIARITY});
        cleanupInstance.execSQL("DELETE FROM refueling WHERE note LIKE ?", new String[]{"%"+PECULIARITY});
        cleanupInstance.setTransactionSuccessful();
        cleanupInstance.endTransaction();
    }

    @Test
    public void getUsedFuelTypeCategories() {
        CarPresenter cp = CarPresenter.getInstance(mContext);

        Set<String> car1FuelTypes = cp.getUsedFuelTypeCategories(mGasolineCarId);
        assertEquals(1, car1FuelTypes.size());
        assertTrue(car1FuelTypes.contains(GASOLINE_CATEGORY));

        Set<String> car2FuelTypes = cp.getUsedFuelTypeCategories(mDieselCarId);
        assertEquals(1, car2FuelTypes.size());
        assertTrue(car2FuelTypes.contains(DIESEL_CATEGORY));

        Set<String> car3FuelTypes = cp.getUsedFuelTypeCategories(mMixedCarId);
        assertEquals(2, car3FuelTypes.size());
        assertTrue(car3FuelTypes.contains(GASOLINE_CATEGORY));
        assertTrue(car3FuelTypes.contains(DIESEL_CATEGORY));

        RefuelingDAO refDAO = mDB.getRefuelingDao();
        for (Refueling r: refDAO.getAllForCar(mMixedCarId)) {
            refDAO.delete(r);
        }
        car3FuelTypes = cp.getUsedFuelTypeCategories(mMixedCarId);
        assertEquals(0, car3FuelTypes.size());
    }

    @Test
    public void getLatestMileage() {
        CarPresenter cp = CarPresenter.getInstance(mContext);

        assertEquals(190, cp.getLatestMileage(mGasolineCarId));
        assertEquals(200, cp.getLatestMileage(mDieselCarId));
        assertEquals(210, cp.getLatestMileage(mMixedCarId));

        OtherCostDAO ocDAO = mDB.getOtherCostDao();
        RefuelingDAO refDAO = mDB.getRefuelingDao();

        OtherCost oc1 = new OtherCost();
        oc1.setCarId(mGasolineCarId);
        oc1.setDate(new Date());
        oc1.setMileage(210);
        oc1.setPrice(7);
        oc1.setTitle("wiper_"+PECULIARITY);
        ocDAO.insert(oc1);

        assertEquals(210, cp.getLatestMileage(mGasolineCarId));
        assertEquals(200, cp.getLatestMileage(mDieselCarId));
        assertEquals(210, cp.getLatestMileage(mMixedCarId));

        refDAO.delete(refDAO.getAllForCar(mDieselCarId).get(1));

        assertEquals(210, cp.getLatestMileage(mGasolineCarId));
        assertEquals(100, cp.getLatestMileage(mDieselCarId));
        assertEquals(210, cp.getLatestMileage(mMixedCarId));

        Refueling ref1 = new Refueling();
        ref1.setVolume(4);
        ref1.setPrice(4.5f);
        ref1.setDate(new Date());
        ref1.setMileage(260);
        ref1.setCarId(mMixedCarId);
        ref1.setFuelTypeId(mDieselFTId);
        ref1.setNote(PECULIARITY);
        refDAO.insert(ref1);

        assertEquals(210, cp.getLatestMileage(mGasolineCarId));
        assertEquals(100, cp.getLatestMileage(mDieselCarId));
        assertEquals(260, cp.getLatestMileage(mMixedCarId));
    }
}
