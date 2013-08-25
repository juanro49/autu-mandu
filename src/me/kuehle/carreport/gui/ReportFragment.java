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

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.data.report.AbstractReport;
import me.kuehle.carreport.data.report.CostsReport;
import me.kuehle.carreport.data.report.FuelConsumptionReport;
import me.kuehle.carreport.data.report.FuelPriceReport;
import me.kuehle.carreport.data.report.MileageReport;
import me.kuehle.carreport.gui.MainActivity.DataChangeListener;
import me.kuehle.carreport.gui.util.SectionListAdapter;
import me.kuehle.chartlib.ChartView;
import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;

public class ReportFragment extends Fragment implements
		OnMenuItemClickListener, DataChangeListener {
	private FrameLayout mGraphHolder;
	private ChartView mGraph;
	private ListView mLstData;
	private AbstractReport mCurrentReport;
	private int mCurrentGraphOption;

	private OnNavigationListener navigationListener = new OnNavigationListener() {
		@Override
		public boolean onNavigationItemSelected(int itemPosition, long itemId) {
			updateReport();
			return true;
		}
	};

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				getActivity(), R.array.reports,
				android.R.layout.simple_spinner_dropdown_item);

		ActionBar actionBar = getActivity().getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		actionBar.setListNavigationCallbacks(adapter, navigationListener);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_report, container, false);

		mLstData = (ListView) v.findViewById(R.id.lst_data);
		mGraphHolder = (FrameLayout) v.findViewById(R.id.graph_holder);
		mGraph = (ChartView) v.findViewById(R.id.graph);
		mGraph.setNotEnoughDataView(View.inflate(getActivity(),
				R.layout.chart_not_enough_data, null));

		View btnReportOptions = v.findViewById(R.id.btn_report_options);
		btnReportOptions.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showReportOptions(v);
			}
		});

		return v;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		ActionBar actionBar = getActivity().getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setListNavigationCallbacks(null, null);
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		if (item.getItemId() == R.id.menu_show_trend) {
			mCurrentReport.setShowTrend(!item.isChecked());
			saveGraphSettings();
			updateReportGraph();
			return true;
		} else if (item.getGroupId() == R.id.group_graph) {
			mCurrentGraphOption = item.getOrder();
			saveGraphSettings();
			updateReportGraph();
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void onDataChanged() {
		updateReport();
	}

	public void showReportOptions(View v) {
		PopupMenu popup = new PopupMenu(getActivity(), v);
		popup.inflate(R.menu.report_options);
		popup.setOnMenuItemClickListener(this);

		Menu menu = popup.getMenu();
		MenuItem item = menu.findItem(R.id.menu_show_trend);
		item.setChecked(mCurrentReport.isShowTrend());

		int[] graphOptions = mCurrentReport.getGraphOptions();
		if (graphOptions.length >= 2) {
			for (int i = 0; i < graphOptions.length; i++) {
				item = menu
						.add(R.id.group_graph, Menu.NONE, i, graphOptions[i]);
				item.setChecked(i == mCurrentGraphOption);
			}
			menu.setGroupCheckable(R.id.group_graph, true, true);
		}

		popup.show();
	}

	private void loadGraphSettings() {
		SharedPreferences prefs = getActivity().getPreferences(
				Context.MODE_PRIVATE);
		String reportName = mCurrentReport.getClass().getSimpleName();
		mCurrentReport.setShowTrend(prefs.getBoolean(
				reportName + "_show_trend", false));
		mCurrentGraphOption = prefs.getInt(
				reportName + "_current_graph_option", 0);
		if (mCurrentGraphOption >= mCurrentReport.getGraphOptions().length) {
			mCurrentGraphOption = 0;
		}
	}

	private void saveGraphSettings() {
		SharedPreferences.Editor prefsEdit = getActivity().getPreferences(
				Context.MODE_PRIVATE).edit();
		String reportName = mCurrentReport.getClass().getSimpleName();
		prefsEdit.putBoolean(reportName + "_show_trend",
				mCurrentReport.isShowTrend());
		prefsEdit.putInt(reportName + "_current_graph_option",
				mCurrentGraphOption);
		prefsEdit.apply();
	}

	private void updateReport() {
		ActionBar actionBar = getActivity().getActionBar();
		int reportIndex = actionBar.getSelectedNavigationIndex();

		switch (reportIndex) {
		case 0:
			mCurrentReport = new FuelConsumptionReport(getActivity()
					.getApplicationContext());
			break;
		case 1:
			mCurrentReport = new FuelPriceReport(getActivity()
					.getApplicationContext());
			break;
		case 2:
			mCurrentReport = new MileageReport(getActivity()
					.getApplicationContext());
			break;
		case 3:
			mCurrentReport = new CostsReport(getActivity()
					.getApplicationContext());
			break;
		default:
			return;
		}

		loadGraphSettings();
		updateReportGraph();

		Preferences prefs = new Preferences(getActivity());
		mLstData.setAdapter(new SectionListAdapter(getActivity(),
				R.layout.list_item_report_data, mCurrentReport.getData()
						.getData(), prefs.isColorSections()));
	}

	private void updateReportGraph() {
		mGraph.setChart(mCurrentReport.getChart(mCurrentGraphOption));
		if (mGraph.getChart() == null) {
			mGraphHolder.setVisibility(View.GONE);
		} else {
			mGraphHolder.setVisibility(View.VISIBLE);
		}
	}
}