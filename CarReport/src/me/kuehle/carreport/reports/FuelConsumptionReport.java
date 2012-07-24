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

import java.text.DateFormat;
import java.util.ArrayList;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.db.Refueling;
import android.content.Context;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphView.GraphViewSeries;
import com.jjoe64.graphview.GraphView.GraphViewStyle;
import com.jjoe64.graphview.GraphView.LegendAlign;
import com.jjoe64.graphview.LineGraphView;

public class FuelConsumptionReport extends AbstractReport {
	private ArrayList<GraphViewSeries> graphSerieses = new ArrayList<GraphViewSeries>();
	private String unit;

	public FuelConsumptionReport(Context context) {
		super(context);

		Preferences prefs = new Preferences(context);
		unit = String.format("%s/100%s", prefs.getUnitVolume(),
				prefs.getUnitDistance());

		ArrayList<Double> consumptions = new ArrayList<Double>();

		Car[] cars = Car.getAll();
		for (Car car : cars) {
			ArrayList<GraphViewData> graphData = new ArrayList<GraphView.GraphViewData>();
			ArrayList<Double> consumptionsCar = new ArrayList<Double>();

			Refueling[] refuelings = Refueling.getAllForCar(car, true);
			int lastTacho = -1;
			float volume = 0;
			for (Refueling refueling : refuelings) {
				volume += refueling.getVolume();
				if (!refueling.isPartial()) {
					if (lastTacho > -1) {
						double consumption = volume
								/ (refueling.getTachometer() - lastTacho) * 100;
						graphData.add(new GraphViewData(refueling.getDate()
								.getTime(), consumption));
						consumptionsCar.add(consumption);
					}
					lastTacho = refueling.getTachometer();
					volume = 0;
				}
			}

			if (graphData.size() == 0) {
				addData(context.getString(R.string.report_not_enough_data), "",
						car);
			} else {
				GraphViewSeries series = new GraphViewSeries(car.getName(),
						new GraphViewStyle(car.getColor(), 3),
						graphData.toArray(new GraphViewData[graphData.size()]));
				graphSerieses.add(series);

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
	public GraphView getGraphView() {
		GraphView graphView = new LineGraphView(context,
				context.getString(R.string.report_title_fuel_consumption)) {
			@Override
			protected String formatLabel(double value, boolean isValueX) {
				if (isValueX) {
					return super.formatLabel(value, isValueX);
				} else {
					return String.format("%.2f", value);
				}
			}
		};

		for (GraphViewSeries series : graphSerieses) {
			graphView.addSeries(series);
		}
		graphView.setShowLegend(true);
		graphView.setLegendAlign(LegendAlign.BOTTOM);

		Refueling firstFuel = Refueling.getFirst();
		Refueling lastFuel = Refueling.getLast();
		if (firstFuel != null && lastFuel != null) {
			DateFormat dateFmt = DateFormat.getDateInstance();
			graphView.setHorizontalLabels(new String[] {
					dateFmt.format(firstFuel.getDate()),
					dateFmt.format(lastFuel.getDate()) });
		}
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
}
