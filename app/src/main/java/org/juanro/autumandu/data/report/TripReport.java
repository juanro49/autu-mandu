/*
 * Copyright 2026 Juanro49
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

import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;
import org.juanro.autumandu.model.AutuManduDatabase;
import org.juanro.autumandu.model.dto.TripMonthSummary;
import org.juanro.autumandu.model.dto.TripPurposeSummary;
import org.juanro.autumandu.model.dto.TripStatistics;
import org.juanro.autumandu.model.entity.Car;
import org.juanro.autumandu.model.entity.Trip;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class TripReport extends AbstractReport {

    private String mUnitDistance;
    private String mUnitCurrency;
    private final List<AbstractReportChartData> mChartData = new ArrayList<>();

    public TripReport(Context context) {
        super(context);
    }

    @Override
    public int[] getAvailableChartOptions() {
        return new int[]{
                R.string.report_chart_option_pie,
                R.string.report_chart_option_donut
        };
    }

    @Override
    public String getTitle() {
        return mContext.getString(R.string.report_title_trips);
    }

    @Override
    public String formatXValue(float value, int chartOption) {
        return "";
    }

    @Override
    public String formatYValue(float value, int chartOption) {
        return String.format(Locale.getDefault(), "%.1f%%", value);
    }

    @Override
    public List<AbstractReportChartData> getRawChartData(int chartOption) {
        synchronized (mCachedChartData) {
            final Integer optionKey = chartOption;
            if (mCachedChartData.containsKey(optionKey)) {
                return mCachedChartData.get(optionKey);
            }
            List<AbstractReportChartData> data = new ArrayList<>(mChartData);
            mCachedChartData.put(optionKey, data);
            return data;
        }
    }

    @Override
    protected void onUpdate() {
        Preferences prefs = new Preferences(mContext);
        mUnitDistance = prefs.getUnitDistance();
        mUnitCurrency = prefs.getUnitCurrency();
        mChartData.clear();

        AutuManduDatabase db = AutuManduDatabase.getInstance(mContext);
        List<Car> cars = db.getCarDao().getAll();

        Map<String, Double> globalPurposeDistance = new HashMap<>();

        for (Car car : cars) {
            processCar(car, db, globalPurposeDistance);
        }

        // Add chart for purpose breakdown
        if (!globalPurposeDistance.isEmpty()) {
            ReportChartData chartData = new ReportChartData(mContext, mContext.getString(R.string.report_title_trips), -1);
            double totalDistance = globalPurposeDistance.values().stream().mapToDouble(Double::doubleValue).sum();
            if (totalDistance > 0) {
                float j = 0;
                for (Map.Entry<String, Double> entry : globalPurposeDistance.entrySet()) {
                    chartData.add(j++, (float) (entry.getValue() / totalDistance * 100), entry.getKey());
                }
                mChartData.add(chartData);
            }
        }
    }

    private void processCar(Car car, AutuManduDatabase db, Map<String, Double> globalPurposeDistance) {
        List<Trip> trips = db.getTripDao().getTripsForCar(car.getId());
        if (trips.isEmpty()) return;

        Section section = addDataSection(car.getName(), car.getColor());

        TripStatistics stats = TripStatistics.fromTrips(trips);

        section.addItem(new Item(mContext.getString(R.string.report_total_mileage),
                String.format(Locale.getDefault(), "%d %s", stats.totalDistance(), mUnitDistance)));
        section.addItem(new Item(mContext.getString(R.string.hint_trip_km_business),
                String.format(Locale.getDefault(), "%d %s", stats.businessKm(), mUnitDistance)));
        section.addItem(new Item(mContext.getString(R.string.hint_trip_km_private),
                String.format(Locale.getDefault(), "%d %s", stats.privateKm(), mUnitDistance)));
        section.addItem(new Item(mContext.getString(R.string.hint_trip_km_home_work),
                String.format(Locale.getDefault(), "%d %s", stats.homeWorkKm(), mUnitDistance)));
        section.addItem(new Item(mContext.getString(R.string.report_title_costs),
                String.format(Locale.getDefault(), "%.2f %s", stats.totalCosts(), mUnitCurrency)));

        // Add purpose breakdown
        Map<String, TripPurposeSummary> purposeSummary = getSummaryByPurpose(car);
        for (TripPurposeSummary summary : purposeSummary.values()) {
            section.addItem(new Item("  - " + summary.purpose(),
                    String.format(Locale.getDefault(), "%d %s (x%d)",
                            summary.totalDistance(), mUnitDistance, summary.count())));

            globalPurposeDistance.merge(summary.purpose(), (double) summary.totalDistance(), Double::sum);
        }

        // Add monthly summary
        List<TripMonthSummary> monthlySummary = getSummaryByMonth(car);
        if (!monthlySummary.isEmpty()) {
            section.addItem(new Item(mContext.getString(R.string.report_graph_month_history), ""));
            String monthFormat = "MMM yyyy";
            if (mContext.getResources().getConfiguration().smallestScreenWidthDp > 480) {
                monthFormat = "MMMM yyyy";
            }
            DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern(monthFormat);
            for (TripMonthSummary summary : monthlySummary) {
                section.addItem(new Item("  - " + summary.month().format(monthFormatter),
                        String.format(Locale.getDefault(), "%d %s (x%d)",
                                summary.totalDistance(), mUnitDistance, summary.totalTrips())));
            }
        }
    }

    public List<TripMonthSummary> getSummaryByMonth(Car car) {
        AutuManduDatabase db = AutuManduDatabase.getInstance(mContext);
        List<Trip> trips = db.getTripDao().getTripsForCar(car.getId());

        Map<YearMonth, List<Trip>> grouped = trips.stream()
                .collect(Collectors.groupingBy(trip -> YearMonth.from(trip.getDate())));

        return grouped.entrySet().stream()
                .map(entry -> {
                    YearMonth month = entry.getKey();
                    List<Trip> monthTrips = entry.getValue();
                    int totalDistance = monthTrips.stream().mapToInt(Trip::getTotalDistance).sum();
                    int businessKm = monthTrips.stream().mapToInt(Trip::getKmBusiness).sum();
                    int privateKm = monthTrips.stream().mapToInt(Trip::getKmPrivate).sum();
                    int homeWorkKm = monthTrips.stream().mapToInt(Trip::getKmHomeWork).sum();
                    double totalCosts = monthTrips.stream().mapToDouble(Trip::getTotalCost).sum();
                    return new TripMonthSummary(month, monthTrips.size(), totalDistance,
                            businessKm, privateKm, homeWorkKm, totalCosts);
                })
                .sorted(Comparator.comparing(TripMonthSummary::month).reversed())
                .collect(Collectors.toList());
    }

    public Map<String, TripPurposeSummary> getSummaryByPurpose(Car car) {
        AutuManduDatabase db = AutuManduDatabase.getInstance(mContext);
        List<Trip> trips = db.getTripDao().getTripsForCar(car.getId());

        Map<String, List<Trip>> grouped = trips.stream()
                .collect(Collectors.groupingBy(Trip::getPurpose));

        Map<String, TripPurposeSummary> result = new HashMap<>();
        grouped.forEach((purpose, purposeTrips) -> {
            int totalDistance = purposeTrips.stream().mapToInt(Trip::getTotalDistance).sum();
            double totalCosts = purposeTrips.stream().mapToDouble(Trip::getTotalCost).sum();
            result.put(purpose, new TripPurposeSummary(purpose, purposeTrips.size(), totalDistance, totalCosts));
        });
        return result;
    }

    private static class ReportChartData extends AbstractReportChartColumnData {
        protected ReportChartData(Context context, String name, int color) {
            super(context, name, color);
        }
    }
}
