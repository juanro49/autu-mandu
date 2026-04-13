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

package org.juanro.autumandu.gui.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionMenu;

import java.util.List;

import lecho.lib.hellocharts.listener.ComboLineColumnChartOnValueSelectListener;
import lecho.lib.hellocharts.model.ComboLineColumnChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.SubcolumnValue;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.ComboLineColumnChartView;
import lecho.lib.hellocharts.view.PieChartView;
import org.juanro.autumandu.R;
import org.juanro.autumandu.data.report.AbstractReport;
import org.juanro.autumandu.data.report.AbstractReportChartColumnData;
import org.juanro.autumandu.data.report.AbstractReportChartLineData;
import org.juanro.autumandu.data.report.ReportChartOptions;
import org.juanro.autumandu.gui.MainActivity.BackPressedListener;
import org.juanro.autumandu.gui.util.FloatingActionButtonRevealer;
import org.juanro.autumandu.viewmodel.ReportViewModel;

public class ReportFragment extends Fragment implements PopupMenu.OnMenuItemClickListener,
        BackPressedListener {

    private ReportAdapter reportAdapter;

    private AppBarLayout appBarLayout;
    private AbstractReport currentMenuReport;
    private ViewGroup currentMenuReportView;

    private ComboLineColumnChartView fullScreenChart;
    private View fullScreenChartHolder;
    private Animator fullScreenChartAnimator;
    private ComboLineColumnChartView currentFullScreenChart;
    private Rect currentFullScreenStartBounds;
    private float currentFullScreenStartScaleX;
    private float currentFullScreenStartScaleY;

    private class ReportHolder extends RecyclerView.ViewHolder {
        private final TextView txtTitle;
        private final ComboLineColumnChartView chart;
        private final PieChartView chartPie;
        private final View chartNotEnoughData;
        private final ViewGroup details;

        private AbstractReport report;

        public ReportHolder(View itemView) {
            super(itemView);

            txtTitle = itemView.findViewById(R.id.txt_title);

            View btnReportDetails = itemView.findViewById(R.id.btn_report_details);
            btnReportDetails.setOnClickListener(ReportFragment.this::toggleReportDetails);

            View btnReportOptions = itemView.findViewById(R.id.btn_report_options);
            btnReportOptions.setOnClickListener(v -> showReportOptions(report, v));

            chart = itemView.findViewById(R.id.chart);
            chart.setZoomEnabled(false);
            chart.setScrollEnabled(false);
            chart.setValueTouchEnabled(false);
            chart.setOnClickListener(v -> showFullScreenChart(report, chart));

            chartPie = itemView.findViewById(R.id.chart_pie);
            chartPie.setChartRotationEnabled(true);
            chartPie.setValueTouchEnabled(true);

            chartNotEnoughData = itemView.findViewById(R.id.chart_not_enough_data);

            details = itemView.findViewById(R.id.details);
        }

        public void bind(AbstractReport report) {
            this.report = report;

            txtTitle.setText(report.getTitle());

            new Thread(() -> {
                var options = loadReportChartOptions(itemView.getContext(), report);
                var data = report.getChartData(options);
                var pieData = report.getPieChartData();
                var reportData = report.getData(true);

                itemView.post(() -> {
                    if (this.report != report) {
                        return;
                    }

                    boolean enoughData;
                    if (pieData != null) {
                        chartPie.setPieChartData(pieData);
                        chartPie.setVisibility(View.VISIBLE);
                        chart.setVisibility(View.GONE);
                        enoughData = !pieData.getValues().isEmpty();
                    } else {
                        chart.setComboLineColumnChartData(data);
                        applyViewport(chart, true);
                        chartPie.setVisibility(View.GONE);
                        chart.setVisibility(View.VISIBLE);
                        enoughData = data != null && (!data.getLineChartData().getLines().isEmpty() ||
                                !data.getColumnChartData().getColumns().isEmpty());
                    }

                    chart.setVisibility(enoughData && pieData == null ? View.VISIBLE : View.GONE);
                    chartPie.setVisibility(enoughData && pieData != null ? View.VISIBLE : View.GONE);
                    chartNotEnoughData.setVisibility(enoughData ? View.GONE : View.VISIBLE);

                    details.removeAllViews();
                    for (var item : reportData) {
                        var rowView = View.inflate(details.getContext(),
                                item instanceof AbstractReport.Section
                                        ? R.layout.report_row_section
                                        : R.layout.report_row_data, null);

                        if (item instanceof AbstractReport.Section section) {
                            var text = (TextView) rowView;

                            text.setText(section.getLabel());
                            text.setTextColor(section.getColor());
                            var drawableBottom = (GradientDrawable) text
                                    .getCompoundDrawables()[3];
                            drawableBottom.setColorFilter(section.getColor(), PorterDuff.Mode.SRC);
                        } else {
                            ((TextView) rowView.findViewById(android.R.id.text1)).setText(item.getLabel());
                            ((TextView) rowView.findViewById(android.R.id.text2))
                                    .setText(((AbstractReport.Item) item).getValue());
                        }

                        details.addView(rowView);
                    }
                });
            }).start();
        }
    }

    private class ReportAdapter extends ListAdapter<AbstractReport, ReportHolder> {
        public ReportAdapter() {
            super(new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull AbstractReport oldItem, @NonNull AbstractReport newItem) {
                    return oldItem.getClass().equals(newItem.getClass());
                }

                @Override
                public boolean areContentsTheSame(@NonNull AbstractReport oldItem, @NonNull AbstractReport newItem) {
                    // This is a bit simplified, but since reports are recalculated,
                    // we usually want to rebind.
                    return false;
                }
            });
        }

        @NonNull
        @Override
        public ReportHolder onCreateViewHolder(@NonNull ViewGroup parent, int position) {
            var view = LayoutInflater.from(parent.getContext()).inflate(R.layout.report,
                    parent, false);
            return new ReportHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ReportHolder reportHolder, int position) {
            var report = getItem(position);
            reportHolder.bind(report);
        }

        public void setItems(List<AbstractReport> items) {
            submitList(items);
        }
    }

    private class ReportItemDecoration extends RecyclerView.ItemDecoration {
        private final int spacing;

        public ReportItemDecoration() {
            spacing = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16,
                    getResources().getDisplayMetrics());
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            var layoutManager = (StaggeredGridLayoutManager) parent.getLayoutManager();
            if (layoutManager == null) {
                return;
            }
            var columns = layoutManager.getSpanCount();
            var position = parent.getChildLayoutPosition(view);

            outRect.right = spacing;
            outRect.bottom = spacing;

            // Add top spacing for items in first row.
            if (position < columns) {
                outRect.top = spacing;
            }

            // Add left spacing for items in the first column.
            if (position % columns == 0) {
                outRect.left = spacing;
            }
        }
    }

    @Override
    public boolean onBackPressed() {
        if (currentFullScreenChart != null) {
            hideFullScreenChart();
            return true;
        }

        return false;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final var v = inflater.inflate(R.layout.fragment_report, container, false);

        appBarLayout = v.findViewById(R.id.app_bar_layout);

        var recyclerView = (RecyclerView) v.findViewById(R.id.list);
        var orientation = getResources().getConfiguration().orientation;
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(
                orientation == Configuration.ORIENTATION_LANDSCAPE ? 2 : 1,
                StaggeredGridLayoutManager.VERTICAL));
        reportAdapter = new ReportAdapter();
        recyclerView.setAdapter(reportAdapter);
        recyclerView.addItemDecoration(new ReportItemDecoration());

        var fab = (FloatingActionMenu) v.findViewById(R.id.fab);
        FloatingActionButtonRevealer.setup(fab, recyclerView);

        fullScreenChart = v.findViewById(R.id.full_screen_chart);
        fullScreenChart.setOnValueTouchListener(new ComboLineColumnChartOnValueSelectListener() {
            @Override
            public void onColumnValueSelected(int columnIndex, int subcolumnIndex, SubcolumnValue value) {
                if (value instanceof AbstractReportChartColumnData.SubcolumnValueWithTooltip subcolumnValueWithTooltip) {
                    var tooltip = subcolumnValueWithTooltip.getTooltip();
                    Snackbar.make(v, tooltip, Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onPointValueSelected(int lineIndex, int pointIndex, PointValue value) {
                if (value instanceof AbstractReportChartLineData.PointValueWithTooltip pointValueWithTooltip) {
                    var tooltip = pointValueWithTooltip.getTooltip();
                    Snackbar.make(v, tooltip, Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onValueDeselected() {
            }
        });

        fullScreenChartHolder = v.findViewById(R.id.full_screen_chart_holder);

        var btnCloseFullScreen = v.findViewById(R.id.btn_close_full_screen);
        btnCloseFullScreen.setOnClickListener(v1 -> hideFullScreenChart());

        ReportViewModel viewModel = new ViewModelProvider(this).get(ReportViewModel.class);
        viewModel.getReports().observe(getViewLifecycleOwner(), reports -> reportAdapter.setItems(reports));

        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(view.findViewById(R.id.toolbar));
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        var options = loadReportChartOptions(requireContext(), currentMenuReport);
        if (item.getItemId() == R.id.menu_show_trend) {
            options.setShowTrend(!item.isChecked());
        } else if (item.getItemId() == R.id.menu_show_overall_trend) {
            options.setShowOverallTrend(!item.isChecked());
        } else if (item.getGroupId() == R.id.group_graph) {
            options.setChartOption(item.getOrder());
        }

        saveReportChartOptions(requireContext(), currentMenuReport, options);
        var chart = (ComboLineColumnChartView) currentMenuReportView
                .findViewById(R.id.chart);
        chart.setComboLineColumnChartData(currentMenuReport.getChartData(options));
        applyViewport(chart, true);

        return true;
    }

    private void hideFullScreenChart() {
        if (fullScreenChartAnimator != null) {
            fullScreenChartAnimator.cancel();
        }

        // Animate the four positioning/sizing properties in parallel, back to
        // their original values.
        var set = new AnimatorSet();
        set.play(
                ObjectAnimator.ofFloat(fullScreenChartHolder, View.X,
                        currentFullScreenStartBounds.left))
                .with(ObjectAnimator.ofFloat(fullScreenChartHolder, View.Y,
                        currentFullScreenStartBounds.top))
                .with(ObjectAnimator.ofFloat(fullScreenChartHolder, View.SCALE_X,
                        currentFullScreenStartScaleX))
                .with(ObjectAnimator.ofFloat(fullScreenChartHolder, View.SCALE_Y,
                        currentFullScreenStartScaleY));
        set.setDuration(getResources().getInteger(
                android.R.integer.config_longAnimTime));
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                appBarLayout.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                currentFullScreenChart.setVisibility(View.VISIBLE);
                currentFullScreenChart = null;
                fullScreenChartHolder.setVisibility(View.GONE);
                fullScreenChartAnimator = null;
            }
        });
        set.start();
        fullScreenChartAnimator = set;
    }

    private void showFullScreenChart(AbstractReport report, ComboLineColumnChartView v) {
        if (getView() == null) {
            return;
        }

        if (fullScreenChartAnimator != null) {
            fullScreenChartAnimator.cancel();
        }

        currentFullScreenChart = v;

        var options = loadReportChartOptions(requireContext(), report);
        fullScreenChart.setComboLineColumnChartData(report.getChartData(options));
        applyViewport(fullScreenChart, false);

        // Calculate translation start and end point and scales.
        currentFullScreenStartBounds = new Rect();
        final var finalBounds = new Rect();
        final var globalOffset = new Point();

        currentFullScreenChart.getGlobalVisibleRect(currentFullScreenStartBounds);
        getView().getGlobalVisibleRect(finalBounds, globalOffset);
        currentFullScreenStartBounds.offset(-globalOffset.x, -globalOffset.y);
        finalBounds.offset(-globalOffset.x, -globalOffset.y);

        currentFullScreenStartScaleX = (float) currentFullScreenStartBounds
                .width() / finalBounds.width();
        currentFullScreenStartScaleY = (float) currentFullScreenStartBounds
                .height() / finalBounds.height();

        // Hide the small chart and show the zoomed-in view. When the animation
        // begins, it will position the zoomed-in view in the place of the small
        // chart.
        currentFullScreenChart.setVisibility(View.INVISIBLE);
        fullScreenChartHolder.setVisibility(View.VISIBLE);

        // Set the pivot point for SCALE_X and SCALE_Y transformations to the
        // top-left corner of the zoomed-in view (the default is the center of
        // the view).
        fullScreenChartHolder.setPivotX(0f);
        fullScreenChartHolder.setPivotY(0f);

        // Construct and run the parallel animation of the four translation and
        // scale properties (X, Y, SCALE_X, and SCALE_Y).
        var set = new AnimatorSet();
        set.play(
                ObjectAnimator.ofFloat(fullScreenChartHolder, View.X,
                        currentFullScreenStartBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(fullScreenChartHolder, View.Y,
                        currentFullScreenStartBounds.top, finalBounds.top))
                .with(ObjectAnimator.ofFloat(fullScreenChartHolder, View.SCALE_X,
                        currentFullScreenStartScaleX, 1f))
                .with(ObjectAnimator.ofFloat(fullScreenChartHolder, View.SCALE_Y,
                        currentFullScreenStartScaleY, 1f));
        set.setDuration(getResources().getInteger(
                android.R.integer.config_longAnimTime));
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                fullScreenChartAnimator = null;
                appBarLayout.setVisibility(View.INVISIBLE);
            }
        });
        set.start();
        fullScreenChartAnimator = set;
    }

    private void toggleReportDetails(View v) {
        var card = (ViewGroup) v.getParent().getParent().getParent();

        final var main = card.findViewById(R.id.main);
        final var details = card.findViewById(R.id.details);
        final var detailsParams = (ViewGroup.MarginLayoutParams) details
                .getLayoutParams();

        var from = detailsParams.topMargin == main.getHeight() ? main
                .getHeight() : (main.getHeight() - details.getHeight());
        var to = detailsParams.topMargin == main.getHeight() ? (main
                .getHeight() - details.getHeight()) : main.getHeight();

        var animator = new ValueAnimator();
        animator.setDuration(getResources().getInteger(
                android.R.integer.config_longAnimTime));
        animator.setValues(PropertyValuesHolder.ofInt((String) null, from, to));
        animator.addUpdateListener(animation -> {
            detailsParams.topMargin = (Integer) animation
                    .getAnimatedValue();
            details.requestLayout();
        });
        animator.start();
    }

    private void showReportOptions(AbstractReport report, View v) {
        currentMenuReport = report;
        currentMenuReportView = (ViewGroup) v.getParent().getParent().getParent();
        var options = loadReportChartOptions(requireContext(), report);

        var popup = new PopupMenu(getActivity(), v);
        popup.inflate(R.menu.report_options);
        popup.setOnMenuItemClickListener(this);

        var menu = popup.getMenu();
        menu.findItem(R.id.menu_show_trend).setChecked(options.isShowTrend());
        menu.findItem(R.id.menu_show_overall_trend).setChecked(options.isShowOverallTrend());

        var graphOptions = report.getAvailableChartOptions();
        if (graphOptions.length >= 2) {
            for (var i = 0; i < graphOptions.length; i++) {
                var item = menu.add(R.id.group_graph, Menu.NONE, i,
                        graphOptions[i]);
                item.setChecked(i == options.getChartOption());
            }

            menu.setGroupCheckable(R.id.group_graph, true, true);
        }

        popup.show();
    }

    private static ReportChartOptions loadReportChartOptions(Context context, AbstractReport report) {
        var prefs = context.getSharedPreferences(ReportFragment.class.getName(),
                Context.MODE_PRIVATE);
        var reportName = report.getClass().getSimpleName();

        var options = new ReportChartOptions();
        options.setShowTrend(prefs.getBoolean(reportName + "_show_trend", false));
        options.setShowOverallTrend(prefs.getBoolean(reportName + "_show_overall_trend", false));
        options.setChartOption(prefs.getInt(reportName + "_current_chart_option", 0));

        return options;
    }

    private static void saveReportChartOptions(Context context, AbstractReport report, ReportChartOptions options) {
        var prefsEdit = context.getSharedPreferences(
                ReportFragment.class.getName(), Context.MODE_PRIVATE).edit();
        var reportName = report.getClass().getSimpleName();

        prefsEdit.putBoolean(reportName + "_show_trend", options.isShowTrend());
        prefsEdit.putBoolean(reportName + "_show_overall_trend", options.isShowOverallTrend());
        prefsEdit.putInt(reportName + "_current_chart_option", options.getChartOption());
        prefsEdit.apply();
    }

    /**
     * Apply viewport to see only the last 3 columns / last 10 points
     *
     * @param chart The chart view.
     * @param fix   Should the user be able to change the viewport.
     */
    private static void applyViewport(ComboLineColumnChartView chart, boolean fix) {
        var data = (ComboLineColumnChartData) chart.getChartData();

        var leftValue = Math.max(0, data.getColumnChartData().getColumns().size() - 3 - .5f);
        for (var line : data.getLineChartData().getLines()) {
            if (!line.getValues().isEmpty()) {
                var i = Math.max(0, line.getValues().size() - 10);
                leftValue = Math.max(leftValue, line.getValues().get(i).getX());
            }
        }

        var tempViewport = new Viewport(chart.getMaximumViewport());
        tempViewport.left = leftValue;
        chart.setCurrentViewport(tempViewport);
        if (fix) {
            chart.setMaximumViewport(tempViewport);
        }
    }
}
