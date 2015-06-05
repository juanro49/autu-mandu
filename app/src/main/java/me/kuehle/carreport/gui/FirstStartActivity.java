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

package me.kuehle.carreport.gui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import me.kuehle.carreport.R;
import me.kuehle.carreport.data.query.CarQueries;

public class FirstStartActivity extends AppCompatActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_start);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (CarQueries.getCount(this) > 0) {
            setResult(RESULT_OK);
            finish();
        }
    }

    public void onCreateCarClick(View v) {
        Intent intent = new Intent(this, DataDetailActivity.class);
        intent.putExtra(DataDetailActivity.EXTRA_EDIT, DataDetailActivity.EXTRA_EDIT_CAR);

        startActivityForResult(intent, 0);
    }

    public void onSetupSyncClick(View v) {
        Intent intent = new Intent(this, PreferencesActivity.class);
        intent.putExtra(PreferencesActivity.EXTRA_SHOW_FRAGMENT,
                PreferencesBackupFragment.class.getName());
        intent.putExtra(PreferencesActivity.EXTRA_SHOW_FRAGMENT_TITLE,
                R.string.pref_title_header_backup);

        startActivityForResult(intent, 0);
    }
}
