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
import me.kuehle.carreport.util.gui.SectionListAdapter.Item;
import me.kuehle.carreport.util.gui.SectionListAdapter.Section;
import me.kuehle.chartlib.chart.Chart;
import me.kuehle.chartlib.data.Dataset;
import me.kuehle.chartlib.data.Series;
import me.kuehle.chartlib.renderer.LineRenderer;
import me.kuehle.chartlib.renderer.OnClickListener;
import me.kuehle.chartlib.renderer.RendererList;
import android.content.Context;
import android.text.format.DateFormat;
import android.widget.Toast;

public class MileageReport extends AbstractReport {
	private class ReportGraphDataAccumulated extends AbstractReportGraphData {
		public ReportGraphDataAccumulated(Context context, Car car) {
			super(context, car.getName(), car.getColor());
			for (Refueling refueling : Refueling.getAllForCar(car, true)) {
				xValues.add(refueling.getDate().getTime());
				yValues.add((double) refueling.getMileage());
			}
		}
	}

	private class ReportGraphDataNormal extends AbstractReportGraphData {
		public ReportGraphDataNormal(Context context, Car car) {
			super(context, car.getName(), car.getColor());

			Refueling[] refuelings = Refueling.getAllForCar(car, true);
			for (int i = 1; i < refuelings.length; i++) {
				xValues.add(refuelings[i].getDate().getTime());
				yValues.add((double) (refuelings[i].getMileage() - refuelings[i - 1]
						.getMileage()));
			}
		}
	}

	public static final int GRAPH_OPTION_ACCUMULATED = 0;
	public static final int GRAPH_OPTION_NORMAL = 1;

	private Vector<AbstractReportGraphData> reportDataAccumulated = new Vector<AbstractReportGraphData>();
	private Vector<AbstractReportGraphData> reportDataNormal = new Vector<AbstractReportGraphData>();
	private double[] minXValue = { Long.MAX_VALUE, Long.MAX_VALUE };

	private String unit;
	private boolean showLegend;

	public MileageReport(Context context) {
		super(context);

		// Preferences
		Preferences prefs = new Preferences(context);
		unit = prefs.getUnitDistance();
		showLegend = prefs.isShowLegend();

		// Car data
		Car[] cars = Car.getAll();
		for (Car car : cars) {
			// Add section for car
			Section section;
			if (car.isSuspended()) {
				section = addDataSection(
						String.format("%s (%s)", car.getName(),
								context.getString(R.string.suspended)),
						car.getColor(), Section.STICK_BOTTOM);
			} else {
				section = addDataSection(car.getName(), car.getColor());
			}

			// Accumulated data
			ReportGraphDataAccumulated carDataAccumulated = new ReportGraphDataAccumulated(
					context, car);
			if (carDataAccumulated.size() > 0) {
				reportDataAccumulated.add(carDataAccumulated);
				minXValue[GRAPH_OPTION_ACCUMULATED] = Math.min(
						minXValue[GRAPH_OPTION_ACCUMULATED], carDataAccumulated
								.getSeries().minX());
			}

			// Normal data
			ReportGraphDataNormal carDataNormal = new ReportGraphDataNormal(
					context, car);
			if (carDataNormal.size() == 0) {
				section.addItem(new Item(context
						.getString(R.string.report_not_enough_data), ""));
			} else {
				reportDataNormal.add(carDataNormal);
				minXValue[GRAPH_OPTION_NORMAL] = Math.min(
						minXValue[GRAPH_OPTION_NORMAL], carDataNormal
								.getSeries().minX());

				section.addItem(new Item(context
						.getString(R.string.report_highest), String.format(
						"%d %s", Calculator.max(carDataNormal.yValues)
								.intValue(), unit)));
				section.addItem(new Item(context
						.getString(R.string.report_lowest), String.format(
						"%d %s", Calculator.min(carDataNormal.yValues)
								.intValue(), unit)));
				section.addItem(new Item(context
						.getString(R.string.report_average), String.format(
						"%d %s", Calculator.avg(carDataNormal.yValues)
								.intValue(), unit)));
			}
		}
	}

	@Override
	public CalculationOption[] getCalculationOptions() {
		return new CalculationOption[0];
	}

	@Override
	public Chart getChart(int option) {
		final Dataset dataset = new Dataset();
		RendererList renderers = new RendererList();
		LineRenderer renderer = new LineRenderer(context);
		renderers.addRenderer(renderer);

		Vector<AbstractReportGraphData> reportData = option == GRAPH_OPTION_ACCUMULATED ? reportDataAccumulated
				: reportDataNormal;
		Vector<AbstractReportGraphData> chartReportData = new Vector<AbstractReportGraphData>();
		if (isShowTrend()) {
			for (AbstractReportGraphData data : reportData) {
				chartReportData.add(data.createRegressionData());
			}
		}
		chartReportData.addAll(reportData);
		for (int i = 0; i < chartReportData.size(); i++) {
			dataset.add(chartReportData.get(i).getSeries());
			chartReportData.get(i).applySeriesStyle(i, renderer);
		}

		renderer.setOnClickListener(new OnClickListener() {
			@Override
			public void onSeriesClick(int series, int point) {
				Series s = dataset.get(series);
				String car = s.getTitle();
				String date = DateFormat.getDateFormat(context).format(
						new Date((long) s.get(point).x));
				Toast.makeText(
						context,
						String.format(
								"%s: %s\n%s: %.0f %s\n%s: %s",
								context.getString(R.string.report_toast_car),
								car,
								context.getString(R.string.report_toast_mileage),
								s.get(point).y, unit, context
										.getString(R.string.report_toast_date),
								date), Toast.LENGTH_LONG).show();
			}
		});

		final Chart chart = new Chart(context, dataset, renderers);
		applyDefaultChartStyles(chart);
		chart.setShowLegend(showLegend);
		if (isShowTrend()) {
			for (int i = 0; i < chartReportData.size() / 2; i++) {
				chart.getLegend().setSeriesVisible(i, false);
			}
		}
		chart.getDomainAxis().setLabelFormatter(dateLabelFormatter);
		chart.getDomainAxis().setDefaultBottomBound(minXValue[option]);

		return chart;
	}

	@Override
	public int[] getGraphOptions() {
		int[] options = new int[2];
		options[GRAPH_OPTION_ACCUMULATED] = R.string.report_graph_accumulated;
		options[GRAPH_OPTION_NORMAL] = R.string.report_graph_normal;
		return options;
	}
}
