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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.db.OtherCost;
import me.kuehle.carreport.db.Refueling;
import me.kuehle.carreport.util.Recurrence;
import me.kuehle.chartlib.axis.AxisLabelFormatter;
import me.kuehle.chartlib.chart.Chart;
import me.kuehle.chartlib.data.Dataset;
import me.kuehle.chartlib.data.Series;
import me.kuehle.chartlib.renderer.BarRenderer;
import me.kuehle.chartlib.renderer.LineRenderer;
import me.kuehle.chartlib.renderer.RendererList;

import org.joda.time.DateTime;
import org.joda.time.Seconds;

import android.content.Context;
import android.text.format.DateFormat;
import android.util.SparseArray;

public class CostsReport extends AbstractReport {
	private class ReportGraphData extends AbstractReportGraphData {
		private int option;

		public ReportGraphData(Context context, Car car, int option) {
			super(context, car.name, car.color);
			this.option = option;
		}

		public void add(DateTime date, double costs) {
			if (option == GRAPH_OPTION_MONTH) {
				date = new DateTime(date.getYear(), date.getMonthOfYear(), 1,
						0, 0);
			} else {
				date = new DateTime(date.getYear(), 1, 1, 0, 0);
			}

			int index = xValues.indexOf(date.getMillis());
			if (index == -1) {
				xValues.add(date.getMillis());
				yValues.add(costs);
			} else {
				yValues.set(index, yValues.get(index) + costs);
			}
		}

		@Override
		public AbstractReportGraphData createRegressionData() {
			if (size() == 0) {
				return super.createRegressionData();
			}

			long lastX = xValues.lastElement();
			xValues.remove(xValues.size() - 1);
			double lastY = yValues.lastElement();
			yValues.remove(yValues.size() - 1);
			AbstractReportGraphData data = super.createRegressionData();
			xValues.add(lastX);
			yValues.add(lastY);
			return data;
		}
	}

	public static final int GRAPH_OPTION_MONTH = 0;
	public static final int GRAPH_OPTION_YEAR = 1;

	private static final long[] SEC_PER_PERIOD = {
			1000l * 60l * 60l * 24l * 30l, 1000l * 60l * 60l * 24l * 365l };

	private SparseArray<ReportGraphData> costsPerMonth = new SparseArray<ReportGraphData>();
	private SparseArray<ReportGraphData> costsPerYear = new SparseArray<ReportGraphData>();
	private String unit;
	private boolean showLegend;
	private String[] xLabelFormat = new String[2];
	private int visibleBarCount;

	public CostsReport(Context context) {
		super(context);

		Preferences prefs = new Preferences(context);
		unit = prefs.getUnitCurrency();
		showLegend = prefs.isShowLegend();

		// Settings, which are based on the screen size.
		if (context.getResources().getConfiguration().smallestScreenWidthDp > 480) {
			xLabelFormat[GRAPH_OPTION_MONTH] = "MMMM, yyyy";
			xLabelFormat[GRAPH_OPTION_YEAR] = "yyyy";
			visibleBarCount = 4;
		} else {
			xLabelFormat[GRAPH_OPTION_MONTH] = "MMM, yyyy";
			xLabelFormat[GRAPH_OPTION_YEAR] = "yyyy";
			visibleBarCount = 3;
		}

		List<Car> cars = Car.getAll();
		for (Car car : cars) {
			Section section;
			if (car.isSuspended()) {
				section = addDataSection(
						String.format("%s [%s]", car.name,
								context.getString(R.string.suspended)),
						car.color, 1);
			} else {
				section = addDataSection(car.name, car.color);
			}

			costsPerMonth.put(car.getId().intValue(), new ReportGraphData(
					context, car, GRAPH_OPTION_MONTH));
			costsPerYear.put(car.getId().intValue(), new ReportGraphData(
					context, car, GRAPH_OPTION_YEAR));

			int startMileage = Integer.MAX_VALUE;
			int endMileage = Integer.MIN_VALUE;
			DateTime startDate = new DateTime();
			DateTime endDate;
			if (car.isSuspended()) {
				endDate = new DateTime(car.suspendedSince);
			} else {
				endDate = new DateTime();
			}
			double costs = 0;

			List<Refueling> refuelings = car.refuelings();
			List<OtherCost> otherCosts = car.otherCosts();

			if ((refuelings.size() + otherCosts.size()) < 2) {
				section.addItem(new Item(context
						.getString(R.string.report_not_enough_data), ""));
				continue;
			}

			boolean first = true;
			for (Refueling refueling : refuelings) {
				if (!first) {
					costs += refueling.price;
				}

				DateTime date = new DateTime(refueling.date);

				costsPerMonth.get(car.getId().intValue()).add(date,
						refueling.price);
				costsPerYear.get(car.getId().intValue()).add(date,
						refueling.price);

				startMileage = Math.min(startMileage, refueling.mileage);
				endMileage = Math.max(endMileage, refueling.mileage);
				if (startDate.isAfter(date)) {
					startDate = date;
				}

				first = false;
			}

			first = true;
			for (OtherCost otherCost : otherCosts) {
				if (!first
						|| (otherCost.mileage > -1 && otherCost.mileage < startMileage)) {
					costs += otherCost.price
							* otherCost.recurrence
									.getRecurrencesSince(otherCost.date);
				}

				Recurrence recurrence = otherCost.recurrence;
				DateTime date = new DateTime(otherCost.date);
				while (date.isBefore(endDate)) {
					costsPerMonth.get(car.getId().intValue()).add(date,
							otherCost.price);
					costsPerYear.get(car.getId().intValue()).add(date,
							otherCost.price);
					switch (recurrence.getInterval()) {
					case ONCE:
						// Set date after now, so the loop ends.
						date = DateTime.now().plusYears(1);
						break;
					case DAY:
						date = date.plusDays(recurrence.getMultiplier());
						break;
					case MONTH:
						date = date.plusMonths(recurrence.getMultiplier());
						break;
					case QUARTER:
						date = date.plusDays(recurrence.getMultiplier() * 3);
						break;
					case YEAR:
						date = date.plusYears(recurrence.getMultiplier());
						break;
					}
				}

				if (otherCost.mileage > -1) {
					startMileage = Math.min(startMileage, otherCost.mileage);
					endMileage = Math.max(endMileage, otherCost.mileage);
				}
				if (startDate.isAfter(otherCost.date.getTime())) {
					startDate = new DateTime(otherCost.date);
				}

				first = false;
			}

			// Calculate averages
			Seconds elapsedSeconds = Seconds.secondsBetween(startDate, endDate);
			double costsPerSecond = costs / elapsedSeconds.getSeconds();
			// 60 seconds per minute * 60 minutes per hour * 24 hours per day =
			// 86400 seconds per day
			section.addItem(new Item("\u00D8 "
					+ context.getString(R.string.report_day), String.format(
					"%.2f %s", costsPerSecond * 86400, unit)));
			// 86400 seconds per day * 30,4375 days per month = 2629800 seconds
			// per month
			// (365,25 days per year means 365,25 / 12 = 30,4375 days per month)
			section.addItem(new Item("\u00D8 "
					+ context.getString(R.string.report_month), String.format(
					"%.2f %s", costsPerSecond * 2629800, unit)));
			// 86400 seconds per day * 365,25 days per year = 31557600 seconds
			// per year
			section.addItem(new Item("\u00D8 "
					+ context.getString(R.string.report_year), String.format(
					"%.2f %s", costsPerSecond * 31557600, unit)));
			int tachoDiff = Math.max(1, endMileage - startMileage);
			section.addItem(new Item("\u00D8 " + prefs.getUnitDistance(),
					String.format("%.2f %s", costs / tachoDiff, unit)));

			section.addItem(new Item(context.getString(R.string.report_since,
					DateFormat.getDateFormat(context)
							.format(startDate.toDate())), String.format(
					"%.2f %s", costs, unit)));
		}
	}

	@Override
	public int[] getAvailableChartOptions() {
		int[] options = new int[2];
		options[GRAPH_OPTION_MONTH] = R.string.report_graph_month_history;
		options[GRAPH_OPTION_YEAR] = R.string.report_graph_year_history;
		return options;
	}

	@Override
	public Chart getChart(boolean zoomable, boolean moveable) {
		final Dataset dataset = new Dataset();
		RendererList renderers = new RendererList();
		BarRenderer renderer = new BarRenderer(context);
		LineRenderer trendRenderer = new LineRenderer(context);
		renderers.addRenderer(renderer);
		renderers.addRenderer(trendRenderer);

		int series = 0;
		for (Car car : Car.getAll()) {
			ReportGraphData data = getChartOption() == GRAPH_OPTION_MONTH ? costsPerMonth
					.get(car.getId().intValue()) : costsPerYear.get(car.getId()
					.intValue());
			if (data.isEmpty()) {
				continue;
			}

			dataset.add(data.getSeries());
			data.applySeriesStyle(series, renderer);
			series++;

			if (isShowTrend()) {
				AbstractReportGraphData trendData = data.createRegressionData();
				dataset.add(trendData.getSeries());
				trendData.applySeriesStyle(series, trendRenderer);
				renderers.mapSeriesToRenderer(series, trendRenderer);
				series++;
			}
		}

		// Draw report
		final Chart chart = new Chart(context, dataset, renderers);
		applyDefaultChartStyles(chart);
		chart.setShowLegend(showLegend);
		if (isShowTrend()) {
			for (int i = 1; i < dataset.size(); i += 2) {
				chart.getLegend().setSeriesVisible(i, false);
			}
		}
		double[] xValues = getXValues(dataset);
		chart.getDomainAxis().setLabels(xValues);
		chart.getDomainAxis().setLabelFormatter(new AxisLabelFormatter() {
			@Override
			public String formatLabel(double value) {
				DateTime date = new DateTime((long) value);
				return date.toString(xLabelFormat[getChartOption()]);
			}
		});
		double padding = SEC_PER_PERIOD[getChartOption()] / 2;
		double topBound = dataset.maxX();
		double bottomBound = topBound
				- (SEC_PER_PERIOD[getChartOption()] * Math.min(
						visibleBarCount - 1, xValues.length - 1));
		chart.getDomainAxis().setDefaultBottomBound(bottomBound - padding);
		chart.getDomainAxis().setDefaultTopBound(topBound + padding);
		chart.getRangeAxis().setDefaultBottomBound(0);
		chart.getDomainAxis().setZoomable(zoomable);
		chart.getDomainAxis().setMovable(moveable);
		chart.getRangeAxis().setZoomable(zoomable);
		chart.getRangeAxis().setMovable(moveable);

		return chart;
	}

	@Override
	public String getTitle() {
		return context.getString(R.string.report_title_costs);
	}

	private double[] getXValues(Dataset dataset) {
		HashSet<Double> values = new HashSet<Double>();
		for (int s = 0; s < dataset.size(); s++) {
			Series series = dataset.get(s);
			for (int p = 0; p < series.size(); p++) {
				values.add(series.get(p).x);
			}
		}
		ArrayList<Double> list = new ArrayList<Double>(values);
		Collections.sort(list);

		double[] arrValues = new double[list.size()];
		for (int i = 0; i < arrValues.length; i++) {
			arrValues[i] = list.get(i);
		}

		return arrValues;
	}
}
