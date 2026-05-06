package org.juanro.autumandu.data.report;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class CostsReportTest {

    private CostsReport report;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        report = new CostsReport(context);
    }

    @Test
    public void testGetTitle() {
        assertNotNull(report.getTitle());
    }

    @Test
    public void testGetRawChartData_EmptyByDefault() {
        List<AbstractReportChartData> data = report.getRawChartData(0);
        assertNotNull(data);
        assertEquals(0, data.size());
    }

    @Test
    public void testGetAvailableChartOptions() {
        int[] options = report.getAvailableChartOptions();
        assertNotNull(options);
        assertEquals(2, options.length);
    }
}
