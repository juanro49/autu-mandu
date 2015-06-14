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

package me.kuehle.carreport.data.report;

import android.content.Context;
import android.database.Cursor;
import android.text.format.DateFormat;
import android.widget.Toast;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Vector;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.data.balancing.BalancedRefuelingCursor;
import me.kuehle.carreport.data.balancing.RefuelingBalancer;
import me.kuehle.carreport.data.query.CarQueries;
import me.kuehle.carreport.provider.car.CarCursor;
import me.kuehle.carreport.provider.car.CarSelection;
import me.kuehle.carreport.util.Calculator;
import me.kuehle.chartlib.chart.Chart;
import me.kuehle.chartlib.data.Dataset;
import me.kuehle.chartlib.data.Series;
import me.kuehle.chartlib.renderer.LineRenderer;
import me.kuehle.chartlib.renderer.OnClickListener;
import me.kuehle.chartlib.renderer.RendererList;

public class MileageReport extends AbstractReport {
    private class ReportGraphDataAccumulated extends AbstractReportGraphData {
        private Cursor mCursor;

        public ReportGraphDataAccumulated(Context context, CarCursor car) {
            super(context, car.getName(), car.getColor());

            RefuelingBalancer balancer = new RefuelingBalancer(context);
            BalancedRefuelingCursor refueling = balancer.getBalancedRefuelings(car.getId());
            mCursor = refueling;

            boolean wasLastRefuelingGuessed = false;
            while(refueling.moveToNext()) {
                xValues.add(refueling.getDate().getTime());
                yValues.add((double) refueling.getMileage());

                if (refueling.getGuessed()) {
                    markLastPoint();
                    markLastLine();
                } else if (wasLastRefuelingGuessed) {
                    markLastLine();
                }

                wasLastRefuelingGuessed = refueling.getGuessed();
            }
        }

        public Cursor[] getUsedCursors() {
            return new Cursor[] { mCursor };
        }
    }

    private class ReportGraphDataPerRefueling extends AbstractReportGraphData {
        private Cursor mCursor;

        public ReportGraphDataPerRefueling(Context context, CarCursor car, String category) {
            super(context, String.format("%s (%s)", car.getName(), category), car.getColor());

            RefuelingBalancer balancer = new RefuelingBalancer(context);
            BalancedRefuelingCursor refueling = balancer.getBalancedRefuelings(car.getId(), category);
            mCursor = refueling;

            boolean wasLastRefuelingGuessed = false;
            int lastRefuelingMileage = -1;
            while(refueling.moveToNext()) {
                if (lastRefuelingMileage > -1) {
                    xValues.add(refueling.getDate().getTime());
                    yValues.add((double) (refueling.getMileage() - lastRefuelingMileage));

                    if (refueling.getGuessed()) {
                        markLastPoint();
                        markLastLine();
                    } else if (wasLastRefuelingGuessed) {
                        markLastLine();
                    }
                }

                wasLastRefuelingGuessed = refueling.getGuessed();
                lastRefuelingMileage = refueling.getMileage();
            }
        }

        public Cursor[] getUsedCursors() {
            return new Cursor[] { mCursor };
        }
    }

    private class ReportGraphDataPerMonth extends AbstractReportGraphData {
        private Cursor mCursor;

        public ReportGraphDataPerMonth(Context context, CarCursor car) {
            super(context, car.getName(), car.getColor());

            RefuelingBalancer balancer = new RefuelingBalancer(context);
            BalancedRefuelingCursor refueling = balancer.getBalancedRefuelings(car.getId());
            mCursor = refueling;

            int lastRefuelingMileage = -1;
            while(refueling.moveToNext()) {
                if (lastRefuelingMileage > -1) {
                    long x = getMonthTime(refueling.getDate().getTime());
                    double y = (double) (refueling.getMileage() - lastRefuelingMileage);

                    int xIndex = xValues.indexOf(x);
                    if (xIndex == -1) {
                        xValues.add(x);
                        yValues.add(y);
                    } else {
                        yValues.set(xIndex, yValues.get(xIndex) + y);
                    }
                }

                lastRefuelingMileage = refueling.getMileage();
            }
        }

        public Cursor[] getUsedCursors() {
            return new Cursor[] { mCursor };
        }

        private long getMonthTime(long time) {
            DateTime date = new DateTime(time);
            date = new DateTime(date.getYear(), date.getMonthOfYear(), 1, 0, 0);

            return date.getMillis();
        }
    }

    public static final int GRAPH_OPTION_ACCUMULATED = 0;
    public static final int GRAPH_OPTION_PER_REFUELING = 1;
    public static final int GRAPH_OPTION_PER_MONTH = 2;

    private Vector<AbstractReportGraphData> reportDataAccumulated = new Vector<>();
    private Vector<AbstractReportGraphData> reportDataPerRefueling = new Vector<>();
    private Vector<AbstractReportGraphData> reportDataPerMonth = new Vector<>();
    private double[] minXValue = {Long.MAX_VALUE, Long.MAX_VALUE,
            Long.MAX_VALUE};

    private String unit;

    public MileageReport(Context context) {
        super(context);
    }

    @Override
    public int[] getAvailableChartOptions() {
        int[] options = new int[3];
        options[GRAPH_OPTION_ACCUMULATED] = R.string.report_graph_accumulated;
        options[GRAPH_OPTION_PER_REFUELING] = R.string.report_graph_per_refueling;
        options[GRAPH_OPTION_PER_MONTH] = R.string.report_graph_per_month;
        return options;
    }

    @Override
    public String getTitle() {
        return mContext.getString(R.string.report_title_mileage);
    }

    @Override
    protected Chart onGetChart(boolean zoomable, boolean moveable) {
        final Dataset dataset = new Dataset();
        RendererList renderers = new RendererList();
        LineRenderer renderer = new LineRenderer(mContext);
        renderers.addRenderer(renderer);

        Vector<AbstractReportGraphData> reportData;
        if (getChartOption() == GRAPH_OPTION_ACCUMULATED) {
            reportData = reportDataAccumulated;
        } else if (getChartOption() == GRAPH_OPTION_PER_REFUELING) {
            reportData = reportDataPerRefueling;
        } else {
            reportData = reportDataPerMonth;
        }
        Vector<AbstractReportGraphData> chartReportData = new Vector<>();

        if (isShowTrend()) {
            for (AbstractReportGraphData data : reportData) {
                chartReportData.add(data.createTrendData());
            }
        }

        if (isShowOverallTrend()) {
            for (AbstractReportGraphData data : reportData) {
                chartReportData.add(data.createOverallTrendData());
            }
        }

        chartReportData.addAll(reportData);

        for (int i = 0; i < chartReportData.size(); i++) {
            dataset.add(chartReportData.get(i).getSeries());
            chartReportData.get(i).applySeriesStyle(i, renderer);
        }

        renderer.setOnClickListener(new OnClickListener() {
            @Override
            public void onSeriesClick(int series, int point, boolean marked) {
                Series s = dataset.get(series);
                String car = s.getTitle();
                String date = DateFormat.getDateFormat(mContext).format(
                        new Date((long) s.get(point).x));
                Toast.makeText(
                        mContext,
                        String.format(
                                "%s: %s\n%s: %.0f %s\n%s: %s",
                                mContext.getString(R.string.report_toast_car),
                                car,
                                mContext.getString(R.string.report_toast_mileage),
                                s.get(point).y, unit, mContext
                                        .getString(R.string.report_toast_date),
                                date), Toast.LENGTH_LONG).show();
            }
        });

        final Chart chart = new Chart(mContext, dataset, renderers);
        applyDefaultChartStyles(chart);
        chart.setShowLegend(false);
        if (isShowTrend()) {
            for (int i = 0; i < chartReportData.size() / 2; i++) {
                chart.getLegend().setSeriesVisible(i, false);
            }
        }
        chart.getDomainAxis().setLabelFormatter(mDateLabelFormatter);
        chart.getDomainAxis()
                .setDefaultBottomBound(minXValue[getChartOption()]);
        chart.getDomainAxis().setZoomable(zoomable);
        chart.getDomainAxis().setMovable(moveable);
        chart.getRangeAxis().setZoomable(zoomable);
        chart.getRangeAxis().setMovable(moveable);

        return chart;
    }

    @Override
    protected Cursor[] onUpdate() {
        // Preferences
        Preferences prefs = new Preferences(mContext);
        unit = prefs.getUnitDistance();

        ArrayList<Cursor> cursors = new ArrayList<>();

        // Car data
        CarCursor car = new CarSelection().query(mContext.getContentResolver());
        cursors.add(car);
        while (car.moveToNext()) {
            // Accumulated data
            ReportGraphDataAccumulated carDataAccumulated = new ReportGraphDataAccumulated(
                    mContext, car);
            cursors.addAll(Arrays.asList(carDataAccumulated.getUsedCursors()));
            if (carDataAccumulated.size() > 0) {
                reportDataAccumulated.add(carDataAccumulated);
                minXValue[GRAPH_OPTION_ACCUMULATED] = Math.min(
                        minXValue[GRAPH_OPTION_ACCUMULATED], carDataAccumulated
                                .getSeries().minX());
            }

            // Per refueling data
            String[] categories = CarQueries.getUsedFuelTypeCategories(mContext, car.getId());
            for (String category : categories) {
                ReportGraphDataPerRefueling carDataPerRefueling = new ReportGraphDataPerRefueling(
                        mContext, car, category);
                cursors.addAll(Arrays.asList(carDataPerRefueling.getUsedCursors()));

                // Add section for car
                Section section;
                if (car.getSuspendedSince() != null) {
                    section = addDataSection(String.format("%s (%s) [%s]", car.getName(), category,
                            mContext.getString(R.string.suspended)), car.getColor(), 1);
                } else {
                    section = addDataSection(String.format("%s (%s)", car.getName(), category),
                            car.getColor());
                }

                if (carDataPerRefueling.size() == 0) {
                    section.addItem(new Item(mContext
                            .getString(R.string.report_not_enough_data), ""));
                } else {
                    reportDataPerRefueling.add(carDataPerRefueling);
                    minXValue[GRAPH_OPTION_PER_REFUELING] = Math.min(
                            minXValue[GRAPH_OPTION_PER_REFUELING],
                            carDataPerRefueling.getSeries().minX());

                    Double[] carYValues = carDataPerRefueling.yValues.toArray(
                            new Double[carDataPerRefueling.yValues.size()]);
                    section.addItem(new Item(mContext.getString(R.string.report_highest),
                            String.format("%d %s", Calculator.max(carYValues).intValue(), unit)));
                    section.addItem(new Item(mContext.getString(R.string.report_lowest),
                            String.format("%d %s", Calculator.min(carYValues).intValue(), unit)));
                    section.addItem(new Item(mContext.getString(R.string.report_average),
                            String.format("%d %s", Calculator.avg(carYValues).intValue(), unit)));
                }
            }

            // Per month data
            ReportGraphDataPerMonth carDataPerMonth = new ReportGraphDataPerMonth(mContext, car);
            cursors.addAll(Arrays.asList(carDataPerMonth.getUsedCursors()));
            if (carDataPerMonth.size() > 0) {
                reportDataPerMonth.add(carDataPerMonth);
                minXValue[GRAPH_OPTION_PER_MONTH] = Math.min(minXValue[GRAPH_OPTION_PER_MONTH],
                        carDataPerMonth.getSeries().minX());
            }
        }

        return cursors.toArray(new Cursor[cursors.size()]);
    }
}
