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

import java.util.ArrayList;
import java.util.List;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.data.report.AbstractReport;
import me.kuehle.carreport.data.report.AbstractReport.AbstractListItem;
import me.kuehle.carreport.data.report.AbstractReport.Item;
import me.kuehle.carreport.data.report.AbstractReport.Section;
import me.kuehle.carreport.gui.MainActivity.BackPressedListener;
import me.kuehle.carreport.gui.MainActivity.DataChangeListener;
import me.kuehle.chartlib.ChartView;
import me.kuehle.chartlib.chart.Chart;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;

public class ReportFragment extends Fragment implements OnMenuItemClickListener, DataChangeListener,
        BackPressedListener {
	private class ReportUpdateTask extends AsyncTask<Void, Object, Void> {
		private final int[] columnIDs = { R.id.list1, R.id.list2 };
		private List<ViewGroup> columns;
		private int currentColumn;

		private ViewGroup getNextColumn() {
			if (++currentColumn >= columns.size()) {
				currentColumn = 0;
			}

			return columns.get(currentColumn);
		}

		@Override
		protected Void doInBackground(Void... params) {
			List<Class<? extends AbstractReport>> reportClasses = new Preferences(
					getActivity()).getReportOrder();
			for (Class<? extends AbstractReport> reportClass : reportClasses) {
				AbstractReport report = AbstractReport.newInstance(reportClass, getActivity());
				loadGraphSettings(report);
				report.update();
				publishProgress(report, report.getChart(false, false));
			}

			return null;
		}

		@Override
		protected void onPreExecute() {
			currentColumn = -1;
			columns = new ArrayList<>();
			for (int columnId : columnIDs) {
				ViewGroup column = (ViewGroup) getView().findViewById(columnId);
				if (column != null) {
					column.removeAllViews();
					columns.add(column);
				}
			}
		}

		@Override
		protected void onProgressUpdate(Object... values) {
			final AbstractReport report = (AbstractReport) values[0];
			View card = View.inflate(getActivity(), R.layout.report, null);
			getNextColumn().addView(card);

			((TextView) card.findViewById(R.id.txt_title)).setText(report.getTitle());

			View btnReportDetails = card.findViewById(R.id.btn_report_details);
			btnReportDetails.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					toggleReportDetails(v);
				}
			});

			View btnReportOptions = card.findViewById(R.id.btn_report_options);
			btnReportOptions.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showReportOptions(report, v);
				}
			});

			ChartView chart = (ChartView) card.findViewById(R.id.chart);
			chart.setNotEnoughDataView(View.inflate(getActivity(), R.layout.chart_not_enough_data,
                    null));
			chart.setChart((Chart) values[1]);
			chart.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showFullScreenChart(report, v);
				}
			});

			ViewGroup details = (ViewGroup) card.findViewById(R.id.details);
			for (AbstractListItem item : report.getData(true)) {
				View itemView = View.inflate(getActivity(),
						item instanceof Section ? R.layout.row_section
								: R.layout.row_report_data, null);

				if (item instanceof Section) {
					Section section = (Section) item;
					TextView text = (TextView) itemView;

					text.setText(section.getLabel());
                    text.setTextColor(section.getColor());
                    GradientDrawable drawableBottom = (GradientDrawable) text
                            .getCompoundDrawables()[3];
                    drawableBottom.setColorFilter(section.getColor(), PorterDuff.Mode.SRC);
				} else {
					((TextView) itemView.findViewById(android.R.id.text1)).setText(item.getLabel());
					((TextView) itemView.findViewById(android.R.id.text2))
                            .setText(((Item) item).getValue());
				}

				details.addView(itemView);
			}

			ObjectAnimator
					.ofFloat(chart, View.ALPHA, 0f, 1f)
					.setDuration(getResources().getInteger(android.R.integer.config_longAnimTime))
					.start();
		}
	}

	private ReportUpdateTask mCurrentUpdateTask;

	private AbstractReport mCurrentMenuReport;
	private ViewGroup mCurrentMenuReportView;

	private Animator mFullScreenChartAnimator;
	private ChartView mCurrentFullScreenChart;
	private Rect mCurrentFullScreenStartBounds;
	private float mCurrentFullScreenStartScaleX;
	private float mCurrentFullScreenStartScaleY;

	@Override
	public boolean onBackPressed() {
		if (mCurrentFullScreenChart != null) {
			hideFullScreenChart();
			return true;
		}

		return false;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mCurrentUpdateTask = new ReportUpdateTask();
		mCurrentUpdateTask.execute();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_report, container, false);

		View btnCloseFullScreen = v.findViewById(R.id.btn_close_full_screen);
		btnCloseFullScreen.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				hideFullScreenChart();
			}
		});

		return v;
	}

	@Override
	public void onDataChanged() {
		mCurrentUpdateTask.cancel(true);
		mCurrentUpdateTask = new ReportUpdateTask();
		mCurrentUpdateTask.execute();
	}

    @Override
    public void onDetach() {
        super.onDetach();
        mCurrentUpdateTask.cancel(true);
    }

    @Override
	public boolean onMenuItemClick(MenuItem item) {
		if (item.getItemId() == R.id.menu_show_trend) {
			mCurrentMenuReport.setShowTrend(!item.isChecked());
			saveGraphSettings(mCurrentMenuReport);
			((ChartView) mCurrentMenuReportView.findViewById(R.id.chart))
					.setChart(mCurrentMenuReport.getChart(false, false));
			return true;
		} else if (item.getItemId() == R.id.menu_show_overall_trend) {
			mCurrentMenuReport.setShowOverallTrend(!item.isChecked());
			saveGraphSettings(mCurrentMenuReport);
			((ChartView) mCurrentMenuReportView.findViewById(R.id.chart))
					.setChart(mCurrentMenuReport.getChart(false, false));
			return true;
		} else if (item.getGroupId() == R.id.group_graph) {
			mCurrentMenuReport.setChartOption(item.getOrder());
			saveGraphSettings(mCurrentMenuReport);
			((ChartView) mCurrentMenuReportView.findViewById(R.id.chart))
					.setChart(mCurrentMenuReport.getChart(false, false));
			return true;
		} else {
			return false;
		}
	}

	private void hideFullScreenChart() {
		if (mFullScreenChartAnimator != null) {
			mFullScreenChartAnimator.cancel();
		}

		final View chartHolder = getView().findViewById(R.id.full_screen_chart_holder);

		// Animate the four positioning/sizing properties in parallel, back to
		// their original values.
		AnimatorSet set = new AnimatorSet();
		set.play(
				ObjectAnimator.ofFloat(chartHolder, View.X,
						mCurrentFullScreenStartBounds.left))
				.with(ObjectAnimator.ofFloat(chartHolder, View.Y,
						mCurrentFullScreenStartBounds.top))
				.with(ObjectAnimator.ofFloat(chartHolder, View.SCALE_X,
						mCurrentFullScreenStartScaleX))
				.with(ObjectAnimator.ofFloat(chartHolder, View.SCALE_Y,
						mCurrentFullScreenStartScaleY));
		set.setDuration(getResources().getInteger(
				android.R.integer.config_longAnimTime));
		set.setInterpolator(new DecelerateInterpolator());
		set.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				mCurrentFullScreenChart.setVisibility(View.VISIBLE);
				mCurrentFullScreenChart = null;
				chartHolder.setVisibility(View.GONE);
				mFullScreenChartAnimator = null;
			}
		});
		set.start();
		mFullScreenChartAnimator = set;
	}

	private void loadGraphSettings(AbstractReport report) {
		SharedPreferences prefs = getActivity().getSharedPreferences(getClass().getName(),
                Context.MODE_PRIVATE);
		String reportName = report.getClass().getSimpleName();
		report.setShowTrend(prefs.getBoolean(reportName + "_show_trend", false));
		report.setShowOverallTrend(prefs.getBoolean(reportName + "_show_overall_trend", false));
		report.setChartOption(prefs.getInt(reportName + "_current_chart_option", 0));
	}

	private void saveGraphSettings(AbstractReport report) {
		SharedPreferences.Editor prefsEdit = getActivity().getSharedPreferences(
                getClass().getName(), Context.MODE_PRIVATE).edit();
		String reportName = report.getClass().getSimpleName();
		prefsEdit.putBoolean(reportName + "_show_trend", report.isShowTrend());
		prefsEdit.putBoolean(reportName + "_show_overall_trend", report.isShowOverallTrend());
		prefsEdit.putInt(reportName + "_current_chart_option", report.getChartOption());
		prefsEdit.apply();
	}

	private void showFullScreenChart(AbstractReport report, View v) {
		if (mFullScreenChartAnimator != null) {
			mFullScreenChartAnimator.cancel();
		}

		mCurrentFullScreenChart = (ChartView) v;
		View chartHolder = getView().findViewById(R.id.full_screen_chart_holder);
		((ChartView) getView().findViewById(R.id.full_screen_chart))
				.setChart(report.getChart(true, true));

		// Calculate translation start and end point and scales.
		mCurrentFullScreenStartBounds = new Rect();
		final Rect finalBounds = new Rect();
		final Point globalOffset = new Point();

		mCurrentFullScreenChart
				.getGlobalVisibleRect(mCurrentFullScreenStartBounds);
		getView().getGlobalVisibleRect(finalBounds, globalOffset);
		mCurrentFullScreenStartBounds.offset(-globalOffset.x, -globalOffset.y);
		finalBounds.offset(-globalOffset.x, -globalOffset.y);

		mCurrentFullScreenStartScaleX = (float) mCurrentFullScreenStartBounds
				.width() / finalBounds.width();
		mCurrentFullScreenStartScaleY = (float) mCurrentFullScreenStartBounds
				.height() / finalBounds.height();

		// Hide the small chart and show the zoomed-in view. When the animation
		// begins, it will position the zoomed-in view in the place of the small
		// chart.
		mCurrentFullScreenChart.setVisibility(View.INVISIBLE);
		chartHolder.setVisibility(View.VISIBLE);

		// Set the pivot point for SCALE_X and SCALE_Y transformations to the
		// top-left corner of the zoomed-in view (the default is the center of
		// the view).
		chartHolder.setPivotX(0f);
		chartHolder.setPivotY(0f);

		// Construct and run the parallel animation of the four translation and
		// scale properties (X, Y, SCALE_X, and SCALE_Y).
		AnimatorSet set = new AnimatorSet();
		set.play(
				ObjectAnimator.ofFloat(chartHolder, View.X,
						mCurrentFullScreenStartBounds.left, finalBounds.left))
				.with(ObjectAnimator.ofFloat(chartHolder, View.Y,
						mCurrentFullScreenStartBounds.top, finalBounds.top))
				.with(ObjectAnimator.ofFloat(chartHolder, View.SCALE_X,
						mCurrentFullScreenStartScaleX, 1f))
				.with(ObjectAnimator.ofFloat(chartHolder, View.SCALE_Y,
						mCurrentFullScreenStartScaleY, 1f));
		set.setDuration(getResources().getInteger(
				android.R.integer.config_longAnimTime));
		set.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				mFullScreenChartAnimator = null;
			}
		});
		set.start();
		mFullScreenChartAnimator = set;
	}

	private void toggleReportDetails(View v) {
		ViewGroup card = (ViewGroup) v.getParent().getParent().getParent();

		final View main = card.findViewById(R.id.main);
		final View details = card.findViewById(R.id.details);
		final ViewGroup.MarginLayoutParams detailsParams = (ViewGroup.MarginLayoutParams) details
				.getLayoutParams();

		int from = detailsParams.topMargin == main.getHeight() ? main
				.getHeight() : (main.getHeight() - details.getHeight());
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

	private void showReportOptions(AbstractReport report, View v) {
		mCurrentMenuReport = report;
		mCurrentMenuReportView = (ViewGroup) v.getParent().getParent().getParent();

		PopupMenu popup = new PopupMenu(getActivity(), v);
		popup.inflate(R.menu.report_options);
		popup.setOnMenuItemClickListener(this);

		Menu menu = popup.getMenu();
		menu.findItem(R.id.menu_show_trend).setChecked(report.isShowTrend());
		menu.findItem(R.id.menu_show_overall_trend).setChecked(report.isShowOverallTrend());

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
}