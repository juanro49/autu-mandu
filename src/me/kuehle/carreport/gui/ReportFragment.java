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
import me.kuehle.carreport.data.report.AbstractReport.AbstractListItem;
import me.kuehle.carreport.data.report.AbstractReport.Item;
import me.kuehle.carreport.data.report.AbstractReport.Section;
import me.kuehle.carreport.data.report.CostsReport;
import me.kuehle.carreport.data.report.FuelConsumptionReport;
import me.kuehle.carreport.data.report.FuelPriceReport;
import me.kuehle.carreport.data.report.MileageReport;
import me.kuehle.carreport.gui.MainActivity.DataChangeListener;
import me.kuehle.chartlib.ChartView;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.ActionBar;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;

public class ReportFragment extends Fragment implements
		OnMenuItemClickListener, DataChangeListener {
	private AbstractReport[] mReports;
	private AbstractReport mCurrentMenuReport;
	private ViewGroup mCurrentMenuReportView;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		updateReports();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_report, container, false);
	}

	@Override
	public void onDataChanged() {
		updateReports();
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
			mCurrentMenuReport.setShowTrend(!item.isChecked());
			saveGraphSettings(mCurrentMenuReport);
			((ChartView) mCurrentMenuReportView.findViewById(R.id.graph))
					.setChart(mCurrentMenuReport.getChart());
			return true;
		} else if (item.getGroupId() == R.id.group_graph) {
			mCurrentMenuReport.setChartOption(item.getOrder());
			saveGraphSettings(mCurrentMenuReport);
			((ChartView) mCurrentMenuReportView.findViewById(R.id.graph))
					.setChart(mCurrentMenuReport.getChart());
			return true;
		} else {
			return false;
		}
	}

	public void showReportDetails(View v) {
		ViewGroup card = (ViewGroup) v.getParent().getParent().getParent();

		final View main = card.findViewById(R.id.main);
		final View details = card.findViewById(R.id.details);
		final ViewGroup.MarginLayoutParams detailsParams = (ViewGroup.MarginLayoutParams) details
				.getLayoutParams();

		int from = detailsParams.topMargin;
		int to = detailsParams.topMargin == main.getHeight() ? (main
				.getHeight() - details.getHeight()) : main.getHeight();

		ValueAnimator animator = new ValueAnimator();
		animator.setDuration(getResources().getInteger(
				android.R.integer.config_longAnimTime));
		animator.setValues(PropertyValuesHolder.ofInt((String) null, from, to));
		animator.addUpdateListener(new AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				detailsParams.topMargin = (Integer) animation
						.getAnimatedValue();
				details.requestLayout();
			}
		});
		animator.start();
	}

	public void showReportOptions(AbstractReport report, View v) {
		mCurrentMenuReport = report;
		mCurrentMenuReportView = (ViewGroup) v.getParent().getParent()
				.getParent();

		PopupMenu popup = new PopupMenu(getActivity(), v);
		popup.inflate(R.menu.report_options);
		popup.setOnMenuItemClickListener(this);

		Menu menu = popup.getMenu();
		menu.findItem(R.id.menu_show_trend).setChecked(report.isShowTrend());

		int[] graphOptions = report.getAvailableChartOptions();
		if (graphOptions.length >= 2) {
			for (int i = 0; i < graphOptions.length; i++) {
				MenuItem item = menu.add(R.id.group_graph, Menu.NONE, i,
						graphOptions[i]);
				item.setChecked(i == report.getChartOption());
			}
			menu.setGroupCheckable(R.id.group_graph, true, true);
		}

		popup.show();
	}

	private void loadGraphSettings(AbstractReport report) {
		SharedPreferences prefs = getActivity().getPreferences(
				Context.MODE_PRIVATE);
		String reportName = report.getClass().getSimpleName();
		report.setShowTrend(prefs.getBoolean(reportName + "_show_trend", false));
		report.setChartOption(prefs.getInt(
				reportName + "_current_chart_option", 0));
	}

	private void saveGraphSettings(AbstractReport report) {
		SharedPreferences.Editor prefsEdit = getActivity().getPreferences(
				Context.MODE_PRIVATE).edit();
		String reportName = report.getClass().getSimpleName();
		prefsEdit.putBoolean(reportName + "_show_trend", report.isShowTrend());
		prefsEdit.putInt(reportName + "_current_chart_option",
				report.getChartOption());
		prefsEdit.apply();
	}

	private void updateReports() {
		Preferences prefs = new Preferences(getActivity());

		mReports = new AbstractReport[] {
				new FuelConsumptionReport(getActivity()),
				new FuelPriceReport(getActivity()),
				new MileageReport(getActivity()),
				new CostsReport(getActivity()) };

		ViewGroup list1 = (ViewGroup) getView().findViewById(R.id.list1);
		list1.removeAllViews();
		ViewGroup list2 = (ViewGroup) getView().findViewById(R.id.list2);
		if (list2 != null) {
			list2.removeAllViews();
		}

		for (int i = 0; i < mReports.length; i++) {
			final AbstractReport report = mReports[i];
			loadGraphSettings(report);

			ViewGroup list = i % 2 != 0 && list2 != null ? list2 : list1;
			View card = View.inflate(getActivity(), R.layout.report, null);
			list.addView(card);

			((TextView) card.findViewById(R.id.txt_title)).setText(report
					.getTitle());

			View btnReportDetails = card.findViewById(R.id.btn_report_details);
			btnReportDetails.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showReportDetails(v);
				}
			});

			View btnReportOptions = card.findViewById(R.id.btn_report_options);
			btnReportOptions.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showReportOptions(report, v);
				}
			});

			ChartView graph = (ChartView) card.findViewById(R.id.graph);
			graph.setNotEnoughDataView(View.inflate(getActivity(),
					R.layout.chart_not_enough_data, null));
			graph.setChart(report.getChart());

			ViewGroup details = (ViewGroup) card.findViewById(R.id.details);
			for (AbstractListItem item : report.getData(true)) {
				View itemView = View.inflate(getActivity(),
						item instanceof Section ? R.layout.row_section
								: R.layout.row_report_data, null);

				if (item instanceof Section) {
					Section section = (Section) item;
					TextView text = (TextView) itemView;

					text.setText(section.getLabel());
					if (prefs.isColorSections()) {
						text.setTextColor(section.getColor());
						GradientDrawable drawableBottom = (GradientDrawable) text
								.getCompoundDrawables()[3];
						drawableBottom.setColorFilter(section.getColor(),
								PorterDuff.Mode.SRC);
					}
				} else {
					((TextView) itemView.findViewById(android.R.id.text1))
							.setText(item.getLabel());
					((TextView) itemView.findViewById(android.R.id.text2))
							.setText(((Item) item).getValue());
				}

				details.addView(itemView);
			}

			// Set position of details view.
			View main = card.findViewById(R.id.main);
			int widthMeasureSpec = MeasureSpec.makeMeasureSpec(
					ViewGroup.LayoutParams.MATCH_PARENT, MeasureSpec.EXACTLY);
			int heightMeasureSpec = MeasureSpec.makeMeasureSpec(
					ViewGroup.LayoutParams.WRAP_CONTENT, MeasureSpec.EXACTLY);
			main.measure(widthMeasureSpec, heightMeasureSpec);
			details.measure(widthMeasureSpec, heightMeasureSpec);
			((ViewGroup.MarginLayoutParams) details.getLayoutParams()).topMargin = main
					.getMeasuredHeight() - details.getMeasuredHeight();
		}
	}
}