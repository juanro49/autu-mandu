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
import android.database.Cursor;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import lecho.lib.hellocharts.util.ChartUtils;
import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;
import org.juanro.autumandu.provider.fueltype.FuelTypeColumns;
import org.juanro.autumandu.provider.fueltype.FuelTypeCursor;
import org.juanro.autumandu.provider.fueltype.FuelTypeSelection;
import org.juanro.autumandu.provider.refueling.RefuelingColumns;
import org.juanro.autumandu.provider.refueling.RefuelingCursor;
import org.juanro.autumandu.provider.refueling.RefuelingSelection;

public class FuelPriceReport extends AbstractReport {
    private class ReportChartData extends AbstractReportChartLineData {
        private Cursor mCursor;
        private double mMax, mMin, mAverage;

        public ReportChartData(Context context, FuelTypeCursor fuelType, int color) {
            super(context, fuelType.getName(), color);

            RefuelingCursor refueling = new RefuelingSelection()
                    .fuelTypeId(fuelType.getId())
                    .query(mContext.getContentResolver(), RefuelingColumns.ALL_COLUMNS, RefuelingColumns.DATE);
            mCursor = refueling;
            mMax = Double.MIN_VALUE;
            mMin = Double.MAX_VALUE;
            mAverage = 0;
            int count = 0;
            while (refueling.moveToNext()) {
                if (refueling.getPrice() == 0.0f) {
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

            mAverage /= count;
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

        public Cursor[] getUsedCursors() {
            return new Cursor[]{mCursor};
        }
    }

    private ArrayList<AbstractReportChartData> mReportChartData;
    private String mUnit;
    private DateFormat mDateFormat;

    public FuelPriceReport(Context context) {
        super(context);
    }

    @Override
    protected String formatXValue(float value, int chartOption) {
        return mDateFormat.format(ReportDateHelper.toDate(value));
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
        return mContext.getString(R.string.report_title_fuel_price);
    }

    @Override
    protected List<AbstractReportChartData> getRawChartData(int chartOption) {
        return mReportChartData;
    }

    @Override
    protected Cursor[] onUpdate() {
        Preferences prefs = new Preferences(mContext);
        mUnit = String.format("%s/%s", prefs.getUnitCurrency(), prefs.getUnitVolume());
        mDateFormat = android.text.format.DateFormat.getDateFormat(mContext);

        ArrayList<Cursor> cursors = new ArrayList<>();

        FuelTypeCursor fuelType = new FuelTypeSelection().query(mContext.getContentResolver(), null,
                FuelTypeColumns.NAME + " COLLATE UNICODE");
        cursors.add(fuelType);

        int[] colors = ChartUtils.COLORS;
        int currentColor = 0;

        mReportChartData = new ArrayList<>();
        while (fuelType.moveToNext()) {
            int color = colors[currentColor];
            ReportChartData data = new ReportChartData(mContext, fuelType, color);
            cursors.addAll(Arrays.asList(data.getUsedCursors()));
            if (!data.isEmpty()) {
                mReportChartData.add(data);

                Section section = addDataSection(fuelType.getName(), color);
                section.addItem(new Item(mContext
                        .getString(R.string.report_highest), String.format(Locale.getDefault(),
                        "%.3f %s", data.getMax(), mUnit)));
                section.addItem(new Item(mContext
                        .getString(R.string.report_lowest), String.format(Locale.getDefault(),
                        "%.3f %s", data.getMin(), mUnit)));
                section.addItem(new Item(mContext
                        .getString(R.string.report_average), String.format(Locale.getDefault(),
                        "%.3f %s", data.getAverage(), mUnit)));

                currentColor++;
                if (currentColor >= colors.length) {
                    currentColor = 0;
                }
            }
        }

        return cursors.toArray(new Cursor[cursors.size()]);
    }
}
