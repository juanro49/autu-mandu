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
import android.graphics.Color;
import android.text.format.DateFormat;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.provider.fueltype.FuelTypeCursor;
import me.kuehle.carreport.provider.fueltype.FuelTypeSelection;
import me.kuehle.carreport.provider.refueling.RefuelingColumns;
import me.kuehle.carreport.provider.refueling.RefuelingCursor;
import me.kuehle.carreport.provider.refueling.RefuelingSelection;
import me.kuehle.chartlib.axis.DecimalAxisLabelFormatter;
import me.kuehle.chartlib.chart.Chart;
import me.kuehle.chartlib.data.Dataset;
import me.kuehle.chartlib.data.PointD;
import me.kuehle.chartlib.data.Series;
import me.kuehle.chartlib.renderer.LineRenderer;
import me.kuehle.chartlib.renderer.OnClickListener;
import me.kuehle.chartlib.renderer.RendererList;

public class FuelPriceReport extends AbstractReport {
    private class ReportGraphData extends AbstractReportGraphData {
        public ReportGraphData(Context context, FuelTypeCursor fuelType, int color) {
            super(context, fuelType.getName(), color);

            RefuelingCursor refueling = new RefuelingSelection().fuelTypeId(fuelType.getId())
                    .query(mContext.getContentResolver(), RefuelingColumns.ALL_COLUMNS);
            while (refueling.moveToNext()) {
                xValues.add(refueling.getDate().getTime());
                yValues.add((double) (refueling.getPrice() / refueling.getVolume()));
            }
        }
    }

    private ArrayList<ReportGraphData> reportData;
    private String unit;

    public FuelPriceReport(Context context) {
        super(context);
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
    protected Chart onGetChart(boolean zoomable, boolean moveable) {
        final Dataset dataset = new Dataset();
        RendererList renderers = new RendererList();
        LineRenderer renderer = new LineRenderer(mContext);
        renderers.addRenderer(renderer);

        int series = 0;
        for (ReportGraphData data : reportData) {
            dataset.add(data.getSeries());
            data.applySeriesStyle(series++, renderer);
            if (reportData.size() == 1) {
                renderer.setSeriesFillBelowLine(0, true);
            }

            if (isShowTrend()) {
                AbstractReportGraphData trendReportData = data
                        .createTrendData();
                dataset.add(trendReportData.getSeries());
                trendReportData.applySeriesStyle(series++, renderer);
            }

            if (isShowOverallTrend()) {
                AbstractReportGraphData trendReportData = data
                        .createOverallTrendData();
                dataset.add(trendReportData.getSeries());
                trendReportData.applySeriesStyle(series++, renderer);
            }
        }

        renderer.setOnClickListener(new OnClickListener() {
            @Override
            public void onSeriesClick(int series, int point, boolean marked) {
                Series s = dataset.get(series);
                String fuelType = s.getTitle() == null ? mContext
                        .getString(R.string.report_toast_none) : s.getTitle();
                PointD p = s.get(point);
                String date = DateFormat.getDateFormat(mContext).format(
                        new Date((long) p.x));
                Toast.makeText(
                        mContext,
                        String.format("%s: %s\n%s: %.3f %s\n%s: %s", mContext
                                        .getString(R.string.report_toast_fuel_type),
                                fuelType,
                                mContext.getString(R.string.report_toast_price),
                                p.y, unit, mContext
                                        .getString(R.string.report_toast_date),
                                date), Toast.LENGTH_LONG).show();
            }
        });

        final Chart chart = new Chart(mContext, dataset, renderers);
        applyDefaultChartStyles(chart);
        chart.setShowLegend(false);
        chart.getDomainAxis().setLabelFormatter(mDateLabelFormatter);
        chart.getRangeAxis()
                .setLabelFormatter(new DecimalAxisLabelFormatter(3));
        chart.getDomainAxis().setZoomable(zoomable);
        chart.getDomainAxis().setMovable(moveable);
        chart.getRangeAxis().setZoomable(zoomable);
        chart.getRangeAxis().setMovable(moveable);

        return chart;
    }

    @Override
    protected void onUpdate() {
        Preferences prefs = new Preferences(mContext);
        unit = String.format("%s/%s", prefs.getUnitCurrency(),
                prefs.getUnitVolume());

        FuelTypeCursor fuelType = new FuelTypeSelection().query(mContext.getContentResolver());

        float[] hsvColor = new float[3];
        Color.colorToHSV(
                mContext.getResources().getColor(android.R.color.holo_blue_dark),
                hsvColor);
        float hueDiff = fuelType.getCount() == 0 ? 60 : Math.min(60,
                360 / fuelType.getCount());

        reportData = new ArrayList<>();
        while (fuelType.moveToNext()) {
            int color = Color.HSVToColor(hsvColor);
            ReportGraphData data = new ReportGraphData(mContext, fuelType, color);
            if (!data.isEmpty()) {
                reportData.add(data);

                Series series = data.getSeries();
                double avg = 0;
                for (int i = 0; i < series.size(); i++) {
                    avg += series.get(i).y;
                }
                avg /= series.size();

                Section section = addDataSection(fuelType.getName(), color);
                section.addItem(new Item(mContext
                        .getString(R.string.report_highest), String.format(
                        "%.3f %s", series.maxY(), unit)));
                section.addItem(new Item(mContext
                        .getString(R.string.report_lowest), String.format(
                        "%.3f %s", series.minY(), unit)));
                section.addItem(new Item(mContext
                        .getString(R.string.report_average), String.format(
                        "%.3f %s", avg, unit)));

                hsvColor[0] += hueDiff;
                if (hsvColor[0] > 360) {
                    hsvColor[0] -= 360;
                }
            }
        }
    }
}
