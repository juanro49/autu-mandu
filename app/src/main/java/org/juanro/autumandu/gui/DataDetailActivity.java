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

package org.juanro.autumandu.gui;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;

import org.juanro.autumandu.R;
import org.juanro.autumandu.gui.fragment.AbstractDataDetailFragment;
import org.juanro.autumandu.gui.fragment.DataDetailCarFragment;
import org.juanro.autumandu.gui.fragment.DataDetailOtherFragment;
import org.juanro.autumandu.gui.fragment.DataDetailRefuelingFragment;
import org.juanro.autumandu.gui.fragment.DataDetailReminderFragment;
import org.juanro.autumandu.gui.fragment.DataDetailTireFragment;
import org.juanro.autumandu.gui.fragment.DataDetailTripFragment;
import org.juanro.autumandu.gui.pref.PreferencesActivity;
import org.juanro.autumandu.gui.pref.PreferencesStationsFragment;

/**
 * Activity for displaying and editing data details.
 * Modernized to handle all data types and integrate with Room-based fragments.
 */
public class DataDetailActivity extends AppCompatActivity implements
        AbstractDataDetailFragment.OnItemActionListener {
    public static final String EXTRA_EDIT = "edit";
    public static final String EXTRA_NEW_ID = "new_id";

    public static final int EXTRA_EDIT_REFUELING = 0;
    public static final int EXTRA_EDIT_OTHER = 1;
    public static final int EXTRA_EDIT_CAR = 2;
    public static final int EXTRA_EDIT_REMINDER = 3;
    public static final int EXTRA_EDIT_STATION = 4;
    public static final int EXTRA_EDIT_TIRE = 5;
    public static final int EXTRA_EDIT_TRIP = 6;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, Math.max(systemBars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });

        if (savedInstanceState == null) {
            int edit = getIntent().getIntExtra(EXTRA_EDIT, EXTRA_EDIT_REFUELING);

            // Special case for stations: redirect to PreferencesActivity
            if (edit == EXTRA_EDIT_STATION) {
                Intent intent = new Intent(this, PreferencesActivity.class);
                intent.putExtra(PreferencesActivity.EXTRA_SHOW_FRAGMENT, PreferencesStationsFragment.class.getName());
                intent.putExtra(PreferencesActivity.EXTRA_SHOW_FRAGMENT_TITLE, R.string.pref_title_header_stations);
                startActivity(intent);
                finish();
                return;
            }

            Fragment fragment = createFragment(edit);
            if (fragment != null) {
                fragment.setArguments(getIntent().getExtras());
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.detail, fragment)
                        .commit();
            }
        }
    }

    /**
     * Factory method to create the appropriate fragment for the given edit type.
     */
    @Nullable
    private Fragment createFragment(int edit) {
        return switch (edit) {
            case EXTRA_EDIT_REFUELING -> new DataDetailRefuelingFragment();
            case EXTRA_EDIT_OTHER -> new DataDetailOtherFragment();
            case EXTRA_EDIT_CAR -> new DataDetailCarFragment();
            case EXTRA_EDIT_TIRE -> new DataDetailTireFragment();
            case EXTRA_EDIT_REMINDER -> new DataDetailReminderFragment();
            case EXTRA_EDIT_TRIP -> new DataDetailTripFragment();
            default -> null;
        };
    }

    @Override
    public void onItemSaved(long newId) {
        Intent data = new Intent();
        data.putExtra(EXTRA_NEW_ID, newId);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void onItemCanceled() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public void onItemDeleted() {
        setResult(RESULT_OK);
        finish();
    }
}
