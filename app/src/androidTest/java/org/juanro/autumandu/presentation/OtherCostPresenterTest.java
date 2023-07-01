package org.juanro.autumandu.presentation;

import android.content.Context;
import android.graphics.Color;

import org.juanro.autumandu.model.AutuManduDatabase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.platform.app.InstrumentationRegistry;

import org.juanro.autumandu.model.dao.CarDAO;
import org.juanro.autumandu.model.dao.OtherCostDAO;
import org.juanro.autumandu.model.entity.Car;
import org.juanro.autumandu.model.entity.OtherCost;
import org.juanro.autumandu.model.entity.helper.RecurrenceInterval;

import static org.junit.Assert.*;

public class OtherCostPresenterTest {

    AutuManduDatabase mDB;
    Context mContext;

    Car car1;

    OtherCost otherCost1;
    OtherCost otherCost2;
    OtherCost otherCost3;

    private static final String PECULIARITY = "79c8c5f3";

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mDB = AutuManduDatabase.getInstance(mContext);

        CarDAO carDAO = mDB.getCarDao();
        car1 = new Car("car1-"+PECULIARITY, Color.BLUE, 0, null);
        car1.setId(carDAO.insert(car1)[0]);

        OtherCostDAO ocDAO = mDB.getOtherCostDao();
        otherCost1 = new OtherCost("wipers-"+PECULIARITY, car1.getId(), new Date(), 23, 14.95f, RecurrenceInterval.ONCE, 0, null, "");
        otherCost2 = new OtherCost("shock absorber-"+PECULIARITY, car1.getId(), new Date(), 492, 630, RecurrenceInterval.ONCE, 0, null, "");
        otherCost3 = new OtherCost("bloblicar-"+PECULIARITY, car1.getId(), new Date(), 530, -15, RecurrenceInterval.ONCE, 0, null, "");
        long[] ocIds = ocDAO.insert(otherCost1, otherCost2, otherCost3);
        otherCost1.setId(ocIds[0]);
        otherCost2.setId(ocIds[1]);
        otherCost3.setId(ocIds[2]);
    }

    @After
    public void tearDown() throws Exception {
        SupportSQLiteDatabase clearInstance = mDB.getOpenHelper().getWritableDatabase();
        clearInstance.execSQL("DELETE FROM other_cost WHERE title LIKE ?", new String[]{ "%"+PECULIARITY });
        clearInstance.execSQL("DELETE FROM car WHERE car__name LIKE ?", new String[]{ "%"+PECULIARITY });
        clearInstance.close();
    }

    @Test
    public void getTitles() {
        OtherCostPresenter ocPresenter = OtherCostPresenter.getInstance(mContext);

        Set<String> requiredTitles1 = new HashSet<>();
        requiredTitles1.add("wipers-"+PECULIARITY);
        requiredTitles1.add("shock absorber-"+PECULIARITY);
        for (String availableTitle: ocPresenter.getTitles(true)) {
            requiredTitles1.remove(availableTitle);
        }
        assertEquals(0, requiredTitles1.size());

        Set<String> requiredTitles2 = new HashSet<>();
        requiredTitles2.add("bloblicar-"+PECULIARITY);
        for (String availableTitle: ocPresenter.getTitles(false)) {
            requiredTitles2.remove(availableTitle);
        }
        assertEquals(0, requiredTitles2.size());

        OtherCost oc4 = new OtherCost("tires-"+PECULIARITY, car1.getId(), new Date(), 1200, 360, RecurrenceInterval.ONCE, 0, null, "");
        mDB.getOtherCostDao().insert(oc4);

        Set<String> requiredTitles3 = new HashSet<>();
        requiredTitles3.add("wipers-"+PECULIARITY);
        requiredTitles3.add("shock absorber-"+PECULIARITY);
        requiredTitles3.add("tires-"+PECULIARITY);
        for (String availableTitle: ocPresenter.getTitles(true)) {
            requiredTitles3.remove(availableTitle);
        }
        assertEquals(0, requiredTitles3.size());

        Set<String> requiredTitles4 = new HashSet<>();
        requiredTitles4.add("bloblicar-"+PECULIARITY);
        for (String availableTitle: ocPresenter.getTitles(false)) {
            requiredTitles4.remove(availableTitle);
        }
        assertEquals(0, requiredTitles4.size());
    }
}
