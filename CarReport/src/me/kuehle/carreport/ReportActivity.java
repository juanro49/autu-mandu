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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import me.kuehle.carreport.reports.AbstractReport;
import me.kuehle.carreport.reports.CostsReport;
import me.kuehle.carreport.reports.FuelConsumptionReport;
import me.kuehle.carreport.reports.FuelPriceReport;
import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.content.Intent;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;

public class ReportActivity extends Activity implements OnMenuItemClickListener {
	private static final int ADD_REFUELING_REQUEST_CODE = 0;
	private static final int ADD_OTHER_REQUEST_CODE = 1;
	private static final int EDIT_REFUELING_REQUEST_CODE = 2;
	private static final int EDIT_OTHER_REQUEST_CODE = 3;
	private static final int PREFERENCES_REQUEST_CODE = 4;

	private AbstractReport mCurrentReport;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.report);

		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this, R.array.reports,
				android.R.layout.simple_spinner_dropdown_item);

		ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
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
			intent.putExtra(EditFragmentActivity.EXTRA_EDIT,
					EditFragmentActivity.EXTRA_EDIT_REFUELING);
			startActivityForResult(intent, ADD_REFUELING_REQUEST_CODE);
			return true;
		case R.id.menu_add_other:
			Intent intent3 = new Intent(this, EditFragmentActivity.class);
			intent3.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
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

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_show_trend:
			mCurrentReport.setShowTrend(!item.isChecked());
			updateReportGraph();
			return true;
		default:
			return false;
		}
	}

	public void showReportOptions(View v) {
		PopupMenu popup = new PopupMenu(this, v);
		popup.inflate(R.menu.report_options);
		popup.setOnMenuItemClickListener(this);

		Menu menu = popup.getMenu();
		MenuItem item = menu.findItem(R.id.menu_show_trend);
		item.setChecked(mCurrentReport.isShowTrend());

		popup.show();
	}

	private void updateReport() {
		ActionBar actionBar = getActionBar();
		int reportIndex = actionBar.getSelectedNavigationIndex();

		switch (reportIndex) {
		case 0:
			mCurrentReport = new FuelConsumptionReport(getApplicationContext());
			break;
		case 1:
			mCurrentReport = new FuelPriceReport(getApplicationContext());
			break;
		case 2:
			mCurrentReport = new CostsReport(getApplicationContext());
			break;
		default:
			return;
		}

		updateReportGraph();

		ListView lstData = (ListView) findViewById(R.id.lstData);
		lstData.setAdapter(new ReportAdapter(mCurrentReport.getData()));
	}
	
	private void updateReportGraph() {
		FrameLayout graph = (FrameLayout) findViewById(R.id.graph);
		if(graph.getChildCount() == 2) {
			graph.removeViewAt(0);
		}
		
		View graphView = mCurrentReport.getGraphView();
		if (graphView == null) {
			graph.setVisibility(View.GONE);
		} else {
			graph.setVisibility(View.VISIBLE);
			graph.addView(graphView, 0);
		}
	}

	private OnNavigationListener navigationListener = new OnNavigationListener() {
		@Override
		public boolean onNavigationItemSelected(int itemPosition, long itemId) {
			updateReport();
			return true;
		}
	};

	private class ReportAdapter extends BaseAdapter {
		private static final int ITEM_VIEW_TYPE_NORMAL = 0;
		private static final int ITEM_VIEW_TYPE_SEPARATOR = 1;
		private static final int ITEM_VIEW_TYPE_COUNT = 2;

		private Object[] items;
		private boolean colorSections;

		public ReportAdapter(
				HashMap<AbstractReport.Section, ArrayList<AbstractReport.Item>> data) {
			ArrayList<Object> items = new ArrayList<Object>();

			ArrayList<AbstractReport.Section> keys = new ArrayList<AbstractReport.Section>(
					data.keySet());
			Collections.sort(keys);
			for (AbstractReport.Section section : keys) {
				if (section != null) {
					items.add(section);
				}
				items.addAll(data.get(section));
			}

			this.items = items.toArray();

			Preferences prefs = new Preferences(ReportActivity.this);
			colorSections = prefs.isColorSections();
		}

		@Override
		public int getCount() {
			return items.length;
		}

		@Override
		public Object getItem(int position) {
			return items[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public int getViewTypeCount() {
			return ITEM_VIEW_TYPE_COUNT;
		}

		@Override
		public int getItemViewType(int position) {
			return (items[position] instanceof AbstractReport.Section) ? ITEM_VIEW_TYPE_SEPARATOR
					: ITEM_VIEW_TYPE_NORMAL;
		}

		@Override
		public boolean isEnabled(int position) {
			return getItemViewType(position) != ITEM_VIEW_TYPE_SEPARATOR;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final int type = getItemViewType(position);

			if (convertView == null) {
				convertView = LayoutInflater
						.from(ReportActivity.this)
						.inflate(
								type == ITEM_VIEW_TYPE_SEPARATOR ? R.layout.separator_list_item
										: android.R.layout.simple_list_item_2,
								parent, false);
			}

			if (type == ITEM_VIEW_TYPE_SEPARATOR) {
				AbstractReport.Section section = (AbstractReport.Section) getItem(position);
				TextView text = (TextView) convertView;
				text.setText(section.getLabel());
				if (colorSections) {
					text.setTextColor(section.getColor());
					GradientDrawable drawableBottom = (GradientDrawable) text
							.getCompoundDrawables()[3];
					drawableBottom.setColorFilter(section.getColor(),
							Mode.SRC_ATOP);
				}
			} else {
				AbstractReport.Item item = (AbstractReport.Item) getItem(position);
				((TextView) convertView.findViewById(android.R.id.text1))
						.setText(item.getLabel());
				((TextView) convertView.findViewById(android.R.id.text2))
						.setText(item.getValue());
			}

			return convertView;
		}
	}
}