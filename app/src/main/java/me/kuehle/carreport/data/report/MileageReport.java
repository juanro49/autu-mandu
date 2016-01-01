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

import org.joda.time.DateTime;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.data.balancing.BalancedRefuelingCursor;
import me.kuehle.carreport.data.balancing.RefuelingBalancer;
import me.kuehle.carreport.data.query.CarQueries;
import me.kuehle.carreport.provider.car.CarColumns;
import me.kuehle.carreport.provider.car.CarCursor;
import me.kuehle.carreport.provider.car.CarSelection;
import me.kuehle.carreport.util.Calculator;

public class MileageReport extends AbstractReport {
    private class ReportChartDataAccumulated extends AbstractReportChartLineData {
        private Cursor mCursor;

        public ReportChartDataAccumulated(Context context, CarCursor car) {
            super(context, car.getName(), car.getColor());

            RefuelingBalancer balancer = new RefuelingBalancer(context);
            BalancedRefuelingCursor refueling = balancer.getBalancedRefuelings(car.getId());
            mCursor = refueling;

            while (refueling.moveToNext()) {
                String tooltip = mContext.getString(R.string.report_toast_mileage,
                        car.getName(),
                        refueling.getMileage(),
                        mUnit,
                        formatXValue(refueling.getDate().getTime(), GRAPH_OPTION_ACCUMULATED));
                if (refueling.getGuessed()) {
                    tooltip += "\n" + mContext.getString(R.string.report_toast_guessed);
                }

                add((float) refueling.getDate().getTime(),
                        (float) refueling.getMileage(),
                        tooltip,
                        refueling.getGuessed());
            }
        }

        public Cursor[] getUsedCursors() {
            return new Cursor[]{mCursor};
        }
    }

    private class ReportChartDataPerRefueling extends AbstractReportChartLineData {
        private Cursor mCursor;

        public ReportChartDataPerRefueling(Context context, CarCursor car, String category) {
            super(context, String.format("%s (%s)", car.getName(), category), car.getColor());

            RefuelingBalancer balancer = new RefuelingBalancer(context);
            BalancedRefuelingCursor refueling = balancer.getBalancedRefuelings(car.getId(), category);
            mCursor = refueling;

            int lastRefuelingMileage = -1;
            while (refueling.moveToNext()) {
                if (lastRefuelingMileage > -1) {
                    int mileageDiff = refueling.getMileage() - lastRefuelingMileage;
                    String tooltip = mContext.getString(R.string.report_toast_mileage,
                            car.getName(),
                            mileageDiff,
                            mUnit,
                            formatXValue(refueling.getDate().getTime(), GRAPH_OPTION_PER_REFUELING));
                    if (refueling.getGuessed()) {
                        tooltip += "\n" + mContext.getString(R.string.report_toast_guessed);
                    }

                    add((float) refueling.getDate().getTime(),
                            (float) mileageDiff,
                            tooltip,
                            refueling.getGuessed());
                }

                lastRefuelingMileage = refueling.getMileage();
            }
        }

        public Cursor[] getUsedCursors() {
            return new Cursor[]{mCursor};
        }
    }

    private class ReportChartDataPerMonth extends AbstractReportChartColumnData {
        private Cursor mCursor;

        public ReportChartDataPerMonth(Context context, CarCursor car) {
            super(context, car.getName(), car.getColor());

            RefuelingBalancer balancer = new RefuelingBalancer(context);
            BalancedRefuelingCursor refueling = balancer.getBalancedRefuelings(car.getId());
            mCursor = refueling;

            int lastRefuelingMileage = -1;
            while (refueling.moveToNext()) {
                if (lastRefuelingMileage > -1) {
                    DateTime date = new DateTime(refueling.getDate());
                    float x = date.getYear() * 100 + date.getMonthOfYear();
                    float y = refueling.getMileage() - lastRefuelingMileage;

                    int xIndex = indexOf(x);
                    if (xIndex == -1) {
                        add(x, y, mContext.getString(R.string.report_toast_mileage_month,
                                car.getName(), y, mUnit, formatXValue(x, GRAPH_OPTION_PER_MONTH)));
                    } else {
                        y += getYValues().get(xIndex);
                        set(xIndex, x, y, mContext.getString(R.string.report_toast_mileage_month,
                                car.getName(), y, mUnit, formatXValue(x, GRAPH_OPTION_PER_MONTH)));
                    }
                }

                lastRefuelingMileage = refueling.getMileage();
            }
        }

        public Cursor[] getUsedCursors() {
            return new Cursor[]{mCursor};
        }
    }

    public static final int GRAPH_OPTION_ACCUMULATED = 0;
    public static final int GRAPH_OPTION_PER_REFUELING = 1;
    public static final int GRAPH_OPTION_PER_MONTH = 2;

    private List<AbstractReportChartData> reportDataAccumulated = new ArrayList<>();
    private List<AbstractReportChartData> reportDataPerRefueling = new ArrayList<>();
    private List<AbstractReportChartData> reportDataPerMonth = new ArrayList<>();

    private String mUnit;
    private DateFormat mDateFormat;
    private String mMonthLabelFormat;

    public MileageReport(Context context) {
        super(context);
    }

    @Override
    protected String formatXValue(float value, int chartOption) {
        if (chartOption == GRAPH_OPTION_PER_MONTH) {
            int dateValue = (int) value;
            int year = dateValue / 100;
            int month = dateValue % 100;
            DateTime date = new DateTime(year, month, 1, 0, 0);
            return date.toString(mMonthLabelFormat);
        } else {
            return mDateFormat.format(new Date((long) value));
        }
    }

    @Override
    protected String formatYValue(float value, int chartOption) {
        int rounded = (int) (value + .5);
        if (rounded > 1000) {
            return String.format("%dk", rounded / 1000);
        } else {
            return String.valueOf(rounded);
        }
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
    protected List<AbstractReportChartData> getRawChartData(int chartOption) {
        if (chartOption == GRAPH_OPTION_ACCUMULATED) {
            return reportDataAccumulated;
        } else if (chartOption == GRAPH_OPTION_PER_REFUELING) {
            return reportDataPerRefueling;
        } else {
            return reportDataPerMonth;
        }
    }

    @Override
    protected Cursor[] onUpdate() {
        // Preferences
        Preferences prefs = new Preferences(mContext);
        mUnit = prefs.getUnitDistance();
        mDateFormat = android.text.format.DateFormat.getDateFormat(mContext);
        if (mContext.getResources().getConfiguration().smallestScreenWidthDp > 480) {
            mMonthLabelFormat = "MMMM yyyy";
        } else {
            mMonthLabelFormat = "MMM yyyy";
        }

        ArrayList<Cursor> cursors = new ArrayList<>();

        // Car data
        CarCursor car = new CarSelection().query(mContext.getContentResolver(), null, CarColumns.NAME + " COLLATE UNICODE");
        cursors.add(car);
        while (car.moveToNext()) {
            // Accumulated data
            ReportChartDataAccumulated carDataAccumulated = new ReportChartDataAccumulated(
                    mContext, car);
            cursors.addAll(Arrays.asList(carDataAccumulated.getUsedCursors()));
            if (carDataAccumulated.size() > 0) {
                reportDataAccumulated.add(carDataAccumulated);
            }

            // Per refueling data
            String[] categories = CarQueries.getUsedFuelTypeCategories(mContext, car.getId());
            for (String category : categories) {
                ReportChartDataPerRefueling carDataPerRefueling = new ReportChartDataPerRefueling(
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

                    Float[] carYValues = carDataPerRefueling.getYValues().toArray(
                            new Float[carDataPerRefueling.size()]);
                    section.addItem(new Item(mContext.getString(R.string.report_highest),
                            String.format("%d %s", Calculator.max(carYValues).intValue(), mUnit)));
                    section.addItem(new Item(mContext.getString(R.string.report_lowest),
                            String.format("%d %s", Calculator.min(carYValues).intValue(), mUnit)));
                    section.addItem(new Item(mContext.getString(R.string.report_average),
                            String.format("%d %s", Calculator.avg(carYValues).intValue(), mUnit)));
                }
            }

            // Per month data
            ReportChartDataPerMonth carDataPerMonth = new ReportChartDataPerMonth(mContext, car);
            cursors.addAll(Arrays.asList(carDataPerMonth.getUsedCursors()));
            if (carDataPerMonth.size() > 0) {
                reportDataPerMonth.add(carDataPerMonth);
            }
        }

        return cursors.toArray(new Cursor[cursors.size()]);
    }
}
