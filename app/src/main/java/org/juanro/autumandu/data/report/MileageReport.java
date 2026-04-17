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

package org.juanro.autumandu.data.report;

import android.content.Context;

import org.joda.time.DateTime;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;
import org.juanro.autumandu.model.AutuManduDatabase;
import org.juanro.autumandu.model.dto.BalancedRefueling;
import org.juanro.autumandu.model.dto.RefuelingWithDetails;
import org.juanro.autumandu.model.entity.Car;
import org.juanro.autumandu.util.Calculator;

public class MileageReport extends AbstractReport {
    private class ReportChartDataAccumulated extends AbstractReportChartLineData {
        public ReportChartDataAccumulated(Context context, Car car, List<BalancedRefueling> refuelings) {
            super(context, car.getName(), car.getColor());

            for (BalancedRefueling refueling : refuelings) {
                String tooltip = mContext.getString(R.string.report_toast_mileage,
                        car.getName(),
                        refueling.getMileage(),
                        mUnit,
                        formatXValue(ReportDateHelper.toFloat(refueling.getDate()),
                                GRAPH_OPTION_ACCUMULATED));
                if (refueling.isGuessed()) {
                    tooltip += "\n" + mContext.getString(R.string.report_toast_guessed);
                }

                add(ReportDateHelper.toFloat(refueling.getDate()),
                        (float) refueling.getMileage(),
                        tooltip,
                        refueling.isGuessed());
            }
        }
    }

    private class ReportChartDataPerRefueling extends AbstractReportChartLineData {
        public ReportChartDataPerRefueling(Context context, Car car, String category, List<BalancedRefueling> refuelings) {
            super(context, String.format("%s (%s)", car.getName(), category), car.getColor());

            int lastRefuelingMileage = -1;
            for (BalancedRefueling refueling : refuelings) {
                if (lastRefuelingMileage > -1) {
                    int mileageDiff = refueling.getMileage() - lastRefuelingMileage;
                    String tooltip = mContext.getString(R.string.report_toast_mileage,
                            car.getName(),
                            mileageDiff,
                            mUnit,
                            formatXValue(ReportDateHelper.toFloat(refueling.getDate()),
                                    GRAPH_OPTION_PER_REFUELING));
                    if (refueling.isGuessed()) {
                        tooltip += "\n" + mContext.getString(R.string.report_toast_guessed);
                    }

                    add(ReportDateHelper.toFloat(refueling.getDate()),
                            (float) mileageDiff,
                            tooltip,
                            refueling.isGuessed());
                }

                lastRefuelingMileage = refueling.getMileage();
            }
        }
    }

    private class ReportChartDataPerMonth extends AbstractReportChartColumnData {
        public ReportChartDataPerMonth(Context context, Car car, List<BalancedRefueling> refuelings) {
            super(context, car.getName(), car.getColor());

            int lastRefuelingMileage = -1;
            for (BalancedRefueling refueling : refuelings) {
                if (lastRefuelingMileage > -1) {
                    DateTime date = new DateTime(refueling.getDate());
                    float x = date.getYear() * 12 + date.getMonthOfYear() - 1;
                    float y = refueling.getMileage() - lastRefuelingMileage;

                    int xIndex = indexOf(x);
                    if (xIndex == -1) {
                        add(x, y, mContext.getString(R.string.report_toast_mileage_month,
                                car.getName(), (double) y, mUnit, formatXValue(x, GRAPH_OPTION_PER_MONTH)));
                    } else {
                        DataPoint dp = mDataPoints.get(xIndex);
                        dp.y += y;
                        dp.tooltip = mContext.getString(R.string.report_toast_mileage_month,
                                car.getName(), (double) dp.y, mUnit, formatXValue(x, GRAPH_OPTION_PER_MONTH));
                    }
                }

                lastRefuelingMileage = refueling.getMileage();
            }
        }
    }

    public static final int GRAPH_OPTION_ACCUMULATED = 0;
    public static final int GRAPH_OPTION_PER_REFUELING = 1;
    public static final int GRAPH_OPTION_PER_MONTH = 2;

    private final List<AbstractReportChartData> reportDataAccumulated = new ArrayList<>();
    private final List<AbstractReportChartData> reportDataPerRefueling = new ArrayList<>();
    private final List<AbstractReportChartData> reportDataPerMonth = new ArrayList<>();

    private String mUnit;
    private DateFormat mDateFormat;
    private String mMonthLabelFormat;

    public MileageReport(Context context) {
        super(context);
    }

    @Override
    public String formatXValue(float value, int chartOption) {
        if (chartOption == GRAPH_OPTION_PER_MONTH) {
            int dateValue = (int) value;
            int year = dateValue / 12;
            int month = (dateValue % 12) + 1;
            DateTime date = new DateTime(year, month, 1, 0, 0);
            return date.toString(mMonthLabelFormat);
        } else {
            return mDateFormat.format(ReportDateHelper.toDate(value));
        }
    }

    @Override
    public String formatYValue(float value, int chartOption) {
        int rounded = (int) (value + .5);
        if (rounded >= 1000) {
            return String.format(Locale.getDefault(), "%dk", rounded / 1000);
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
    public List<AbstractReportChartData> getRawChartData(int chartOption) {
        synchronized (mCachedChartData) {
            if (mCachedChartData.containsKey(chartOption)) {
                return mCachedChartData.get(chartOption);
            }

            List<AbstractReportChartData> data;
            if (chartOption == GRAPH_OPTION_ACCUMULATED) {
                data = new ArrayList<>(reportDataAccumulated);
            } else if (chartOption == GRAPH_OPTION_PER_REFUELING) {
                data = new ArrayList<>(reportDataPerRefueling);
            } else {
                data = new ArrayList<>(reportDataPerMonth);
            }

            mCachedChartData.put(chartOption, data);
            return data;
        }
    }

    @Override
    protected void onUpdate() {
        reportDataAccumulated.clear();
        reportDataPerRefueling.clear();
        reportDataPerMonth.clear();

        // Preferences
        Preferences prefs = new Preferences(mContext);
        mUnit = prefs.getUnitDistance();
        mDateFormat = android.text.format.DateFormat.getDateFormat(mContext);
        if (mContext.getResources().getConfiguration().smallestScreenWidthDp > 480) {
            mMonthLabelFormat = "MMMM yyyy";
        } else {
            mMonthLabelFormat = "MMM yyyy";
        }

        AutuManduDatabase db = AutuManduDatabase.getInstance(mContext);

        // Bulk load all data to optimize performance (N+1 avoidance)
        List<Car> cars = db.getCarDao().getAll();
        Map<Long, List<RefuelingWithDetails>> refuelingsByCar = db.getRefuelingDao().getAllWithDetails()
                .stream().collect(Collectors.groupingBy(RefuelingWithDetails::carId));

        for (Car car : cars) {
            Long carIdObj = car.getId();
            if (carIdObj == null) continue;
            long carId = carIdObj;

            // Get balanced refuelings for this car once.
            List<RefuelingWithDetails> carRefuelings = refuelingsByCar.get(carId);
            if (carRefuelings == null) carRefuelings = Collections.emptyList();

            Preferences prefsForGuess = new Preferences(mContext);
            List<BalancedRefueling> balancedRefuelings = BalancedRefueling.balance(carRefuelings, prefsForGuess.isAutoGuessMissingDataEnabled(), false);

            // Accumulated data
            ReportChartDataAccumulated carDataAccumulated = new ReportChartDataAccumulated(
                    mContext, car, balancedRefuelings);
            if (!carDataAccumulated.isEmpty()) {
                reportDataAccumulated.add(carDataAccumulated);
            }

            // Per refueling data (grouped by category)
            Map<String, List<BalancedRefueling>> refuelingsByCategory = balancedRefuelings.stream()
                    .filter(r -> r.getFuelTypeCategory() != null)
                    .collect(Collectors.groupingBy(BalancedRefueling::getFuelTypeCategory));

            for (Map.Entry<String, List<BalancedRefueling>> entry : refuelingsByCategory.entrySet()) {
                String category = entry.getKey();
                List<BalancedRefueling> categoryRefuelings = entry.getValue();

                ReportChartDataPerRefueling carDataPerRefueling = new ReportChartDataPerRefueling(
                        mContext, car, category, categoryRefuelings);

                // Add section for car
                Section section;
                if (car.getSuspendedSince() != null) {
                    section = addDataSection(String.format("%s (%s) [%s]", car.getName(), category,
                            mContext.getString(R.string.suspended)), car.getColor(), 1);
                } else {
                    section = addDataSection(String.format("%s (%s)", car.getName(), category),
                            car.getColor());
                }

                if (carDataPerRefueling.isEmpty()) {
                    section.addItem(new Item(mContext.getString(R.string.report_not_enough_data), ""));
                } else {
                    reportDataPerRefueling.add(carDataPerRefueling);

                    Float[] carYValues = carDataPerRefueling.getYValues().toArray(new Float[0]);
                    section.addItem(new Item(mContext.getString(R.string.report_highest),
                            String.format(Locale.getDefault(), "%d %s", (int) Calculator.max(carYValues), mUnit)));
                    section.addItem(new Item(mContext.getString(R.string.report_lowest),
                            String.format(Locale.getDefault(), "%d %s", (int) Calculator.min(carYValues), mUnit)));
                    section.addItem(new Item(mContext.getString(R.string.report_average),
                            String.format(Locale.getDefault(), "%d %s", (int) Calculator.avg(carYValues), mUnit)));
                }
            }

            // Per month data
            ReportChartDataPerMonth carDataPerMonth = new ReportChartDataPerMonth(mContext, car, balancedRefuelings);
            if (!carDataPerMonth.isEmpty()) {
                reportDataPerMonth.add(carDataPerMonth);
            }

            // Odometer and Recorded distance stats
            int latestMileage = db.getCarDao().getLatestMileage(carId);
            Integer earliestMileage = db.getCarDao().getEarliestMileage(carId);
            int recordedMileage = earliestMileage != null ? latestMileage - earliestMileage : 0;

            Section section;
            if (car.getSuspendedSince() != null) {
                section = addDataSection(String.format("%s [%s]", car.getName(),
                        mContext.getString(R.string.suspended)), car.getColor(), 1);
            } else {
                section = addDataSection(car.getName(), car.getColor());
            }

            section.addItem(new Item(mContext.getString(R.string.report_total_mileage),
                    String.format(Locale.getDefault(), "%d %s", latestMileage, mUnit)));
            section.addItem(new Item(mContext.getString(R.string.report_recorded_mileage),
                    String.format(Locale.getDefault(), "%d %s", recordedMileage, mUnit)));
        }
    }
}
