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

import me.kuehle.carreport.R;
import android.app.Activity;
import android.os.Bundle;

public class DataDetailActivity extends Activity implements
		AbstractDataDetailFragment.OnItemActionListener {
	public static final String EXTRA_EDIT = "edit";

	public static final int EXTRA_EDIT_REFUELING = 0;
	public static final int EXTRA_EDIT_OTHER = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_data_detail);

		if (savedInstanceState == null) {
			// During initial setup, plug in the details fragment.
			int edit = getIntent()
					.getIntExtra(EXTRA_EDIT, EXTRA_EDIT_REFUELING);
			AbstractDataDetailFragment fragment;
			if (edit == EXTRA_EDIT_REFUELING) {
				fragment = new DataDetailRefuelingFragment();
			} else {
				fragment = new DataDetailOtherFragment();
			}
			fragment.setArguments(getIntent().getExtras());

			getFragmentManager().beginTransaction().add(R.id.detail, fragment)
					.commit();
		}
	}

	@Override
	public void itemSaved() {
		setResult(RESULT_OK);
		finish();
	}

	@Override
	public void itemCanceled() {
		setResult(RESULT_CANCELED);
		finish();
	}

	@Override
	public void itemDeleted() {
		setResult(RESULT_OK);
		finish();
	}
}
