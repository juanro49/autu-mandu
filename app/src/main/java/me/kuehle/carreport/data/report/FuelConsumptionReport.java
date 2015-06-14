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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Vector;

import me.kuehle.carreport.FuelConsumption;
import me.kuehle.carreport.R;
import me.kuehle.carreport.data.balancing.BalancedRefuelingCursor;
import me.kuehle.carreport.data.balancing.RefuelingBalancer;
import me.kuehle.carreport.data.query.CarQueries;
import me.kuehle.carreport.provider.car.CarCursor;
import me.kuehle.carreport.provider.car.CarSelection;
import me.kuehle.carreport.util.Calculator;
import me.kuehle.chartlib.axis.DecimalAxisLabelFormatter;
import me.kuehle.chartlib.chart.Chart;
import me.kuehle.chartlib.data.Dataset;
import me.kuehle.chartlib.data.Series;
import me.kuehle.chartlib.renderer.LineRenderer;
import me.kuehle.chartlib.renderer.OnClickListener;
import me.kuehle.chartlib.renderer.RendererList;

public class FuelConsumptionReport extends AbstractReport {
    private class ReportGraphData extends AbstractReportGraphData {
        private Cursor mCursor;
        private double avgConsumption;

        public ReportGraphData(Context context, CarCursor car, String category) {
            super(context, String.format("%s (%s)", car.getName(), category), car.getColor());

            FuelConsumption fuelConsumption = new FuelConsumption(context);
            RefuelingBalancer balancer = new RefuelingBalancer(context);
            BalancedRefuelingCursor refueling = balancer.getBalancedRefuelings(car.getId(), category);
            mCursor = refueling;

            int lastMileage = 0;
            int totalDistance = 0, partialDistance = 0;
            double totalVolume = 0, partialVolume = 0;
            boolean wasLastFullRefuelingGuessed = false;
            boolean foundFullRefueling = false;

            while (refueling.moveToNext()) {
                if (!foundFullRefueling) {
                    if (!refueling.getPartial()) {
                        foundFullRefueling = true;
                        wasLastFullRefuelingGuessed = refueling.getGuessed();
                    }
                } else {
                    partialDistance += refueling.getMileage() - lastMileage;
                    partialVolume += refueling.getVolume();

                    if (!refueling.getPartial() && partialDistance > 0) {
                        totalDistance += partialDistance;
                        totalVolume += partialVolume;

                        double consumption = fuelConsumption.computeFuelConsumption(partialVolume,
                                partialDistance);
                        xValues.add(refueling.getDate().getTime());
                        yValues.add(consumption);

                        if (refueling.getGuessed()) {
                            markLastPoint();
                            markLastLine();
                        } else if (wasLastFullRefuelingGuessed) {
                            markLastLine();
                        }

                        partialDistance = 0;
                        partialVolume = 0;

                        wasLastFullRefuelingGuessed = refueling.getGuessed();
                    }
                }

                lastMileage = refueling.getMileage();
            }

            avgConsumption = fuelConsumption.computeFuelConsumption(totalVolume, totalDistance);
        }

        public double getAverageConsumption() {
            return avgConsumption;
        }

        public Cursor[] getUsedCursors() {
            return new Cursor[] { mCursor };
        }
    }

    private Vector<AbstractReportGraphData> reportData = new Vector<>();
    private double minXValue = Long.MAX_VALUE;

    private String unit;

    public FuelConsumptionReport(Context context) {
        super(context);
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
    protected Chart onGetChart(boolean zoomable, boolean moveable) {
        final Dataset dataset = new Dataset();
        RendererList renderers = new RendererList();
        LineRenderer renderer = new LineRenderer(mContext);
        renderers.addRenderer(renderer);

        Vector<AbstractReportGraphData> reportData = new Vector<>();

        if (isShowTrend()) {
            for (AbstractReportGraphData data : this.reportData) {
                reportData.add(data.createTrendData());
            }
        }

        if (isShowOverallTrend()) {
            for (AbstractReportGraphData data : this.reportData) {
                reportData.add(data.createOverallTrendData());
            }
        }

        reportData.addAll(this.reportData);

        for (int i = 0; i < reportData.size(); i++) {
            dataset.add(reportData.get(i).getSeries());
            reportData.get(i).applySeriesStyle(i, renderer);
        }

        renderer.setOnClickListener(new OnClickListener() {
            @Override
            public void onSeriesClick(int series, int point, boolean marked) {
                Series s = dataset.get(series);
                String car = s.getTitle().substring(0,
                        s.getTitle().lastIndexOf('(') - 1);
                String fuelType = s.getTitle().substring(
                        s.getTitle().lastIndexOf('(') + 1,
                        s.getTitle().length() - 1);
                String date = DateFormat.getDateFormat(mContext).format(
                        new Date((long) s.get(point).x));

                Toast.makeText(
                        mContext,
                        String.format(
                                "%s: %s\n%s: %s\n%s: %.2f %s\n%s: %s",
                                mContext.getString(R.string.report_toast_car),
                                car,
                                mContext.getString(R.string.report_toast_fuel_type),
                                fuelType,
                                mContext.getString(R.string.report_toast_consumption),
                                s.get(point).y, unit, mContext
                                        .getString(R.string.report_toast_date),
                                date), Toast.LENGTH_LONG).show();
            }
        });

        final Chart chart = new Chart(mContext, dataset, renderers);
        applyDefaultChartStyles(chart);
        chart.setShowLegend(false);
        if (isShowTrend()) {
            for (int i = 0; i < reportData.size() / 2; i++) {
                chart.getLegend().setSeriesVisible(i, false);
            }
        }
        chart.getDomainAxis().setLabelFormatter(mDateLabelFormatter);
        chart.getDomainAxis().setDefaultBottomBound(minXValue);
        chart.getRangeAxis().setLabelFormatter(new DecimalAxisLabelFormatter(2));
        chart.getDomainAxis().setZoomable(zoomable);
        chart.getDomainAxis().setMovable(moveable);
        chart.getRangeAxis().setZoomable(zoomable);
        chart.getRangeAxis().setMovable(moveable);

        return chart;
    }

    @Override
    protected Cursor[] onUpdate() {
        // Preferences
        FuelConsumption fuelConsumption = new FuelConsumption(mContext);
        unit = fuelConsumption.getUnitLabel();

        ArrayList<Cursor> cursors = new ArrayList<>();

        // Collect report data and add info data which will be displayed
        // next to the graph.
        CarCursor car = new CarSelection().query(mContext.getContentResolver());
        cursors.add(car);
        while (car.moveToNext()) {
            boolean sectionAdded = false;

            String[] categories = CarQueries.getUsedFuelTypeCategories(mContext, car.getId());
            for (String category : categories) {
                ReportGraphData carData = new ReportGraphData(mContext, car, category);
                if (carData.isEmpty()) {
                    continue;
                }

                reportData.add(carData);
                cursors.addAll(Arrays.asList(carData.getUsedCursors()));

                Section section = addDataSection(car, category);
                Double[] yValues = carData.yValues.toArray(new Double[carData.yValues.size()]);
                section.addItem(new Item(mContext.getString(R.string.report_highest),
                        String.format("%.2f %s", Calculator.max(yValues), unit)));
                section.addItem(new Item(mContext.getString(R.string.report_lowest),
                        String.format("%.2f %s", Calculator.min(yValues), unit)));
                section.addItem(new Item(mContext.getString(R.string.report_average),
                        String.format("%.2f %s", carData.getAverageConsumption(), unit)));

                sectionAdded = true;

                if (car.getSuspendedSince() == null) {
                    minXValue = Math.min(minXValue, carData.getSeries().minX());
                }
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
