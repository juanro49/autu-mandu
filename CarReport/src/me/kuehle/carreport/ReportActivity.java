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

package me.kuehle.carreport;

import me.kuehle.carreport.reports.AbstractReport;
import me.kuehle.carreport.reports.CostsReport;
import me.kuehle.carreport.reports.FuelConsumptionReport;
import me.kuehle.carreport.reports.FuelPriceReport;
import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class ReportActivity extends Activity {
	private static final int ADD_REFUELING_REQUEST_CODE = 0;
	private static final int ADD_OTHER_REQUEST_CODE = 1;
	private static final int EDIT_REFUELING_REQUEST_CODE = 2;
	private static final int EDIT_OTHER_REQUEST_CODE = 3;
	private static final int PREFERENCES_REQUEST_CODE = 4;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.report);

		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this, R.array.reports,
				android.R.layout.simple_spinner_dropdown_item);

		ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		actionBar.setListNavigationCallbacks(adapter, navigationListener);

		Object reportId = getLastNonConfigurationInstance();
		if (reportId != null) {
			actionBar.setSelectedNavigationItem((Integer) reportId);
		} else {
			Preferences prefs = new Preferences(this);
			actionBar.setSelectedNavigationItem(prefs.getDefaultReport());
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.report, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_add_refueling:
			Intent intent = new Intent(this, EditFragmentActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
			intent.putExtra(EditFragmentActivity.EXTRA_FINISH_ON_BIG_SCREEN,
					false);
			intent.putExtra(EditFragmentActivity.EXTRA_EDIT,
					EditFragmentActivity.EXTRA_EDIT_REFUELING);
			startActivityForResult(intent, ADD_REFUELING_REQUEST_CODE);
			return true;
		case R.id.menu_add_other:
			Intent intent3 = new Intent(this, EditFragmentActivity.class);
			intent3.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
			intent3.putExtra(EditFragmentActivity.EXTRA_FINISH_ON_BIG_SCREEN,
					false);
			intent3.putExtra(EditFragmentActivity.EXTRA_EDIT,
					EditFragmentActivity.EXTRA_EDIT_OTHER);
			startActivityForResult(intent3, ADD_OTHER_REQUEST_CODE);
			return true;
		case R.id.menu_view_data:
			Intent intent1 = new Intent(this, ViewDataActivity.class);
			startActivityForResult(intent1, EDIT_REFUELING_REQUEST_CODE);
			return true;
		case R.id.menu_settings:
			Intent intent2 = new Intent(this, PreferencesActivity.class);
			startActivityForResult(intent2, PREFERENCES_REQUEST_CODE);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if ((requestCode == ADD_REFUELING_REQUEST_CODE && resultCode == RESULT_OK)
				|| (requestCode == ADD_OTHER_REQUEST_CODE && resultCode == RESULT_OK)
				|| requestCode == EDIT_REFUELING_REQUEST_CODE
				|| requestCode == EDIT_OTHER_REQUEST_CODE
				|| requestCode == PREFERENCES_REQUEST_CODE) {
			updateReport();
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		ActionBar actionBar = getActionBar();
		return actionBar.getSelectedNavigationIndex();
	}

	private void updateReport() {
		ActionBar actionBar = getActionBar();
		int reportIndex = actionBar.getSelectedNavigationIndex();

		AbstractReport report;
		switch (reportIndex) {
		case 0:
			report = new FuelConsumptionReport(getApplicationContext());
			break;
		case 1:
			report = new FuelPriceReport(getApplicationContext());
			break;
		case 2:
			report = new CostsReport(getApplicationContext());
			break;
		default:
			return;
		}

		LinearLayout graph = (LinearLayout) findViewById(R.id.graph);
		graph.removeAllViews();
		View graphView = report.getGraphView();
		if (graphView == null) {
			graph.setVisibility(View.GONE);
		} else {
			graph.setVisibility(View.VISIBLE);
			graph.addView(report.getGraphView());
		}

		View txtData = findViewById(R.id.txtData);
		if (txtData != null) {
			txtData.setVisibility(graph.getVisibility());
		}

		ListView lstData = (ListView) findViewById(R.id.lstData);
		SimpleAdapter adapter = new SimpleAdapter(ReportActivity.this,
				report.getData(), android.R.layout.simple_list_item_2,
				report.getDataKeys(), new int[] { android.R.id.text1,
						android.R.id.text2 });
		lstData.setAdapter(adapter);
	}

	private OnNavigationListener navigationListener = new OnNavigationListener() {
		@Override
		public boolean onNavigationItemSelected(int itemPosition, long itemId) {
			updateReport();
			return true;
		}
	};
}