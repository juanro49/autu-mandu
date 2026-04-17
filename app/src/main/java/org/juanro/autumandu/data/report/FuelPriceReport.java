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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.juanro.autumandu.gui.chart.kubit.KubitChartBridge;
import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;
import org.juanro.autumandu.model.AutuManduDatabase;
import org.juanro.autumandu.model.entity.FuelType;
import org.juanro.autumandu.model.entity.Refueling;

public class FuelPriceReport extends AbstractReport {
    private class ReportChartData extends AbstractReportChartLineData {
        private double mMax, mMin, mAverage;

        public ReportChartData(Context context, FuelType fuelType, int color, List<Refueling> refuelings) {
            super(context, fuelType.getName(), color);

            mMax = Double.MIN_VALUE;
            mMin = Double.MAX_VALUE;
            mAverage = 0;
            int count = 0;

            for (Refueling refueling : refuelings) {
                if (refueling.getPrice() == 0.0f || refueling.getVolume() == 0.0f) {
                    continue;
                }

                float fuelPrice = refueling.getPrice() / refueling.getVolume();
                mAverage += fuelPrice;
                mMax = Math.max(mMax, fuelPrice);
                mMin = Math.min(mMin, fuelPrice);
                count++;

                add(ReportDateHelper.toFloat(refueling.getDate()),
                        fuelPrice,
                        mContext.getString(R.string.report_toast_fuel_price,
                                fuelPrice,
                                mUnit,
                                fuelType.getName(),
                                mDateFormat.format(refueling.getDate())),
                        false);
            }

            if (count > 0) {
                mAverage /= count;
            }
        }

        public double getAverage() {
            return mAverage;
        }

        public double getMax() {
            return mMax;
        }

        public double getMin() {
            return mMin;
        }
    }

    private final List<AbstractReportChartData> mReportChartData = new ArrayList<>();
    private String mUnit;
    private DateFormat mDateFormat;

    public FuelPriceReport(Context context) {
        super(context);
    }

    @Override
    public String formatXValue(float value, int chartOption) {
        return mDateFormat.format(ReportDateHelper.toDate(value));
    }

    @Override
    public String formatYValue(float value, int chartOption) {
        return String.format(Locale.getDefault(), "%.3f", value);
    }

    @Override
    public int[] getAvailableChartOptions() {
        return new int[1];
    }

    @Override
    public String getTitle() {
        return mContext.getString(R.string.report_title_fuel_price);
    }

    @Override
    public List<AbstractReportChartData> getRawChartData(int chartOption) {
        synchronized (mCachedChartData) {
            if (mCachedChartData.containsKey(chartOption)) {
                return mCachedChartData.get(chartOption);
            }
            List<AbstractReportChartData> data = new ArrayList<>(mReportChartData);
            mCachedChartData.put(chartOption, data);
            return data;
        }
    }

    @Override
    protected void onUpdate() {
        mReportChartData.clear();
        Preferences prefs = new Preferences(mContext);
        mUnit = String.format("%s/%s", prefs.getUnitCurrency(), prefs.getUnitVolume());
        mDateFormat = android.text.format.DateFormat.getDateFormat(mContext);

        AutuManduDatabase db = AutuManduDatabase.getInstance(mContext);
        List<FuelType> fuelTypes = db.getFuelTypeDao().getAll();

        // Bulk load all refuelings and group by fuelTypeId (N+1 avoidance)
        Map<Long, List<Refueling>> refuelingsByFuelType = db.getRefuelingDao().getAll()
                .stream().collect(Collectors.groupingBy(Refueling::getFuelTypeId));

        int[] colors = KubitChartBridge.getColors(mContext);
        int currentColor = 0;

        for (FuelType fuelType : fuelTypes) {
            Long fuelTypeIdObj = fuelType.getId();
            if (fuelTypeIdObj == null) continue;
            long fuelTypeId = fuelTypeIdObj;

            List<Refueling> fuelTypeRefuelings = refuelingsByFuelType.get(fuelTypeId);
            if (fuelTypeRefuelings == null) fuelTypeRefuelings = Collections.emptyList();

            int color = colors[currentColor % colors.length];
            ReportChartData data = new ReportChartData(mContext, fuelType, color, fuelTypeRefuelings);

            if (!data.isEmpty()) {
                mReportChartData.add(data);

                Section section = addDataSection(fuelType.getName(), color);
                section.addItem(new Item(mContext.getString(R.string.report_highest),
                        String.format(Locale.getDefault(), "%.3f %s", data.getMax(), mUnit)));
                section.addItem(new Item(mContext.getString(R.string.report_lowest),
                        String.format(Locale.getDefault(), "%.3f %s", data.getMin(), mUnit)));
                section.addItem(new Item(mContext.getString(R.string.report_average),
                        String.format(Locale.getDefault(), "%.3f %s", data.getAverage(), mUnit)));

                currentColor++;
            }
        }
    }
}
