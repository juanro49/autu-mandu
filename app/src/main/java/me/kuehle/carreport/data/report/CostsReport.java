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
import android.support.v4.util.LongSparseArray;
import android.text.format.DateFormat;

import org.joda.time.DateTime;
import org.joda.time.Seconds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.data.balancing.BalancedRefuelingCursor;
import me.kuehle.carreport.data.balancing.RefuelingBalancer;
import me.kuehle.carreport.provider.car.CarCursor;
import me.kuehle.carreport.provider.car.CarSelection;
import me.kuehle.carreport.provider.othercost.OtherCostCursor;
import me.kuehle.carreport.provider.othercost.OtherCostSelection;
import me.kuehle.carreport.util.Recurrences;
import me.kuehle.chartlib.axis.AxisLabelFormatter;
import me.kuehle.chartlib.chart.Chart;
import me.kuehle.chartlib.data.Dataset;
import me.kuehle.chartlib.data.Series;
import me.kuehle.chartlib.renderer.BarRenderer;
import me.kuehle.chartlib.renderer.LineRenderer;
import me.kuehle.chartlib.renderer.RendererList;

public class CostsReport extends AbstractReport {
    private class ReportGraphData extends AbstractReportGraphData {
        private int mOption;

        public ReportGraphData(Context context, String carName, int carColor, int option) {
            super(context, carName, carColor);
            this.mOption = option;
        }

        public void add(DateTime date, double costs) {
            if (mOption == GRAPH_OPTION_MONTH) {
                date = new DateTime(date.getYear(), date.getMonthOfYear(), 1,
                        0, 0);
            } else {
                date = new DateTime(date.getYear(), 1, 1, 0, 0);
            }

            int index = xValues.indexOf(date.getMillis());
            if (index == -1) {
                xValues.add(date.getMillis());
                yValues.add(costs);
            } else {
                yValues.set(index, yValues.get(index) + costs);
            }
        }

        @Override
        public AbstractReportGraphData createOverallTrendData() {
            if (size() == 0) {
                return super.createOverallTrendData();
            }

            long lastX = xValues.lastElement();
            xValues.remove(xValues.size() - 1);
            double lastY = yValues.lastElement();
            yValues.remove(yValues.size() - 1);
            AbstractReportGraphData data = super.createOverallTrendData();
            xValues.add(lastX);
            yValues.add(lastY);
            return data;
        }
    }

    public static final int GRAPH_OPTION_MONTH = 0;
    public static final int GRAPH_OPTION_YEAR = 1;

    private static final long[] SEC_PER_PERIOD = {
            1000l * 60l * 60l * 24l * 30l, // Month
            1000l * 60l * 60l * 24l * 365l // Year
    };

    private LongSparseArray<ReportGraphData> mCostsPerMonth = new LongSparseArray<>();
    private LongSparseArray<ReportGraphData> mCostsPerYear = new LongSparseArray<>();
    private String[] mXLabelFormat = new String[2];
    private int mVisibleBarCount;

    public CostsReport(Context context) {
        super(context);
    }

    @Override
    public int[] getAvailableChartOptions() {
        int[] options = new int[2];
        options[GRAPH_OPTION_MONTH] = R.string.report_graph_month_history;
        options[GRAPH_OPTION_YEAR] = R.string.report_graph_year_history;
        return options;
    }

    @Override
    public String getTitle() {
        return mContext.getString(R.string.report_title_costs);
    }

    @Override
    protected Chart onGetChart(boolean zoomable, boolean moveable) {
        final Dataset dataset = new Dataset();
        RendererList renderers = new RendererList();
        BarRenderer renderer = new BarRenderer(mContext);
        LineRenderer trendRenderer = new LineRenderer(mContext);
        renderers.addRenderer(renderer);
        renderers.addRenderer(trendRenderer);

        int series = 0;
        for (int i = 0; i < mCostsPerMonth.size(); i++) {
            ReportGraphData data = getChartOption() == GRAPH_OPTION_MONTH
                    ? mCostsPerMonth.get(mCostsPerMonth.keyAt(i))
                    : mCostsPerYear.get(mCostsPerYear.keyAt(i));
            if (data.isEmpty()) {
                continue;
            }

            dataset.add(data.getSeries());
            data.applySeriesStyle(series, renderer);
            series++;

            if (isShowTrend()) {
                AbstractReportGraphData trendData = data.createTrendData();
                dataset.add(trendData.getSeries());
                trendData.applySeriesStyle(series, trendRenderer);
                renderers.mapSeriesToRenderer(series, trendRenderer);
                series++;
            }

            if (isShowOverallTrend()) {
                AbstractReportGraphData trendData = data.createOverallTrendData();
                dataset.add(trendData.getSeries());
                trendData.applySeriesStyle(series, trendRenderer);
                renderers.mapSeriesToRenderer(series, trendRenderer);
                series++;
            }
        }

        // Draw report
        final Chart chart = new Chart(mContext, dataset, renderers);
        applyDefaultChartStyles(chart);
        chart.setShowLegend(false);
        if (isShowTrend()) {
            for (int i = 1; i < dataset.size(); i += 2) {
                chart.getLegend().setSeriesVisible(i, false);
            }
        }
        double[] xValues = getXValues(dataset);
        chart.getDomainAxis().setLabels(xValues);
        chart.getDomainAxis().setLabelFormatter(new AxisLabelFormatter() {
            @Override
            public String formatLabel(double value) {
                DateTime date = new DateTime((long) value);
                return date.toString(mXLabelFormat[getChartOption()]);
            }
        });
        double padding = SEC_PER_PERIOD[getChartOption()] / 2;
        double topBound = dataset.maxX();
        double bottomBound = topBound
                - (SEC_PER_PERIOD[getChartOption()] * Math.min(
                mVisibleBarCount - 1, xValues.length - 1));
        chart.getDomainAxis().setDefaultBottomBound(bottomBound - padding);
        chart.getDomainAxis().setDefaultTopBound(topBound + padding);
        chart.getRangeAxis().setDefaultBottomBound(0);
        chart.getDomainAxis().setZoomable(zoomable);
        chart.getDomainAxis().setMovable(moveable);
        chart.getRangeAxis().setZoomable(zoomable);
        chart.getRangeAxis().setMovable(moveable);

        return chart;
    }

    @Override
    protected Cursor[] onUpdate() {
        Preferences prefs = new Preferences(mContext);
        String unit = prefs.getUnitCurrency();

        // Settings, which are based on the screen size.
        if (mContext.getResources().getConfiguration().smallestScreenWidthDp > 480) {
            mXLabelFormat[GRAPH_OPTION_MONTH] = "MMMM, yyyy";
            mXLabelFormat[GRAPH_OPTION_YEAR] = "yyyy";
            mVisibleBarCount = 4;
        } else {
            mXLabelFormat[GRAPH_OPTION_MONTH] = "MMM, yyyy";
            mXLabelFormat[GRAPH_OPTION_YEAR] = "yyyy";
            mVisibleBarCount = 3;
        }

        ArrayList<Cursor> cursors = new ArrayList<>();

        RefuelingBalancer balancer = new RefuelingBalancer(mContext);
        CarCursor car = new CarSelection().query(mContext.getContentResolver());
        cursors.add(car);
        while (car.moveToNext()) {
            Section section;
            if (car.getSuspendedSince() != null) {
                section = addDataSection(String.format("%s [%s]", car.getName(),
                        mContext.getString(R.string.suspended)), car.getColor(), 1);
            } else {
                section = addDataSection(car.getName(), car.getColor());
            }

            mCostsPerMonth.put(car.getId(), new ReportGraphData(mContext, car.getName(),
                    car.getColor(), GRAPH_OPTION_MONTH));
            mCostsPerYear.put(car.getId(), new ReportGraphData(mContext, car.getName(),
                    car.getColor(), GRAPH_OPTION_YEAR));

            int startMileage = Integer.MAX_VALUE;
            int endMileage = Integer.MIN_VALUE;
            DateTime startDate = new DateTime();
            DateTime endDate;
            if (car.getSuspendedSince() != null) {
                endDate = new DateTime(car.getSuspendedSince());
            } else {
                endDate = new DateTime();
            }

            BalancedRefuelingCursor refueling = balancer.getBalancedRefuelings(car.getId());
            cursors.add(refueling);
            OtherCostCursor otherCost = new OtherCostSelection().carId(car.getId()).query(mContext.getContentResolver());
            cursors.add(otherCost);

            if ((refueling.getCount() + otherCost.getCount()) < 2) {
                section.addItem(new Item(mContext.getString(R.string.report_not_enough_data), ""));
                continue;
            }

            double totalCosts = 0;

            while (refueling.moveToNext()) {
                totalCosts += refueling.getPrice();

                DateTime date = new DateTime(refueling.getDate());
                mCostsPerMonth.get(car.getId()).add(date, refueling.getPrice());
                mCostsPerYear.get(car.getId()).add(date, refueling.getPrice());

                startMileage = Math.min(startMileage, refueling.getMileage());
                endMileage = Math.max(endMileage, refueling.getMileage());
                if (startDate.isAfter(date)) {
                    startDate = date;
                }
            }

            while (otherCost.moveToNext()) {
                int recurrences;
                if (otherCost.getEndDate() == null ||
                        new DateTime(otherCost.getEndDate()).isAfterNow()) {
                    recurrences = Recurrences.getRecurrencesSince(
                            otherCost.getRecurrenceInterval(),
                            otherCost.getRecurrenceMultiplier(),
                            otherCost.getDate());
                } else {
                    recurrences = Recurrences.getRecurrencesBetween(
                            otherCost.getRecurrenceInterval(),
                            otherCost.getRecurrenceMultiplier(),
                            otherCost.getDate(),
                            otherCost.getEndDate());
                }

                totalCosts += otherCost.getPrice() * recurrences;

                DateTime date = new DateTime(otherCost.getDate());
                DateTime recurrenceEndDate;
                if (otherCost.getEndDate() != null && endDate.isAfter(otherCost.getEndDate().getTime())) {
                    recurrenceEndDate = new DateTime(otherCost.getEndDate());
                } else {
                    recurrenceEndDate = endDate;
                }

                while (date.isBefore(recurrenceEndDate)) {
                    mCostsPerMonth.get(car.getId()).add(date, otherCost.getPrice());
                    mCostsPerYear.get(car.getId()).add(date, otherCost.getPrice());
                    switch (otherCost.getRecurrenceInterval()) {
                        case ONCE:
                            // Set date after now, so the loop ends.
                            date = DateTime.now().plusYears(1);
                            break;
                        case DAY:
                            date = date.plusDays(otherCost.getRecurrenceMultiplier());
                            break;
                        case MONTH:
                            date = date.plusMonths(otherCost.getRecurrenceMultiplier());
                            break;
                        case QUARTER:
                            date = date.plusMonths(otherCost.getRecurrenceMultiplier() * 3);
                            break;
                        case YEAR:
                            date = date.plusYears(otherCost.getRecurrenceMultiplier());
                            break;
                    }
                }

                if (otherCost.getMileage() != null && otherCost.getMileage() > -1) {
                    startMileage = Math.min(startMileage, otherCost.getMileage());
                    endMileage = Math.max(endMileage, otherCost.getMileage());
                }

                if (startDate.isAfter(otherCost.getDate().getTime())) {
                    startDate = new DateTime(otherCost.getDate());
                }
            }

            // Calculate averages
            Seconds elapsedSeconds = Seconds.secondsBetween(startDate, endDate);
            double costsPerSecond = totalCosts / elapsedSeconds.getSeconds();

            // Average costs per day
            // 60 seconds per minute * 60 minutes per hour * 24 hours per day =
            // 86400 seconds per day
            section.addItem(new Item("\u00D8 " + mContext.getString(R.string.report_day),
                    String.format("%.2f %s", costsPerSecond * 86400, unit)));

            // Average costs per month
            // 86400 seconds per day * 30,4375 days per month = 2629800 seconds
            // per month
            // (365,25 days per year means 365,25 / 12 = 30,4375 days per month)
            section.addItem(new Item("\u00D8 " + mContext.getString(R.string.report_month),
                    String.format("%.2f %s", costsPerSecond * 2629800, unit)));

            // Average costs per year
            // 86400 seconds per day * 365,25 days per year = 31557600 seconds
            // per year
            section.addItem(new Item("\u00D8 " + mContext.getString(R.string.report_year),
                    String.format("%.2f %s", costsPerSecond * 31557600, unit)));

            // Average costs per [distance unit]
            int mileageDiff = Math.max(1, endMileage - startMileage);
            section.addItem(new Item("\u00D8 " + prefs.getUnitDistance(),
                    String.format("%.2f %s", totalCosts / mileageDiff, unit)));

            // Total costs
            section.addItem(new Item(mContext.getString(R.string.report_since,
                    DateFormat.getDateFormat(mContext).format(startDate.toDate())),
                    String.format("%.2f %s", totalCosts, unit)));
        }

        return cursors.toArray(new Cursor[cursors.size()]);
    }

    private double[] getXValues(Dataset dataset) {
        HashSet<Double> values = new HashSet<>();
        for (int s = 0; s < dataset.size(); s++) {
            Series series = dataset.get(s);
            for (int p = 0; p < series.size(); p++) {
                values.add(series.get(p).x);
            }
        }
        ArrayList<Double> list = new ArrayList<>(values);
        Collections.sort(list);

        double[] arrValues = new double[list.size()];
        for (int i = 0; i < arrValues.length; i++) {
            arrValues[i] = list.get(i);
        }

        return arrValues;
    }
}
