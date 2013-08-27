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

import java.util.Date;
import java.util.List;
import java.util.Vector;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.db.Refueling;
import me.kuehle.carreport.util.Calculator;
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
			super(context, car.name, car.color);

			List<Refueling> refuelings = car.refuelings();
			for (Refueling refueling : refuelings) {
				xValues.add(refueling.date.getTime());
				yValues.add((double) refueling.mileage);
			}
		}
	}

	private class ReportGraphDataPerRefueling extends AbstractReportGraphData {
		public ReportGraphDataPerRefueling(Context context, Car car) {
			super(context, car.name, car.color);

			List<Refueling> refuelings = car.refuelings();
			for (int i = 1; i < refuelings.size(); i++) {
				xValues.add(refuelings.get(i).date.getTime());
				yValues.add((double) (refuelings.get(i).mileage - refuelings
						.get(i - 1).mileage));
			}
		}
	}

	public static final int GRAPH_OPTION_ACCUMULATED = 0;
	public static final int GRAPH_OPTION_PER_REFUELING = 1;

	private Vector<AbstractReportGraphData> reportDataAccumulated = new Vector<AbstractReportGraphData>();
	private Vector<AbstractReportGraphData> reportDataPerRefueling = new Vector<AbstractReportGraphData>();
	private double[] minXValue = { Long.MAX_VALUE, Long.MAX_VALUE };

	private String unit;
	private boolean showLegend;

	public MileageReport(Context context) {
		super(context);
	}

	@Override
	public int[] getAvailableChartOptions() {
		int[] options = new int[2];
		options[GRAPH_OPTION_ACCUMULATED] = R.string.report_graph_accumulated;
		options[GRAPH_OPTION_PER_REFUELING] = R.string.report_graph_per_refueling;
		return options;
	}

	@Override
	public String getTitle() {
		return context.getString(R.string.report_title_mileage);
	}

	@Override
	protected Chart onGetChart(boolean zoomable, boolean moveable) {
		final Dataset dataset = new Dataset();
		RendererList renderers = new RendererList();
		LineRenderer renderer = new LineRenderer(context);
		renderers.addRenderer(renderer);

		Vector<AbstractReportGraphData> reportData = getChartOption() == GRAPH_OPTION_ACCUMULATED ? reportDataAccumulated
				: reportDataPerRefueling;
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
		chart.getDomainAxis()
				.setDefaultBottomBound(minXValue[getChartOption()]);
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
		unit = prefs.getUnitDistance();
		showLegend = prefs.isShowLegend();

		// Car data
		List<Car> cars = Car.getAll();
		for (Car car : cars) {
			// Add section for car
			Section section;
			if (car.isSuspended()) {
				section = addDataSection(
						String.format("%s [%s]", car.name,
								context.getString(R.string.suspended)),
						car.color, 1);
			} else {
				section = addDataSection(car.name, car.color);
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
			ReportGraphDataPerRefueling carDataNormal = new ReportGraphDataPerRefueling(
					context, car);
			if (carDataNormal.size() == 0) {
				section.addItem(new Item(context
						.getString(R.string.report_not_enough_data), ""));
			} else {
				reportDataPerRefueling.add(carDataNormal);
				minXValue[GRAPH_OPTION_PER_REFUELING] = Math.min(
						minXValue[GRAPH_OPTION_PER_REFUELING], carDataNormal
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
}
