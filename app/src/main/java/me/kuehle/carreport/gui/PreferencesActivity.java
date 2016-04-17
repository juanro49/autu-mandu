/*
 * Copyright 2012 Jan Kühle
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.kuehle.carreport.gui;

import android.app.ActionBar;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v4.app.ActivityCompat;
import android.view.MenuItem;

import java.util.List;

import me.kuehle.carreport.R;

public class PreferencesActivity extends PreferenceActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preference_headers, target);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return PreferencesGeneralFragment.class.getName().equals(fragmentName)
                || PreferencesCarsFragment.class.getName().equals(fragmentName)
                || PreferencesFuelTypesFragment.class.getName().equals(fragmentName)
                || PreferencesRemindersFragment.class.getName().equals(fragmentName)
                || PreferencesReportOrderFragment.class.getName().equals(fragmentName)
                || PreferencesBackupFragment.class.getName().equals(fragmentName)
                || PreferencesAboutFragment.class.getName().equals(fragmentName);
    }
}
