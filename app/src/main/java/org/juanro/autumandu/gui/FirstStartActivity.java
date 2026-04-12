/*
 * Copyright 2013 Jan Kühle
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
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import org.juanro.autumandu.R;
import org.juanro.autumandu.gui.pref.PreferencesActivity;
import org.juanro.autumandu.gui.pref.PreferencesBackupFragment;
import org.juanro.autumandu.viewmodel.MainViewModel;

public class FirstStartActivity extends AppCompatActivity {
    private final ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // The MainViewModel is used here because it already provides a LiveData
                // for the car count, which is exactly what we need to check if a car
                // has been created.
                MainViewModel viewModel = new ViewModelProvider(this).get(MainViewModel.class);
                viewModel.getCarCount().observe(this, count -> {
                    if (count != null && count > 0) {
                        setResult(RESULT_OK);
                        finish();
                    }
                });
            }
    );

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_start);
    }

    public void onCreateCarClick(View v) {
        Intent intent = new Intent(this, DataDetailActivity.class);
        intent.putExtra(DataDetailActivity.EXTRA_EDIT, DataDetailActivity.EXTRA_EDIT_CAR);
        mStartForResult.launch(intent);
    }

    public void onSetupSyncClick(View v) {
        Intent intent = new Intent(this, PreferencesActivity.class);
        intent.putExtra(PreferencesActivity.EXTRA_SHOW_FRAGMENT,
                PreferencesBackupFragment.class.getName());
        intent.putExtra(PreferencesActivity.EXTRA_SHOW_FRAGMENT_TITLE,
                R.string.pref_title_header_backup);
        mStartForResult.launch(intent);
    }
}
