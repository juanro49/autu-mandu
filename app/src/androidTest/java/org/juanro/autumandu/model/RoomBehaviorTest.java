package org.juanro.autumandu.model;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;

import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

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
        AutuManduDatabase db = AutuManduDatabase.getInstance(mContext);

        // The following has no real use for the test, but in ensures the database is opened and is
        // at least readable.
        db.getOtherCostDao().getAll();

        db.close();
        assertFalse(db.isOpen());

        // After closing, we should reset the instance to get a new one that is open.
        AutuManduDatabase.resetInstance();
        AutuManduDatabase newDb = AutuManduDatabase.getInstance(mContext);
        assertTrue(newDb.isOpen());

        // Now it should work again.
        newDb.getFuelTypeDao().getAll();
    }
}
