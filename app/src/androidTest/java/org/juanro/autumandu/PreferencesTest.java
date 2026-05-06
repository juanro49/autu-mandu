package org.juanro.autumandu;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class PreferencesTest {

    private Preferences preferences;
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        preferences = new Preferences(context);
    }

    @Test
    public void testGetBackupPath_DefaultValue() {
        String expectedPath = context.getFilesDir().getAbsolutePath();
        assertEquals(expectedPath, preferences.getBackupPath());
    }

    @Test
    public void testSetAndGetBackupPath() {
        String testPath = "/test/path";
        preferences.setBackupPath(testPath);
        assertEquals(testPath, preferences.getBackupPath());
    }

    @Test
    public void testGetDefaultBackupPath() {
        String expectedPath = context.getFilesDir().getAbsolutePath();
        assertEquals(expectedPath, preferences.getDefaultBackupPath());
    }

    @Test
    public void testRestoreDefaultBackupPath() {
        preferences.setBackupPath("/some/other/path");
        preferences.restoreDefaultBackupPath();
        assertEquals(context.getFilesDir().getAbsolutePath(), preferences.getBackupPath());
    }
}
