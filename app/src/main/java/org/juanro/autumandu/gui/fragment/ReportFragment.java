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
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.appbar.AppBarLayout;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.juanro.autumandu.R;
import org.juanro.autumandu.data.report.AbstractReport;
import org.juanro.autumandu.data.report.FuelConsumptionReport;
import org.juanro.autumandu.data.report.FuelPriceReport;
import org.juanro.autumandu.data.report.MileageReport;
import org.juanro.autumandu.data.report.OverallCostsReport;
import org.juanro.autumandu.data.report.ReportChartOptions;
import org.juanro.autumandu.gui.MainActivity.BackPressedListener;
import org.juanro.autumandu.gui.chart.kubit.KubitChartBridge;
import org.juanro.autumandu.gui.util.FabSpeedDialHelper;
import org.juanro.autumandu.gui.util.FloatingActionButtonRevealer;
import org.juanro.autumandu.viewmodel.ReportViewModel;

public class ReportFragment extends Fragment implements PopupMenu.OnMenuItemClickListener,
        BackPressedListener {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private ReportAdapter reportAdapter;
    private AbstractReport currentMenuReport;

    private AppBarLayout appBarLayout;
    private FrameLayout fullScreenChart;
    private View fullScreenChartHolder;
    private Animator fullScreenChartAnimator;
    private View currentFullScreenOriginView;
    private Rect currentFullScreenStartBounds;
    private float currentFullScreenStartScaleX;
    private float currentFullScreenStartScaleY;

    private class ReportHolder extends RecyclerView.ViewHolder {
        private final TextView txtTitle;
        private final FrameLayout chartContainer;
        private final View chartNotEnoughData;
        private final View main;
        private final ViewGroup details;

        private AbstractReport report;

        public ReportHolder(View itemView) {
            super(itemView);

            chartContainer = itemView.findViewById(R.id.chart_container);

            txtTitle = itemView.findViewById(R.id.txt_title);
            txtTitle.setOnClickListener(v -> showFullScreenChart(report, chartContainer));

            View btnReportDetails = itemView.findViewById(R.id.btn_report_details);
            btnReportDetails.setOnClickListener(ReportFragment.this::toggleReportDetails);

            View btnReportOptions = itemView.findViewById(R.id.btn_report_options);
            btnReportOptions.setOnClickListener(v -> showReportOptions(report, v));

            chartNotEnoughData = itemView.findViewById(R.id.chart_not_enough_data);
            main = itemView.findViewById(R.id.main);
            details = itemView.findViewById(R.id.details);
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

            // Reset visual para evitar que se vea el layout vacío durante la carga
            chartContainer.setVisibility(View.INVISIBLE);
            chartNotEnoughData.setVisibility(View.GONE);
            details.setVisibility(View.GONE); // GONE para seguridad total

            EXECUTOR.execute(() -> {
                report.update();
                var options = loadReportChartOptions(itemView.getContext(), report);
                var reportData = report.getData(true);
                var rawData = report.getRawChartData(options.getChartOption());
                boolean enoughData = !rawData.isEmpty();

                itemView.post(() -> {
                    if (this.report != report) return;

                    // 1. Renderizar detalles primero
                    int currentChildCount = details.getChildCount();
                    int requiredCount = reportData.size();

                    for (int i = 0; i < Math.max(currentChildCount, requiredCount); i++) {
                        if (i >= requiredCount) {
                            details.getChildAt(i).setVisibility(View.GONE);
                            continue;
                        }

                        var item = reportData.get(i);
                        boolean isSection = item instanceof AbstractReport.Section;
                        int layout = isSection ? R.layout.report_row_section : R.layout.report_row_data;

                        View rowView = i < currentChildCount ? details.getChildAt(i) : null;
                        boolean needsInflate = rowView == null || (isSection != (rowView instanceof TextView));

                        if (needsInflate) {
                            if (rowView != null) details.removeViewAt(i);
                            rowView = LayoutInflater.from(details.getContext()).inflate(layout, details, false);
                            details.addView(rowView, i);
                        } else {
                            rowView.setVisibility(View.VISIBLE);
                        }

                        DetailViewHolder vh = (DetailViewHolder) rowView.getTag();
                        if (vh == null) {
                            vh = new DetailViewHolder(rowView);
                            rowView.setTag(vh);
                        }

                        if (item instanceof AbstractReport.Section section) {
                            vh.label.setText(section.getLabel());
                            vh.label.setTextColor(section.getColor());
                            if (vh.sectionDrawable != null) {
                                vh.sectionDrawable.setColorFilter(section.getColor(), PorterDuff.Mode.SRC);
                            }
                        } else {
                            var dataItem = (AbstractReport.Item) item;
                            vh.label.setText(dataItem.getLabel());
                            if (vh.value != null) vh.value.setText(dataItem.getValue());
                        }
                    }

                    // Estado inicial cerrado (asegurar que esté detrás de 'main')
                    details.post(() -> {
                        if (this.report != report) return;
                        var detailsParams = (ViewGroup.MarginLayoutParams) details.getLayoutParams();
                        detailsParams.topMargin = enoughData ? (main.getHeight() - details.getHeight()) : 0;
                        details.requestLayout();
                    });

                    // 2. Renderizar gráfico después
                    itemView.post(() -> {
                        if (this.report != report) return;

                        removePreviousKubitChart();
                        if (enoughData) {
                            View kubitView;
                            if (report instanceof OverallCostsReport) {
                                kubitView = KubitChartBridge.createPieChart(itemView.getContext(), report, rawData, options.getChartOption());
                            } else if (report instanceof FuelConsumptionReport ||
                                    report instanceof FuelPriceReport ||
                                    (report instanceof MileageReport && options.getChartOption() != 2)) {
                                kubitView = KubitChartBridge.createLineChart(itemView.getContext(), report, rawData, options.getChartOption(), options.isShowTrend(), options.isShowOverallTrend());
                            } else {
                                kubitView = KubitChartBridge.createColumnChart(itemView.getContext(), report, rawData, options.getChartOption(), options.isShowTrend(), options.isShowOverallTrend());
                            }
                            kubitView.setId(R.id.kubit_chart_view);
                            chartContainer.addView(kubitView);
                        }

                        chartNotEnoughData.setVisibility(enoughData ? View.GONE : View.VISIBLE);
                        chartContainer.setVisibility(enoughData ? View.VISIBLE : View.GONE);
                    });
                });
            });
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
        if (currentFullScreenOriginView != null) {
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

        View fabContainer = v.findViewById(R.id.fab_container);
        if (fabContainer != null) {
            FabSpeedDialHelper fabHelper = new FabSpeedDialHelper(fabContainer);
            FloatingActionButtonRevealer.setup(fabHelper, recyclerView);
        }

        fullScreenChart = v.findViewById(R.id.full_screen_chart);
        fullScreenChartHolder = v.findViewById(R.id.full_screen_chart_holder);

        var btnCloseFullScreen = v.findViewById(R.id.btn_close_full_screen);
        btnCloseFullScreen.setOnClickListener(view -> hideFullScreenChart());

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

        // Solo notificamos el cambio para el reporte afectado para evitar recargar todo
        int index = reportAdapter.getCurrentList().indexOf(currentMenuReport);
        if (index != -1) {
            reportAdapter.notifyItemChanged(index);
        } else {
            reportAdapter.notifyDataSetChanged();
        }

        return true;
    }

    private void showFullScreenChart(AbstractReport report, View v) {
        if (getView() == null) {
            return;
        }

        if (fullScreenChartAnimator != null) {
            fullScreenChartAnimator.cancel();
        }

        currentFullScreenOriginView = v;

        var options = loadReportChartOptions(requireContext(), report);
        var rawData = report.getRawChartData(options.getChartOption());

        fullScreenChart.removeAllViews();
        View kubitView;
        if (report instanceof OverallCostsReport) {
            kubitView = KubitChartBridge.createPieChart(requireContext(), report, rawData, options.getChartOption());
        } else if (report instanceof FuelConsumptionReport ||
                report instanceof FuelPriceReport ||
                (report instanceof MileageReport && options.getChartOption() != 2)) {
            kubitView = KubitChartBridge.createLineChart(requireContext(), report, rawData, options.getChartOption(), options.isShowTrend(), options.isShowOverallTrend(), true);
        } else {
            kubitView = KubitChartBridge.createColumnChart(requireContext(), report, rawData, options.getChartOption(), options.isShowTrend(), options.isShowOverallTrend(), true);
        }
        fullScreenChart.addView(kubitView);

        currentFullScreenStartBounds = new Rect();
        final var finalBounds = new Rect();
        final var globalOffset = new Point();

        currentFullScreenOriginView.getGlobalVisibleRect(currentFullScreenStartBounds);
        getView().getGlobalVisibleRect(finalBounds, globalOffset);
        currentFullScreenStartBounds.offset(-globalOffset.x, -globalOffset.y);
        finalBounds.offset(-globalOffset.x, -globalOffset.y);

        currentFullScreenStartScaleX = (float) currentFullScreenStartBounds.width() / finalBounds.width();
        currentFullScreenStartScaleY = (float) currentFullScreenStartBounds.height() / finalBounds.height();

        currentFullScreenOriginView.setVisibility(View.INVISIBLE);
        fullScreenChartHolder.setVisibility(View.VISIBLE);

        fullScreenChartHolder.setPivotX(0f);
        fullScreenChartHolder.setPivotY(0f);

        var set = new AnimatorSet();
        set.play(ObjectAnimator.ofFloat(fullScreenChartHolder, View.X,
                currentFullScreenStartBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(fullScreenChartHolder, View.Y,
                        currentFullScreenStartBounds.top, finalBounds.top))
                .with(ObjectAnimator.ofFloat(fullScreenChartHolder, View.SCALE_X,
                        currentFullScreenStartScaleX, 1f))
                .with(ObjectAnimator.ofFloat(fullScreenChartHolder, View.SCALE_Y,
                        currentFullScreenStartScaleY, 1f));
        set.setDuration(getResources().getInteger(android.R.integer.config_longAnimTime));
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

    private void hideFullScreenChart() {
        if (fullScreenChartAnimator != null) {
            fullScreenChartAnimator.cancel();
        }

        var set = new AnimatorSet();
        set.play(ObjectAnimator.ofFloat(fullScreenChartHolder, View.X,
                currentFullScreenStartBounds.left))
                .with(ObjectAnimator.ofFloat(fullScreenChartHolder, View.Y,
                        currentFullScreenStartBounds.top))
                .with(ObjectAnimator.ofFloat(fullScreenChartHolder, View.SCALE_X,
                        currentFullScreenStartScaleX))
                .with(ObjectAnimator.ofFloat(fullScreenChartHolder, View.SCALE_Y,
                        currentFullScreenStartScaleY));
        set.setDuration(getResources().getInteger(android.R.integer.config_longAnimTime));
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                appBarLayout.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                currentFullScreenOriginView.setVisibility(View.VISIBLE);
                currentFullScreenOriginView = null;
                fullScreenChartHolder.setVisibility(View.GONE);
                fullScreenChartAnimator = null;
                fullScreenChart.removeAllViews();
            }
        });
        set.start();
        fullScreenChartAnimator = set;
    }

    private void toggleReportDetails(View v) {
        View container = v;
        while (container != null && container.getId() != R.id.report_content_container) {
            container = (View) container.getParent();
        }

        if (container == null) {
            return;
        }

        final var main = container.findViewById(R.id.main);
        final var details = container.findViewById(R.id.details);
        final var detailsParams = (ViewGroup.MarginLayoutParams) details.getLayoutParams();

        var from = detailsParams.topMargin;
        var to = (from >= main.getHeight()) ? (main.getHeight() - details.getHeight()) : main.getHeight();

        var animator = new ValueAnimator();
        animator.setDuration(getResources().getInteger(android.R.integer.config_longAnimTime));
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

    private void showReportOptions(AbstractReport report, View v) {
        currentMenuReport = report;
        var options = loadReportChartOptions(requireContext(), report);

        var popup = new PopupMenu(getActivity(), v);
        popup.inflate(R.menu.report_options);
        popup.setOnMenuItemClickListener(this);

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

    private static ReportChartOptions loadReportChartOptions(Context context, AbstractReport report) {
        var prefs = context.getSharedPreferences(ReportFragment.class.getName(), Context.MODE_PRIVATE);
        var reportName = report.getClass().getSimpleName();

        var options = new ReportChartOptions();
        options.setShowTrend(prefs.getBoolean(reportName + "_show_trend", false));
        options.setShowOverallTrend(prefs.getBoolean(reportName + "_show_overall_trend", false));
        options.setChartOption(prefs.getInt(reportName + "_current_chart_option", 0));

        return options;
    }

    private static void saveReportChartOptions(Context context, AbstractReport report, ReportChartOptions options) {
        var prefsEdit = context.getSharedPreferences(ReportFragment.class.getName(), Context.MODE_PRIVATE).edit();
        var reportName = report.getClass().getSimpleName();

        prefsEdit.putBoolean(reportName + "_show_trend", options.isShowTrend());
        prefsEdit.putBoolean(reportName + "_show_overall_trend", options.isShowOverallTrend());
        prefsEdit.putInt(reportName + "_current_chart_option", options.getChartOption());
        prefsEdit.apply();
    }

    private static class DetailViewHolder {
        final TextView label;
        @Nullable
        final TextView value;
        @Nullable
        final GradientDrawable sectionDrawable;

        DetailViewHolder(View v) {
            label = v.findViewById(android.R.id.text1);
            value = v.findViewById(android.R.id.text2);
            var drawables = label.getCompoundDrawables();
            if (drawables.length > 3 && drawables[3] instanceof GradientDrawable gd) {
                sectionDrawable = gd;
            } else {
                sectionDrawable = null;
            }
        }
    }
}
