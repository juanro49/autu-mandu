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

package me.kuehle.carreport.reports;

import java.util.Date;
import java.util.Vector;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.db.Refueling;
import me.kuehle.carreport.util.Calculator;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.SeriesSelection;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Toast;

public class FuelConsumptionReport extends AbstractReport {
	private Vector<CarData> reportData = new Vector<CarData>();

	private String unit;
	private boolean showLegend;

	public FuelConsumptionReport(Context context) {
		super(context);

		// Preferences
		Preferences prefs = new Preferences(context);
		unit = String.format("%s/100%s", prefs.getUnitVolume(),
				prefs.getUnitDistance());
		showLegend = prefs.isShowLegend();

		// Collect report data and add info data which will be displayed
		// next to the graph.
		Vector<Double> consumptions = new Vector<Double>();
		Car[] cars = Car.getAll();
		for (Car car : cars) {
			CarData carData = new CarData(car);

			if (carData.size() == 0) {
				addData(context.getString(R.string.report_not_enough_data), "",
						car);
			} else {
				reportData.add(carData);

				consumptions.addAll(carData.consumptions);
				addConsumptionData(car, carData.consumptions);
			}
		}

		// Only display overall section, when at least report data for 2
		// cars is present.
		if (reportData.size() >= 2) {
			addConsumptionData(null, consumptions);
		}
	}

	@Override
	public GraphicalView getGraphView() {
		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
		double minX = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double minY = Double.MAX_VALUE;
		double maxY = Double.MIN_VALUE;

		// Add series
		for (CarData carData : reportData) {
			// Trend
			TimeSeries trendSeries = new TimeSeries(context.getString(
					R.string.report_trend_label, carData.car.getName()));
			for (int i = 0; i < carData.regressionSize(); i++) {
				trendSeries.add(new Date(carData.regressionDates.get(i)),
						carData.regressionValues.get(i));
			}
			dataset.addSeries(trendSeries);

			XYSeriesRenderer tr = new XYSeriesRenderer();
			applyTrendStyle(tr, carData.car.getColor());
			renderer.addSeriesRenderer(tr);

			// Original data
			TimeSeries series = new TimeSeries(carData.car.getName());
			for (int i = 0; i < carData.size(); i++) {
				series.add(new Date(carData.dates.get(i)),
						carData.consumptions.get(i));
			}
			dataset.addSeries(series);

			minX = Math.min(minX, series.getMinX());
			maxX = Math.max(maxX, series.getMaxX());
			minY = Math.min(minY, series.getMinY());
			maxY = Math.max(maxY, series.getMaxY());

			XYSeriesRenderer r = new XYSeriesRenderer();
			applyDefaultStyle(r, carData.car.getColor(), false);
			renderer.addSeriesRenderer(r);
		}

		// Style report
		double[] axesMinMax = { minX, maxX, minY, maxY };
		applyDefaultStyle(renderer, axesMinMax, true, null, "%.2f");
		renderer.setShowLegend(showLegend);
		// Legend height is not proportional to its text size. So this is a
		// quick fix.
		if (showLegend) {
			int[] margins = renderer.getMargins();
			margins[2] = (int) renderer.getLegendTextSize();
			renderer.setMargins(margins);
		}

		// Draw report
		final GraphicalView graphView = ChartFactory.getTimeChartView(context,
				dataset, renderer, getDateFormatPattern());

		// Add click listener
		graphView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				SeriesSelection seriesSelection = graphView
						.getCurrentSeriesAndPoint();
				if (seriesSelection != null
						&& seriesSelection.getSeriesIndex() % 2 != 0) {
					String car = reportData.get(seriesSelection
							.getSeriesIndex() / 2).car.getName();
					String date = DateFormat.getDateFormat(context).format(
							new Date((long) seriesSelection.getXValue()));
					Toast.makeText(
							context,
							String.format(
									"%s: %s\n%s: %.2f %s\n%s: %s",
									context.getString(R.string.report_toast_car),
									car,
									context.getString(R.string.report_toast_consumption),
									seriesSelection.getValue(),
									unit,
									context.getString(R.string.report_toast_date),
									date), Toast.LENGTH_LONG).show();
				}
			}
		});

		return graphView;
	}

	private void addConsumptionData(Car car, Vector<Double> numbers) {
		addData(context.getString(R.string.report_highest),
				String.format("%.2f %s", Calculator.max(numbers), unit), car);
		addData(context.getString(R.string.report_lowest),
				String.format("%.2f %s", Calculator.min(numbers), unit), car);
		addData(context.getString(R.string.report_average),
				String.format("%.2f %s", Calculator.avg(numbers), unit), car);
	}

	private class CarData {
		private Car car;

		private Vector<Long> dates = new Vector<Long>();
		private Vector<Double> consumptions = new Vector<Double>();

		private Vector<Long> regressionDates = new Vector<Long>();
		private Vector<Double> regressionValues = new Vector<Double>();

		public CarData(Car car) {
			this.car = car;

			Refueling[] refuelings = Refueling.getAllForCar(car, true);
			int lastTacho = -1;
			float volume = 0;
			for (Refueling refueling : refuelings) {
				volume += refueling.getVolume();
				if (!refueling.isPartial()) {
					if (lastTacho > -1) {
						double consumption = volume
								/ (refueling.getMileage() - lastTacho) * 100;
						dates.add(refueling.getDate().getTime());
						consumptions.add(consumption);
					}
					lastTacho = refueling.getMileage();
					volume = 0;
				}
			}

			calcRegressionValues();
		}

		public int size() {
			return dates.size();
		}

		public int regressionSize() {
			return regressionDates.size();
		}

		private void calcRegressionValues() {
			long avgX = Calculator.avg(dates);
			double avgY = Calculator.avg(consumptions);

			long sum1 = 0; // (x_i - avg(X)) ^ 2
			double sum2 = 0; // (x_i - avg(X)) * (y_i - avg(Y))
			for (int i = 0; i < size(); i++) {
				long xMinusAvgX = dates.get(i) - avgX;
				double yMinusAvgY = consumptions.get(i) - avgY;
				sum1 += xMinusAvgX * xMinusAvgX;
				sum2 += xMinusAvgX * yMinusAvgY;
			}

			double beta1 = sum2 / sum1;
			double beta0 = avgY - (beta1 * avgX);

			regressionValues.clear();
			regressionDates.clear();
			regressionDates.add(dates.firstElement());
			regressionValues.add(beta0 + (beta1 * dates.firstElement()));
			regressionDates.add(dates.lastElement());
			regressionValues.add(beta0 + (beta1 * dates.lastElement()));
		}
	}

}
