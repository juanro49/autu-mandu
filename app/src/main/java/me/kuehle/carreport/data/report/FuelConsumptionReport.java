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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import me.kuehle.carreport.FuelConsumption;
import me.kuehle.carreport.R;
import me.kuehle.carreport.data.balancing.BalancedRefuelingCursor;
import me.kuehle.carreport.data.balancing.RefuelingBalancer;
import me.kuehle.carreport.data.query.CarQueries;
import me.kuehle.carreport.provider.car.CarColumns;
import me.kuehle.carreport.provider.car.CarCursor;
import me.kuehle.carreport.provider.car.CarSelection;
import me.kuehle.carreport.util.Calculator;

public class FuelConsumptionReport extends AbstractReport {
    private class ReportChartData extends AbstractReportChartLineData {
        private Cursor mCursor;
        private double mAvgConsumption;

        public ReportChartData(Context context, CarCursor car, String category) {
            super(context, String.format("%s (%s)", car.getName(), category), car.getColor());

            FuelConsumption fuelConsumption = new FuelConsumption(context);
            RefuelingBalancer balancer = new RefuelingBalancer(context);
            BalancedRefuelingCursor refueling = balancer.getBalancedRefuelings(car.getId(), category);
            mCursor = refueling;

            int lastMileage = 0;
            int totalDistance = 0, partialDistance = 0;
            float totalVolume = 0, partialVolume = 0;
            boolean foundFullRefueling = false;

            while (refueling.moveToNext()) {
                if (!foundFullRefueling) {
                    if (!refueling.getPartial()) {
                        foundFullRefueling = true;
                    }
                } else {
                    partialDistance += refueling.getMileage() - lastMileage;
                    partialVolume += refueling.getVolume();

                    if (!refueling.getPartial() && partialDistance > 0) {
                        totalDistance += partialDistance;
                        totalVolume += partialVolume;

                        float consumption = fuelConsumption.computeFuelConsumption(partialVolume,
                                partialDistance);
                        String tooltip = mContext.getString(R.string.report_toast_fuel_consumption,
                                car.getName(),
                                consumption,
                                mUnit,
                                refueling.getFuelTypeName(),
                                mDateFormat.format(refueling.getDate()));
                        if (refueling.getGuessed()) {
                            tooltip += "\n" + mContext.getString(R.string.report_toast_guessed);
                        }

                        add((float) refueling.getDate().getTime(),
                                consumption,
                                tooltip,
                                refueling.getGuessed());

                        partialDistance = 0;
                        partialVolume = 0;
                    }
                }

                lastMileage = refueling.getMileage();
            }

            mAvgConsumption = fuelConsumption.computeFuelConsumption(totalVolume, totalDistance);
        }

        public double getAverageConsumption() {
            return mAvgConsumption;
        }

        public Cursor[] getUsedCursors() {
            return new Cursor[]{mCursor};
        }
    }

    private List<AbstractReportChartData> reportData = new ArrayList<>();
    private String mUnit;
    private DateFormat mDateFormat;

    public FuelConsumptionReport(Context context) {
        super(context);
    }

    @Override
    protected String formatXValue(float value, int chartOption) {
        return mDateFormat.format(new Date((long) value));
    }

    @Override
    protected String formatYValue(float value, int chartOption) {
        return String.format("%.2f", value);
    }

    @Override
    public int[] getAvailableChartOptions() {
        return new int[1];
    }

    @Override
    public String getTitle() {
        return mContext.getString(R.string.report_title_fuel_consumption);
    }

    @Override
    protected List<AbstractReportChartData> getRawChartData(int chartOption) {
        return reportData;
    }

    @Override
    protected Cursor[] onUpdate() {
        // Preferences
        FuelConsumption fuelConsumption = new FuelConsumption(mContext);
        mUnit = fuelConsumption.getUnitLabel();
        mDateFormat = android.text.format.DateFormat.getDateFormat(mContext);

        ArrayList<Cursor> cursors = new ArrayList<>();

        // Collect report data and add info data which will be displayed
        // next to the graph.
        CarCursor car = new CarSelection().query(mContext.getContentResolver(), null, CarColumns.NAME + " COLLATE UNICODE");
        cursors.add(car);
        while (car.moveToNext()) {
            boolean sectionAdded = false;

            String[] categories = CarQueries.getUsedFuelTypeCategories(mContext, car.getId());
            for (String category : categories) {
                ReportChartData carData = new ReportChartData(mContext, car, category);
                if (carData.isEmpty()) {
                    continue;
                }

                reportData.add(carData);
                cursors.addAll(Arrays.asList(carData.getUsedCursors()));

                Section section = addDataSection(car, category);
                Float[] yValues = carData.getYValues().toArray(new Float[carData.size()]);
                section.addItem(new Item(mContext.getString(R.string.report_highest),
                        String.format("%.2f %s", Calculator.max(yValues), mUnit)));
                section.addItem(new Item(mContext.getString(R.string.report_lowest),
                        String.format("%.2f %s", Calculator.min(yValues), mUnit)));
                section.addItem(new Item(mContext.getString(R.string.report_average),
                        String.format("%.2f %s", carData.getAverageConsumption(), mUnit)));

                sectionAdded = true;
            }

            if (!sectionAdded) {
                Section section = addDataSection(car);
                section.addItem(new Item(mContext
                        .getString(R.string.report_not_enough_data), ""));
            }
        }

        return cursors.toArray(new Cursor[cursors.size()]);
    }

    private Section addDataSection(CarCursor car) {
        String name = car.getName();

        if (car.getSuspendedSince() != null) {
            return addDataSection(String.format("%s [%s]", name,
                    mContext.getString(R.string.suspended)), car.getColor(), 1);
        } else {
            return addDataSection(name, car.getColor());
        }
    }

    private Section addDataSection(CarCursor car, String category) {
        String name = String.format("%s (%s)", car.getName(), category);

        if (car.getSuspendedSince() != null) {
            return addDataSection(String.format("%s [%s]", name,
                    mContext.getString(R.string.suspended)), car.getColor(), 1);
        } else {
            return addDataSection(name, car.getColor());
        }
    }
}
