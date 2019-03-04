package me.kuehle.carreport.model;

import android.content.Context;
import android.util.Log;

import org.junit.Before;
import org.junit.Test;

import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;
import me.kuehle.carreport.model.entity.FuelType;
import me.kuehle.carreport.model.entity.OtherCost;

import static org.junit.Assert.*;

@SmallTest
public class RoomBehaviorTest {
    private Context mContext;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test
    public void testActionsOnClosed() {
        CarReportDatabase db = CarReportDatabase.getInstance(mContext);

        // The following has no real use for the test, but in ensures the database is opened and is
        // at least readable.
        for (OtherCost oc: db.getOtherCostDao().getAll()) {
            Log.d("TestUnusedOutput", "price " + oc.getPrice());
        }

        db.close();
        assertFalse(db.isOpen());

        // Do the same again to test the behaviour on a closed database. Use different data.
        for (FuelType ft: db.getFuelTypeDao().getAll()) {
            Log.d("TestUnusedOutput", "name " + ft.getName());
        }
        assertTrue(db.isOpen());
    }
}
