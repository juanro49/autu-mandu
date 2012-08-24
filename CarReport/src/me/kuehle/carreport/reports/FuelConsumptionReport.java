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
	private Vector<AbstractReportData> reportData = new Vector<AbstractReportData>();

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
			ReportData carData = new ReportData(context, car);

			if (carData.size() == 0) {
				addData(context.getString(R.string.report_not_enough_data), "",
						car);
			} else {
				reportData.add(carData);
				reportData.add(carData.createRegressionData());

				consumptions.addAll(carData.yValues);
				addConsumptionData(car, carData.yValues);
			}
		}

		// Only display overall section, when at least report data for 2
		// cars is present (2 graphs per car).
		if (reportData.size() >= 4) {
			addConsumptionData(null, consumptions);
		}
	}

	@Override
	public GraphicalView getGraphView() {
		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
		double[] axesMinMax = { Double.MAX_VALUE, Double.MIN_VALUE,
				Double.MAX_VALUE, Double.MIN_VALUE };

		// Add series
		for (AbstractReportData data : reportData) {
			TimeSeries series = data.getSeries();
			dataset.addSeries(series);
			renderer.addSeriesRenderer(data.getRenderer());

			axesMinMax[0] = Math.min(axesMinMax[0], series.getMinX());
			axesMinMax[1] = Math.max(axesMinMax[1], series.getMaxX());
			axesMinMax[2] = Math.min(axesMinMax[2], series.getMinY());
			axesMinMax[3] = Math.max(axesMinMax[3], series.getMaxY());
		}

		// Style report
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
				if (seriesSelection != null) {
					String car = reportData.get(seriesSelection
							.getSeriesIndex()).name;
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

	private class ReportData extends AbstractReportData {
		public ReportData(Context context, Car car) {
			super(context, car.getName(), car.getColor());

			Refueling[] refuelings = Refueling.getAllForCar(car, true);
			int lastTacho = -1;
			float volume = 0;
			for (Refueling refueling : refuelings) {
				volume += refueling.getVolume();
				if (!refueling.isPartial()) {
					if (lastTacho > -1) {
						double consumption = volume
								/ (refueling.getMileage() - lastTacho) * 100;
						xValues.add(refueling.getDate().getTime());
						yValues.add(consumption);
					}
					lastTacho = refueling.getMileage();
					volume = 0;
				}
			}
		}

		@Override
		public XYSeriesRenderer getRenderer() {
			XYSeriesRenderer renderer = new XYSeriesRenderer();
			applyDefaultStyle(renderer, color, false);
			return renderer;
		}
	}
}
