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
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;
import org.juanro.autumandu.data.report.AbstractReport;
import org.juanro.autumandu.data.report.AbstractReportChartData;
import org.juanro.autumandu.data.report.FuelConsumptionReport;
import org.juanro.autumandu.data.report.FuelPriceReport;
import org.juanro.autumandu.data.report.MileageReport;
import org.juanro.autumandu.data.report.OverallCostsReport;
import org.juanro.autumandu.data.report.TripReport;
import org.juanro.autumandu.data.report.ReportChartOptions;
import org.juanro.autumandu.gui.MainActivity.BackPressedListener;
import org.juanro.autumandu.gui.chart.kubit.KubitChartBridge;
import org.juanro.autumandu.gui.util.FabSpeedDialHelper;
import org.juanro.autumandu.gui.util.FloatingActionButtonRevealer;
import org.juanro.autumandu.gui.util.ReportDetailBinder;
import org.juanro.autumandu.gui.util.ReportFullScreenAnimator;
import org.juanro.autumandu.util.Carburoid;
import org.juanro.autumandu.viewmodel.ReportViewModel;

public class ReportFragment extends Fragment implements PopupMenu.OnMenuItemClickListener,
        BackPressedListener {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private ReportAdapter reportAdapter;
    private ReportViewModel viewModel;
    private AbstractReport currentMenuReport;

    private ReportFullScreenAnimator fullScreenAnimator;

    private class ReportHolder extends RecyclerView.ViewHolder {
        private final TextView txtTitle;
        private final FrameLayout chartContainer;
        private final View chartNotEnoughData;
        private final View main;
        private final ViewGroup details;
        private final View chartLoading;
        private final MaterialButton btnReportAction;

        private AbstractReport report;

        public ReportHolder(View itemView) {
            super(itemView);

            chartContainer = itemView.findViewById(R.id.chart_container);
            chartLoading = itemView.findViewById(R.id.chart_loading);
            btnReportAction = itemView.findViewById(R.id.btn_report_action);

            txtTitle = itemView.findViewById(R.id.txt_title);
            txtTitle.setOnClickListener(v -> showFullScreenChart(report, chartContainer));

            View btnReportDetails = itemView.findViewById(R.id.btn_report_details);
            btnReportDetails.setOnClickListener(v -> toggleDetails());

            View btnReportOptions = itemView.findViewById(R.id.btn_report_options);
            btnReportOptions.setOnClickListener(v -> showOptions(report, v));

            chartNotEnoughData = itemView.findViewById(R.id.chart_not_enough_data);
            main = itemView.findViewById(R.id.main);
            details = itemView.findViewById(R.id.details);
        }

        private void showFullScreenChart(AbstractReport report, View v) {
            if (getView() == null) {
                return;
            }

            var options = ReportAdapter.loadOptions(itemView.getContext(), report);
            var rawData = report.getRawChartData(options.getChartOption());

            View kubitView;
            if (report instanceof OverallCostsReport || report instanceof TripReport) {
                kubitView = KubitChartBridge.createPieChart(itemView.getContext(), report, rawData, options.getChartOption());
            } else if (report instanceof FuelConsumptionReport ||
                    report instanceof FuelPriceReport ||
                    (report instanceof MileageReport && options.getChartOption() != 2)) {
                kubitView = KubitChartBridge.createLineChart(itemView.getContext(), report, rawData, options.getChartOption(), options.isShowTrend(), options.isShowOverallTrend(), true);
            } else {
                kubitView = KubitChartBridge.createColumnChart(itemView.getContext(), report, rawData, options.getChartOption(), options.isShowTrend(), options.isShowOverallTrend(), true);
            }

            int animationTime = itemView.getResources().getInteger(android.R.integer.config_longAnimTime);
            fullScreenAnimator.show(v, getView(), kubitView, animationTime);
        }

        private void showOptions(AbstractReport report, View v) {
            currentMenuReport = report;
            var options = ReportAdapter.loadOptions(itemView.getContext(), report);

            var popup = new PopupMenu(itemView.getContext(), v);
            popup.inflate(R.menu.report_options);
            popup.setOnMenuItemClickListener(ReportFragment.this);

            var menu = popup.getMenu();
            if (report instanceof OverallCostsReport) {
                menu.removeItem(R.id.menu_show_trend);
                menu.removeItem(R.id.menu_show_overall_trend);
            } else {
                menu.findItem(R.id.menu_show_trend).setChecked(options.isShowTrend());
                menu.findItem(R.id.menu_show_overall_trend).setChecked(options.isShowOverallTrend());
            }

            var graphOptions = report.getAvailableChartOptions();
            if (graphOptions.length >= 2) {
                for (var i = 0; i < graphOptions.length; i++) {
                    var item = menu.add(R.id.group_graph, Menu.NONE, i, graphOptions[i]);
                    item.setChecked(i == options.getChartOption());
                }
                menu.setGroupCheckable(R.id.group_graph, true, true);
            }

            popup.show();
        }

        private void removePreviousKubitChart() {
            View previous = chartContainer.findViewById(R.id.kubit_chart_view);
            if (previous != null) {
                chartContainer.removeView(previous);
            }
        }

        public void bind(AbstractReport report) {
            this.report = report;
            txtTitle.setText(report.getTitle());

            if (report instanceof FuelPriceReport) {
                Preferences prefs = new Preferences(itemView.getContext());
                if (prefs.isShowCarburoidEnabled()) {
                    btnReportAction.setVisibility(View.VISIBLE);
                    btnReportAction.setText(R.string.btn_find_station_carburoid);
                    btnReportAction.setOnClickListener(v -> {
                        String fuelName = ((FuelPriceReport) report).getMostRecentFuelTypeName();
                        Carburoid.launch(v.getContext(), fuelName);
                    });
                } else {
                    btnReportAction.setVisibility(View.GONE);
                }
            } else {
                btnReportAction.setVisibility(View.GONE);
            }

            resetUIState();

            EXECUTOR.execute(() -> {
                report.update();
                var options = ReportAdapter.loadOptions(itemView.getContext(), report);
                var reportData = report.getData(true);
                var rawData = report.getRawChartData(options.getChartOption());
                boolean enoughData = !rawData.isEmpty();

                itemView.post(() -> updateUI(report, options, reportData, rawData, enoughData));

                try { Thread.sleep(250); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
        }

        private void toggleDetails() {
            final var detailsParams = (ViewGroup.MarginLayoutParams) details.getLayoutParams();

            var from = detailsParams.topMargin;
            var to = (from >= main.getHeight()) ? (main.getHeight() - details.getHeight()) : main.getHeight();

            var animator = new ValueAnimator();
            animator.setDuration(itemView.getResources().getInteger(android.R.integer.config_longAnimTime));
            animator.setValues(PropertyValuesHolder.ofInt((String) null, from, to));
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    details.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (detailsParams.topMargin <= main.getHeight() - details.getHeight()) {
                        details.setVisibility(View.INVISIBLE);
                    }
                }
            });
            animator.addUpdateListener(animation -> {
                detailsParams.topMargin = (Integer) animation.getAnimatedValue();
                details.requestLayout();
            });
            animator.start();
        }

        private void resetUIState() {
            removePreviousKubitChart();
            details.setVisibility(View.GONE);
            chartNotEnoughData.setVisibility(View.GONE);
            chartContainer.setVisibility(View.VISIBLE);
            chartLoading.setAlpha(1f);
            chartLoading.setVisibility(View.VISIBLE);
            chartLoading.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }

        private void updateUI(AbstractReport report, ReportChartOptions options, List<AbstractReport.AbstractListItem> reportData, List<? extends AbstractReportChartData> rawData, boolean enoughData) {
            if (this.report != report) return;

            if (enoughData) {
                renderChart(report, options, rawData);
            }

            chartLoading.animate().alpha(0f).setDuration(200).withEndAction(() -> {
                chartLoading.setVisibility(View.GONE);
                chartLoading.setLayerType(View.LAYER_TYPE_NONE, null);
            }).start();

            chartNotEnoughData.setVisibility(enoughData ? View.GONE : View.VISIBLE);

            ReportDetailBinder.bindDetails(details, reportData);

            var params = (ViewGroup.MarginLayoutParams) details.getLayoutParams();
            params.topMargin = main.getHeight();
            details.requestLayout();
        }

        private void renderChart(AbstractReport report, ReportChartOptions options, List<? extends AbstractReportChartData> rawData) {
            View kubitView;
            if (report instanceof OverallCostsReport || report instanceof TripReport) {
                kubitView = KubitChartBridge.createPieChart(itemView.getContext(), report, rawData, options.getChartOption());
            } else if (report instanceof FuelConsumptionReport ||
                    report instanceof FuelPriceReport ||
                    (report instanceof MileageReport && options.getChartOption() != 2)) {
                kubitView = KubitChartBridge.createLineChart(itemView.getContext(), report, rawData, options.getChartOption(), options.isShowTrend(), options.isShowOverallTrend());
            } else {
                kubitView = KubitChartBridge.createColumnChart(itemView.getContext(), report, rawData, options.getChartOption(), options.isShowTrend(), options.isShowOverallTrend());
            }
            kubitView.setId(R.id.kubit_chart_view);
            kubitView.setAlpha(0f);
            chartContainer.addView(kubitView);
            kubitView.animate().alpha(1f).setDuration(400).start();
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
                    return Objects.equals(oldItem, newItem) && oldItem.isUpdated();
                }
            });
        }

        @NonNull
        @Override
        public ReportHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
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

        public static ReportChartOptions loadOptions(Context context, AbstractReport report) {
            var prefs = context.getSharedPreferences(ReportFragment.class.getName(), Context.MODE_PRIVATE);
            var reportName = report.getClass().getSimpleName();

            var options = new ReportChartOptions();
            options.setShowTrend(prefs.getBoolean(reportName + "_show_trend", false));
            options.setShowOverallTrend(prefs.getBoolean(reportName + "_show_overall_trend", false));
            options.setChartOption(prefs.getInt(reportName + "_current_chart_option", 0));

            return options;
        }

        public static void saveOptions(Context context, AbstractReport report, ReportChartOptions options) {
            var prefsEdit = context.getSharedPreferences(ReportFragment.class.getName(), Context.MODE_PRIVATE).edit();
            var reportName = report.getClass().getSimpleName();

            prefsEdit.putBoolean(reportName + "_show_trend", options.isShowTrend());
            prefsEdit.putBoolean(reportName + "_show_overall_trend", options.isShowOverallTrend());
            prefsEdit.putInt(reportName + "_current_chart_option", options.getChartOption());
            prefsEdit.apply();
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

            if (position < columns) {
                outRect.top = spacing;
            }

            if (position % columns == 0) {
                outRect.left = spacing;
            }
        }
    }

    @Override
    public boolean onBackPressed() {
        if (fullScreenAnimator != null && fullScreenAnimator.isVisible()) {
            hideFullScreenChart();
            return true;
        }
        return false;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (viewModel != null) {
            viewModel.invalidateAndRefresh();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final var v = inflater.inflate(R.layout.fragment_report, container, false);

        var appBarLayout = (AppBarLayout) v.findViewById(R.id.app_bar_layout);

        var recyclerView = (RecyclerView) v.findViewById(R.id.list);
        var orientation = getResources().getConfiguration().orientation;
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(
                orientation == Configuration.ORIENTATION_LANDSCAPE ? 2 : 1,
                StaggeredGridLayoutManager.VERTICAL));
        reportAdapter = new ReportAdapter();
        recyclerView.setAdapter(reportAdapter);
        recyclerView.addItemDecoration(new ReportItemDecoration());
        recyclerView.setItemAnimator(null);

        View fabContainer = v.findViewById(R.id.fab_container);
        if (fabContainer != null) {
            FabSpeedDialHelper fabHelper = new FabSpeedDialHelper(fabContainer);
            FloatingActionButtonRevealer.setup(fabHelper, recyclerView);
        }

        var fullScreenChart = (FrameLayout) v.findViewById(R.id.full_screen_chart);
        var fullScreenChartHolder = v.findViewById(R.id.full_screen_chart_holder);

        fullScreenAnimator = new ReportFullScreenAnimator(appBarLayout, fullScreenChart, fullScreenChartHolder);

        var btnCloseFullScreen = v.findViewById(R.id.btn_close_full_screen);
        btnCloseFullScreen.setOnClickListener(view -> hideFullScreenChart());

        viewModel = new ViewModelProvider(this).get(ReportViewModel.class);
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
        var options = ReportAdapter.loadOptions(requireContext(), currentMenuReport);
        if (item.getItemId() == R.id.menu_show_trend) {
            options.setShowTrend(!item.isChecked());
        } else if (item.getItemId() == R.id.menu_show_overall_trend) {
            options.setShowOverallTrend(!item.isChecked());
        } else if (item.getGroupId() == R.id.group_graph) {
            options.setChartOption(item.getOrder());
        }

        ReportAdapter.saveOptions(requireContext(), currentMenuReport, options);

        int index = reportAdapter.getCurrentList().indexOf(currentMenuReport);
        if (index != -1) {
            reportAdapter.notifyItemChanged(index);
        } else {
            List<AbstractReport> currentList = reportAdapter.getCurrentList();
            for (int i = 0; i < currentList.size(); i++) {
                if (currentList.get(i).getClass().equals(currentMenuReport.getClass())) {
                    reportAdapter.notifyItemChanged(i);
                    break;
                }
            }
        }

        return true;
    }



    private void hideFullScreenChart() {
        int animationTime = getResources().getInteger(android.R.integer.config_longAnimTime);
        fullScreenAnimator.hide(animationTime);
    }
}
