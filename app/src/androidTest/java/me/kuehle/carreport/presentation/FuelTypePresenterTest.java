package me.kuehle.carreport.presentation;

import android.content.Context;
import android.util.Log;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.platform.app.InstrumentationRegistry;
import me.kuehle.carreport.model.CarReportDatabase;
import me.kuehle.carreport.model.dao.CarDAO;
import me.kuehle.carreport.model.dao.FuelTypeDAO;
import me.kuehle.carreport.model.entity.Car;
import me.kuehle.carreport.model.entity.FuelType;
import me.kuehle.carreport.model.entity.Refueling;

import static org.junit.Assert.*;

public class FuelTypePresenterTest {

    private static final String PECULIARITY = "2edc5803";

    private Context mContext;
    private CarReportDatabase mDB;
    private long mFuelType1Id;
    private long mFuelType2Id;
    private long mFuelType3Id;
    private long mCar1Id;
    private long mCar2Id;


    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mDB = CarReportDatabase.getInstance(mContext);

        CarDAO carDAO = mDB.getCarDao();
        FuelTypeDAO ftDAO = mDB.getFuelTypeDao();

        FuelType ft1 = new FuelType("NoPB 95 - "+PECULIARITY, "Gasoline-"+PECULIARITY);
        FuelType ft2 = new FuelType("NoPB 98 - "+PECULIARITY, "Gasoline-"+PECULIARITY);
        FuelType ft3 = new FuelType("Diesel - "+PECULIARITY, "Diesel-"+PECULIARITY);

        Car car1 = new Car("Gasoline Car - "+PECULIARITY, 0, 0, null);
        Car car2 = new Car("Gasoline Car - "+PECULIARITY, 0, 0, null);

        long[] ftIds = ftDAO.insert(ft1, ft2, ft3);
        mFuelType1Id = ftIds[0];
        mFuelType2Id = ftIds[1];
        mFuelType3Id = ftIds[2];

        long[] carIds = carDAO.insert(car1, car2);
        mCar1Id = carIds[0];
        mCar2Id = carIds[1];
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

    }

    @Test
    public void getMostUsedId() {
        fail("Not implemented yet.");
    }

    @Test
    public void isUsed() {
        fail("Not implemented yet.");
    }
}