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

import java.util.ArrayList;
import java.util.Date;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.db.Refueling;

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
	private XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
	private XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
	private double minX = Double.MAX_VALUE;
	private double maxX = Double.MIN_VALUE;
	private double minY = Double.MAX_VALUE;
	private double maxY = Double.MIN_VALUE;

	private String unit;
	private boolean showLegend;

	public FuelConsumptionReport(Context context) {
		super(context);

		Preferences prefs = new Preferences(context);
		unit = String.format("%s/100%s", prefs.getUnitVolume(),
				prefs.getUnitDistance());
		showLegend = prefs.isShowLegend();

		ArrayList<Double> consumptions = new ArrayList<Double>();

		Car[] cars = Car.getAll();
		for (Car car : cars) {
			TimeSeries series = new TimeSeries(car.getName());
			ArrayList<Double> consumptionsCar = new ArrayList<Double>();

			Refueling[] refuelings = Refueling.getAllForCar(car, true);
			int lastTacho = -1;
			float volume = 0;
			for (Refueling refueling : refuelings) {
				volume += refueling.getVolume();
				if (!refueling.isPartial()) {
					if (lastTacho > -1) {
						double consumption = volume
								/ (refueling.getMileage() - lastTacho) * 100;
						series.add(refueling.getDate(), consumption);
						consumptionsCar.add(consumption);
					}
					lastTacho = refueling.getMileage();
					volume = 0;
				}
			}

			if (series.getItemCount() == 0) {
				addData(context.getString(R.string.report_not_enough_data), "",
						car);
			} else {
				addSeries(series, car.getColor());

				consumptions.addAll(consumptionsCar);
				addConsumptionData(car, consumptionsCar);
			}
		}

		if (cars.length > 1) {
			if (consumptions.size() == 0) {
				addData(context.getString(R.string.report_not_enough_data), "");
			} else {
				addConsumptionData(null, consumptions);
			}
		}
	}

	@Override
	public GraphicalView getGraphView() {
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

		final GraphicalView graphView = ChartFactory.getTimeChartView(context,
				dataset, renderer, getDateFormatPattern());

		graphView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				SeriesSelection seriesSelection = graphView
						.getCurrentSeriesAndPoint();
				if (seriesSelection != null) {
					String car = dataset.getSeriesAt(
							seriesSelection.getSeriesIndex()).getTitle();
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

	private void addConsumptionData(Car car, ArrayList<Double> numbers) {
		double max = 0;
		double min = Double.MAX_VALUE;
		double sum = 0;
		for (double num : numbers) {
			max = Math.max(max, num);
			min = Math.min(min, num);
			sum += num;
		}

		addData(context.getString(R.string.report_highest),
				String.format("%.2f %s", max, unit), car);
		addData(context.getString(R.string.report_lowest),
				String.format("%.2f %s", min, unit), car);
		addData(context.getString(R.string.report_average),
				String.format("%.2f %s", sum / numbers.size(), unit), car);
	}

	private void addSeries(TimeSeries series, int color) {
		dataset.addSeries(series);

		minX = Math.min(minX, series.getMinX());
		maxX = Math.max(maxX, series.getMaxX());
		minY = Math.min(minY, series.getMinY());
		maxY = Math.max(maxY, series.getMaxY());

		XYSeriesRenderer r = new XYSeriesRenderer();
		applyDefaultStyle(r, color, false);
		renderer.addSeriesRenderer(r);
	}
}
