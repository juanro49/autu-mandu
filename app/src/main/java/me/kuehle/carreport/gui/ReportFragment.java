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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionMenu;

import java.util.ArrayList;
import java.util.List;

import lecho.lib.hellocharts.listener.ComboLineColumnChartOnValueSelectListener;
import lecho.lib.hellocharts.model.ComboLineColumnChartData;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.SubcolumnValue;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.ComboLineColumnChartView;
import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.data.report.AbstractReport;
import me.kuehle.carreport.data.report.AbstractReportChartColumnData;
import me.kuehle.carreport.data.report.AbstractReportChartLineData;
import me.kuehle.carreport.data.report.ReportChartOptions;
import me.kuehle.carreport.gui.MainActivity.BackPressedListener;
import me.kuehle.carreport.gui.util.FloatingActionButtonRevealer;

public class ReportFragment extends Fragment implements OnMenuItemClickListener,
        BackPressedListener, LoaderManager.LoaderCallbacks<List<AbstractReport>> {
    public static class ReportLoader extends AsyncTaskLoader<List<AbstractReport>> {
        private final ForceLoadContentObserver mObserver;
        private Preferences mPrefs;

        public ReportLoader(Context context) {
            super(context);
            mObserver = new ForceLoadContentObserver();
            mPrefs = new Preferences(context);
        }

        @Override
        public List<AbstractReport> loadInBackground() {
            List<AbstractReport> reports = new ArrayList<>();

            List<Class<? extends AbstractReport>> reportClasses = mPrefs.getReportOrder();
            for (Class<? extends AbstractReport> reportClass : reportClasses) {
                AbstractReport report = AbstractReport.newInstance(reportClass, getContext());
                if (report != null) {
                    report.update();
                    report.registerContentObserver(mObserver);
                    reports.add(report);
                }
            }

            return reports;
        }

        @Override
        protected void onStartLoading() {
            forceLoad();
        }
    }

    private class ReportHolder extends RecyclerView.ViewHolder {
        private TextView mTxtTitle;
        private ComboLineColumnChartView mChart;
        private View mChartNotEnoughData;
        private ViewGroup mDetails;

        private AbstractReport mReport;

        public ReportHolder(View itemView) {
            super(itemView);

            mTxtTitle = (TextView) itemView.findViewById(R.id.txt_title);

            View btnReportDetails = itemView.findViewById(R.id.btn_report_details);
            btnReportDetails.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleReportDetails(v);
                }
            });

            View btnReportOptions = itemView.findViewById(R.id.btn_report_options);
            btnReportOptions.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showReportOptions(mReport, v);
                }
            });

            mChart = (ComboLineColumnChartView) itemView.findViewById(R.id.chart);
            mChart.setZoomEnabled(false);
            mChart.setScrollEnabled(false);
            mChart.setValueTouchEnabled(false);
            mChart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showFullScreenChart(mReport, mChart);
                }
            });

            mChartNotEnoughData = itemView.findViewById(R.id.chart_not_enough_data);

            mDetails = (ViewGroup) itemView.findViewById(R.id.details);
        }

        public void bind(AbstractReport report) {
            mReport = report;

            mTxtTitle.setText(report.getTitle());

            ReportChartOptions options = loadReportChartOptions(getContext(), report);
            ComboLineColumnChartData data = report.getChartData(options);
            mChart.setComboLineColumnChartData(data);
            applyViewport(mChart, true);

            boolean enoughData = data.getLineChartData().getLines().size() > 0 ||
                    data.getColumnChartData().getColumns().size() > 0;
            mChart.setVisibility(enoughData ? View.VISIBLE : View.GONE);
            mChartNotEnoughData.setVisibility(enoughData ? View.GONE : View.VISIBLE);

            mDetails.removeAllViews();
            for (AbstractReport.AbstractListItem item : report.getData(true)) {
                View itemView = View.inflate(mDetails.getContext(),
                        item instanceof AbstractReport.Section
                                ? R.layout.report_row_section
                                : R.layout.report_row_data, null);

                if (item instanceof AbstractReport.Section) {
                    AbstractReport.Section section = (AbstractReport.Section) item;
                    TextView text = (TextView) itemView;

                    text.setText(section.getLabel());
                    text.setTextColor(section.getColor());
                    GradientDrawable drawableBottom = (GradientDrawable) text
                            .getCompoundDrawables()[3];
                    drawableBottom.setColorFilter(section.getColor(), PorterDuff.Mode.SRC);
                } else {
                    ((TextView) itemView.findViewById(android.R.id.text1)).setText(item.getLabel());
                    ((TextView) itemView.findViewById(android.R.id.text2))
                            .setText(((AbstractReport.Item) item).getValue());
                }

                mDetails.addView(itemView);
            }
        }
    }

    private class ReportAdapter extends RecyclerView.Adapter<ReportHolder> {
        private List<AbstractReport> mReports;

        public ReportAdapter() {
            mReports = null;
        }

        @Override
        public ReportHolder onCreateViewHolder(ViewGroup parent, int position) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.report,
                    parent, false);
            return new ReportHolder(view);
        }

        @Override
        public void onBindViewHolder(ReportHolder reportHolder, int position) {
            AbstractReport report = mReports.get(position);
            reportHolder.bind(report);
        }

        @Override
        public int getItemCount() {
            return mReports == null ? 0 : mReports.size();
        }

        public void setItems(List<AbstractReport> items) {
            mReports = items;
            notifyDataSetChanged();
        }
    }

    private class ReportItemDecoration extends RecyclerView.ItemDecoration {
        private int mSpacing;

        public ReportItemDecoration() {
            mSpacing = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16,
                    getResources().getDisplayMetrics());
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) parent.getLayoutManager();
            int columns = layoutManager.getSpanCount();
            int position = parent.getChildLayoutPosition(view);

            outRect.right = mSpacing;
            outRect.bottom = mSpacing;

            // Add top spacing for items in first row.
            if (position < columns) {
                outRect.top = mSpacing;
            }

            // Add left spacing for items in the first column.
            if (position % columns == 0) {
                outRect.left = mSpacing;
            }
        }
    }

    private ReportAdapter mReportAdapter;

    private AppBarLayout mAppBarLayout;
    private AbstractReport mCurrentMenuReport;
    private ViewGroup mCurrentMenuReportView;

    private ComboLineColumnChartView mFullScreenChart;
    private View mFullScreenChartHolder;
    private Animator mFullScreenChartAnimator;
    private ComboLineColumnChartView mCurrentFullScreenChart;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_report, container, false);

        mAppBarLayout = (AppBarLayout) v.findViewById(R.id.app_bar_layout);

        RecyclerView recyclerView = (RecyclerView) v.findViewById(R.id.list);
        int orientation = getResources().getConfiguration().orientation;
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(
                orientation == Configuration.ORIENTATION_LANDSCAPE ? 2 : 1,
                StaggeredGridLayoutManager.VERTICAL));
        mReportAdapter = new ReportAdapter();
        recyclerView.setAdapter(mReportAdapter);
        recyclerView.addItemDecoration(new ReportItemDecoration());

        FloatingActionMenu fab = (FloatingActionMenu) v.findViewById(R.id.fab);
        FloatingActionButtonRevealer.setup(fab, recyclerView);

        mFullScreenChart = (ComboLineColumnChartView) v.findViewById(R.id.full_screen_chart);
        mFullScreenChart.setOnValueTouchListener(new ComboLineColumnChartOnValueSelectListener() {
            @Override
            public void onColumnValueSelected(int columnIndex, int subcolumnIndex, SubcolumnValue value) {
                if (value instanceof AbstractReportChartColumnData.SubcolumnValueWithTooltip) {
                    String tooltip = ((AbstractReportChartColumnData.SubcolumnValueWithTooltip) value).getTooltip();
                    Snackbar.make(v, tooltip, Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onPointValueSelected(int lineIndex, int pointIndex, PointValue value) {
                if (value instanceof AbstractReportChartLineData.PointValueWithTooltip) {
                    String tooltip = ((AbstractReportChartLineData.PointValueWithTooltip) value).getTooltip();
                    Snackbar.make(v, tooltip, Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onValueDeselected() {
            }
        });

        mFullScreenChartHolder = v.findViewById(R.id.full_screen_chart_holder);

        View btnCloseFullScreen = v.findViewById(R.id.btn_close_full_screen);
        btnCloseFullScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideFullScreenChart();
            }
        });

        getLoaderManager().initLoader(0, null, this);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        MainActivity.setSupportActionBar(this);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        ReportChartOptions options = loadReportChartOptions(getContext(), mCurrentMenuReport);
        if (item.getItemId() == R.id.menu_show_trend) {
            options.setShowTrend(!item.isChecked());
        } else if (item.getItemId() == R.id.menu_show_overall_trend) {
            options.setShowOverallTrend(!item.isChecked());
        } else if (item.getGroupId() == R.id.group_graph) {
            options.setChartOption(item.getOrder());
        }

        saveReportChartOptions(getContext(), mCurrentMenuReport, options);
        ComboLineColumnChartView chart = (ComboLineColumnChartView) mCurrentMenuReportView
                .findViewById(R.id.chart);
        chart.setComboLineColumnChartData(mCurrentMenuReport.getChartData(options));
        applyViewport(chart, true);

        return true;
    }

    @Override
    public Loader<List<AbstractReport>> onCreateLoader(int id, Bundle args) {
        return new ReportLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<List<AbstractReport>> loader, List<AbstractReport> data) {
        mReportAdapter.setItems(data);
    }

    @Override
    public void onLoaderReset(Loader<List<AbstractReport>> loader) {
        mReportAdapter.setItems(null);
    }

    private void hideFullScreenChart() {
        if (mFullScreenChartAnimator != null) {
            mFullScreenChartAnimator.cancel();
        }

        // Animate the four positioning/sizing properties in parallel, back to
        // their original values.
        AnimatorSet set = new AnimatorSet();
        set.play(
                ObjectAnimator.ofFloat(mFullScreenChartHolder, View.X,
                        mCurrentFullScreenStartBounds.left))
                .with(ObjectAnimator.ofFloat(mFullScreenChartHolder, View.Y,
                        mCurrentFullScreenStartBounds.top))
                .with(ObjectAnimator.ofFloat(mFullScreenChartHolder, View.SCALE_X,
                        mCurrentFullScreenStartScaleX))
                .with(ObjectAnimator.ofFloat(mFullScreenChartHolder, View.SCALE_Y,
                        mCurrentFullScreenStartScaleY));
        set.setDuration(getResources().getInteger(
                android.R.integer.config_longAnimTime));
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mAppBarLayout.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mCurrentFullScreenChart.setVisibility(View.VISIBLE);
                mCurrentFullScreenChart = null;
                mFullScreenChartHolder.setVisibility(View.GONE);
                mFullScreenChartAnimator = null;
            }
        });
        set.start();
        mFullScreenChartAnimator = set;
    }

    private void showFullScreenChart(AbstractReport report, ComboLineColumnChartView v) {
        if (getView() == null) {
            return;
        }

        if (mFullScreenChartAnimator != null) {
            mFullScreenChartAnimator.cancel();
        }

        mCurrentFullScreenChart = v;

        ReportChartOptions options = loadReportChartOptions(getContext(), report);
        mFullScreenChart.setComboLineColumnChartData(report.getChartData(options));
        applyViewport(mFullScreenChart, false);

        // Calculate translation start and end point and scales.
        mCurrentFullScreenStartBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();

        mCurrentFullScreenChart.getGlobalVisibleRect(mCurrentFullScreenStartBounds);
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
        mFullScreenChartHolder.setVisibility(View.VISIBLE);

        // Set the pivot point for SCALE_X and SCALE_Y transformations to the
        // top-left corner of the zoomed-in view (the default is the center of
        // the view).
        mFullScreenChartHolder.setPivotX(0f);
        mFullScreenChartHolder.setPivotY(0f);

        // Construct and run the parallel animation of the four translation and
        // scale properties (X, Y, SCALE_X, and SCALE_Y).
        AnimatorSet set = new AnimatorSet();
        set.play(
                ObjectAnimator.ofFloat(mFullScreenChartHolder, View.X,
                        mCurrentFullScreenStartBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(mFullScreenChartHolder, View.Y,
                        mCurrentFullScreenStartBounds.top, finalBounds.top))
                .with(ObjectAnimator.ofFloat(mFullScreenChartHolder, View.SCALE_X,
                        mCurrentFullScreenStartScaleX, 1f))
                .with(ObjectAnimator.ofFloat(mFullScreenChartHolder, View.SCALE_Y,
                        mCurrentFullScreenStartScaleY, 1f));
        set.setDuration(getResources().getInteger(
                android.R.integer.config_longAnimTime));
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mFullScreenChartAnimator = null;
                mAppBarLayout.setVisibility(View.INVISIBLE);
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
        ReportChartOptions options = loadReportChartOptions(getContext(), report);

        PopupMenu popup = new PopupMenu(getActivity(), v);
        popup.inflate(R.menu.report_options);
        popup.setOnMenuItemClickListener(this);

        Menu menu = popup.getMenu();
        menu.findItem(R.id.menu_show_trend).setChecked(options.isShowTrend());
        menu.findItem(R.id.menu_show_overall_trend).setChecked(options.isShowOverallTrend());

        int[] graphOptions = report.getAvailableChartOptions();
        if (graphOptions.length >= 2) {
            for (int i = 0; i < graphOptions.length; i++) {
                MenuItem item = menu.add(R.id.group_graph, Menu.NONE, i,
                        graphOptions[i]);
                item.setChecked(i == options.getChartOption());
            }

            menu.setGroupCheckable(R.id.group_graph, true, true);
        }

        popup.show();
    }

    private static ReportChartOptions loadReportChartOptions(Context context, AbstractReport report) {
        SharedPreferences prefs = context.getSharedPreferences(ReportFragment.class.getName(),
                Context.MODE_PRIVATE);
        String reportName = report.getClass().getSimpleName();

        ReportChartOptions options = new ReportChartOptions();
        options.setShowTrend(prefs.getBoolean(reportName + "_show_trend", false));
        options.setShowOverallTrend(prefs.getBoolean(reportName + "_show_overall_trend", false));
        options.setChartOption(prefs.getInt(reportName + "_current_chart_option", 0));

        return options;
    }

    private static void saveReportChartOptions(Context context, AbstractReport report, ReportChartOptions options) {
        SharedPreferences.Editor prefsEdit = context.getSharedPreferences(
                ReportFragment.class.getName(), Context.MODE_PRIVATE).edit();
        String reportName = report.getClass().getSimpleName();

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
        ComboLineColumnChartData data = (ComboLineColumnChartData) chart.getChartData();

        float leftValue = Math.max(0, data.getColumnChartData().getColumns().size() - 3 - .5f);
        for (Line line : data.getLineChartData().getLines()) {
            if (!line.getValues().isEmpty()) {
                int i = Math.max(0, line.getValues().size() - 10);
                leftValue = Math.max(leftValue, line.getValues().get(i).getX());
            }
        }

        Viewport tempViewport = new Viewport(chart.getMaximumViewport());
        tempViewport.left = leftValue;
        chart.setCurrentViewport(tempViewport);
        if (fix) {
            chart.setMaximumViewport(tempViewport);
        }
    }
}