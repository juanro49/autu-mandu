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
import android.support.v7.app.ActionBarActivity;
import android.view.View;

import me.kuehle.carreport.R;

public class FirstStartActivity extends ActionBarActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_start);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        setResult(resultCode);
        finish();
    }

    public void startByCreatingCar(View v) {
        Intent intent = new Intent(this, DataDetailActivity.class);
        intent.putExtra(DataDetailActivity.EXTRA_EDIT, DataDetailActivity.EXTRA_EDIT_CAR);
        intent.putExtra(AbstractDataDetailFragment.EXTRA_ALLOW_CANCEL, false);

        startActivityForResult(intent, 0);
    }
}
