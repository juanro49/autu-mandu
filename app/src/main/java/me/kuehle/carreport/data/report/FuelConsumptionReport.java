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
import android.text.format.DateFormat;
import android.widget.Toast;

import java.util.Date;
import java.util.List;
import java.util.Vector;

import me.kuehle.carreport.FuelConsumption;
import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.data.balancing.RefuelingBalancer;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.db.Refueling;
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
        private double avgConsumption;

        public ReportGraphData(Context context, Car car, String category) {
            super(context, String.format("%s (%s)", car.name, category), car.color);

            FuelConsumption fuelConsumption = new FuelConsumption(context);
            RefuelingBalancer balancer = new RefuelingBalancer(context);
            List<Refueling> refuelings = balancer.getBalancedRefuelings(car, category);

            int totalDistance = 0, partialDistance = 0;
            double totalVolume = 0, partialVolume = 0;
            int lastFullRefueling = -1;
            for (int i = 0; i < refuelings.size(); i++) {
                Refueling refueling = refuelings.get(i);
                if (lastFullRefueling < 0) {
                    if (!refueling.partial) {
                        lastFullRefueling = i;
                    }

                    continue;
                }

                partialDistance += refueling.mileage
                        - refuelings.get(i - 1).mileage;
                partialVolume += refueling.volume;

                if (!refueling.partial && partialDistance > 0) {
                    totalDistance += partialDistance;
                    totalVolume += partialVolume;

                    double consumption = fuelConsumption
                            .computeFuelConsumption(partialVolume,
                                    partialDistance);
                    xValues.add(refueling.date.getTime());
                    yValues.add(consumption);

                    if (refuelings.get(i).guessed) {
                        markLastPoint();
                        markLastLine();
                    } else if (refuelings.get(lastFullRefueling).guessed) {
                        markLastLine();
                    }

                    partialDistance = 0;
                    partialVolume = 0;

                    lastFullRefueling = i;
                }
            }

            avgConsumption = fuelConsumption.computeFuelConsumption(
                    totalVolume, totalDistance);
        }

        public double getAverageConsumption() {
            return avgConsumption;
        }
    }

    private Vector<AbstractReportGraphData> reportData = new Vector<>();
    private double minXValue = Long.MAX_VALUE;

    private String unit;

    private boolean showLegend;

    public FuelConsumptionReport(Context context) {
        super(context);
    }

    @Override
    public int[] getAvailableChartOptions() {
        return new int[1];
    }

    @Override
    public String getTitle() {
        return context.getString(R.string.report_title_fuel_consumption);
    }

    private Section addDataSection(Car car) {
        String name = car.name;

        if (car.isSuspended()) {
            return addDataSection(String.format("%s [%s]", name,
                    context.getString(R.string.suspended)), car.color, 1);
        } else {
            return addDataSection(name, car.color);
        }
    }

    private Section addDataSection(Car car, String category) {
        String name = String.format("%s (%s)", car.name, category);

        if (car.isSuspended()) {
            return addDataSection(String.format("%s [%s]", name,
                    context.getString(R.string.suspended)), car.color, 1);
        } else {
            return addDataSection(name, car.color);
        }
    }

    @Override
    protected Chart onGetChart(boolean zoomable, boolean moveable) {
        final Dataset dataset = new Dataset();
        RendererList renderers = new RendererList();
        LineRenderer renderer = new LineRenderer(context);
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
                String date = DateFormat.getDateFormat(context).format(
                        new Date((long) s.get(point).x));

                Toast.makeText(
                        context,
                        String.format(
                                "%s: %s\n%s: %s\n%s: %.2f %s\n%s: %s",
                                context.getString(R.string.report_toast_car),
                                car,
                                context.getString(R.string.report_toast_fueltype),
                                fuelType,
                                context.getString(R.string.report_toast_consumption),
                                s.get(point).y, unit, context
                                        .getString(R.string.report_toast_date),
                                date), Toast.LENGTH_LONG).show();
            }
        });

        final Chart chart = new Chart(context, dataset, renderers);
        applyDefaultChartStyles(chart);
        chart.setShowLegend(showLegend);
        if (isShowTrend()) {
            for (int i = 0; i < reportData.size() / 2; i++) {
                chart.getLegend().setSeriesVisible(i, false);
            }
        }
        chart.getDomainAxis().setLabelFormatter(dateLabelFormatter);
        chart.getDomainAxis().setDefaultBottomBound(minXValue);
        chart.getRangeAxis().setLabelFormatter(new DecimalAxisLabelFormatter(2));
        chart.getDomainAxis().setZoomable(zoomable);
        chart.getDomainAxis().setMovable(moveable);
        chart.getRangeAxis().setZoomable(zoomable);
        chart.getRangeAxis().setMovable(moveable);

        return chart;
    }

    @Override
    protected void onUpdate() {
        // Preferences
        Preferences prefs = new Preferences(context);
        FuelConsumption fuelConsumption = new FuelConsumption(context);
        unit = fuelConsumption.getUnitLabel();
        showLegend = prefs.isShowLegend();

        // Collect report data and add info data which will be displayed
        // next to the graph.
        List<Car> cars = Car.getAll();
        for (Car car : cars) {
            boolean sectionAdded = false;

            List<String> categories = car.getUsedFuelTypeCategories();
            for (String category : categories) {
                ReportGraphData carData = new ReportGraphData(context, car, category);
                if (carData.isEmpty()) {
                    continue;
                }

                reportData.add(carData);

                Section section = addDataSection(car, category);
                Double[] yValues = carData.yValues.toArray(new Double[carData.yValues.size()]);
                section.addItem(new Item(context.getString(R.string.report_highest),
                        String.format("%.2f %s", Calculator.max(yValues), unit)));
                section.addItem(new Item(context.getString(R.string.report_lowest),
                        String.format("%.2f %s", Calculator.min(yValues), unit)));
                section.addItem(new Item(context.getString(R.string.report_average),
                        String.format("%.2f %s", carData.getAverageConsumption(), unit)));

                sectionAdded = true;

                if (!car.isSuspended()) {
                    minXValue = Math.min(minXValue, carData.getSeries().minX());
                }
            }

            if (!sectionAdded) {
                Section section = addDataSection(car);
                section.addItem(new Item(context
                        .getString(R.string.report_not_enough_data), ""));
            }
        }
    }
}
