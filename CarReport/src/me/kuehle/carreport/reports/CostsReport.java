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
import java.util.Collections;
import java.util.HashSet;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.db.OtherCost;
import me.kuehle.carreport.db.Refueling;
import me.kuehle.carreport.util.Recurrence;
import me.kuehle.carreport.util.SectionListAdapter.Item;
import me.kuehle.carreport.util.SectionListAdapter.Section;
import me.kuehle.chartlib.axis.AxisLabelFormatter;
import me.kuehle.chartlib.chart.Chart;
import me.kuehle.chartlib.data.Dataset;
import me.kuehle.chartlib.data.PointD;
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
	private class CalculableItem extends ReportData.AbstractCalculableItem {
		private static final String FORMAT = "%.2f %s";
		private double value;
		private int pluralId;

		public CalculableItem(int labelId, double value) {
			super(context.getResources().getQuantityString(labelId, 1), String
					.format(FORMAT, value, unit));
			this.pluralId = labelId;
			this.value = value;
		}

		public CalculableItem(String label, double value) {
			super(label, String.format(FORMAT, value, unit));
			this.pluralId = -1;
			this.value = value;
		}

		@Override
		public void applyCalculation(double value1, int option) {
			double result = value * value1;
			setValue(String.format(FORMAT, result, unit));

			String newLabel = origLabel;
			if (pluralId != -1) {
				newLabel = context.getResources().getQuantityString(pluralId,
						value1 == 1 ? 1 : 2);
			}
			setLabel((value1 == (int) value1 ? String.valueOf((int) value1)
					: String.valueOf(value1)) + " " + newLabel);
		}
	}

	private class ReportGraphData extends AbstractReportGraphData {
		private static final int BAR_COUNT = 3;
		private int option;

		public ReportGraphData(Context context, Car car, int option) {
			super(context, car.getName(), car.getColor());
			this.option = option;
		}

		public void add(DateTime date, double costs) {
			if (option == 0) {
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

			if (size() > BAR_COUNT && data.size() == 2) {
				double m = (data.yValues.firstElement() - data.yValues
						.lastElement())
						/ (data.xValues.firstElement() - data.xValues
								.lastElement());
				long x = (long) getSeries().get(0).x;
				double y = data.yValues.lastElement()
						- ((data.xValues.lastElement() - x) * m);
				data.xValues.set(0, x);
				data.yValues.set(0, y);
			}

			return data;
		}

		public void fillXGaps() {
			DateTime date = new DateTime(xValues.firstElement());
			while (date.isBeforeNow()) {
				if (!xValues.contains(date.getMillis())) {
					xValues.add(date.getMillis());
					yValues.add(0.0);
				}

				if (option == 0) {
					date = date.plusMonths(1);
				} else {
					date = date.plusYears(1);
				}
			}

			ArrayList<PointD> points = new ArrayList<PointD>();
			for (int i = 0; i < xValues.size(); i++) {
				points.add(new PointD(xValues.get(i), yValues.get(i)));
			}
			Collections.sort(points);
			xValues.clear();
			yValues.clear();
			for (PointD point : points) {
				xValues.add((long) point.x);
				yValues.add(point.y);
			}
		}

		@Override
		public Series getSeries() {
			Series series = super.getSeries();
			while (series.size() > BAR_COUNT) {
				series.removeAt(0);
			}
			return series;
		}
	}

	private static final long HALF_A_MONTH = 1000l * 60l * 60l * 24l * 15l;
	private static final long HALF_A_YEAR = 1000l * 60l * 60l * 24l * 181l;

	private SparseArray<ReportGraphData> costsPerMonth = new SparseArray<ReportGraphData>();
	private SparseArray<ReportGraphData> costsPerYear = new SparseArray<ReportGraphData>();
	private String unit;
	private boolean showLegend;
	private String[] xLabelFormat = { "MMMM, yyyy", "yyyy" };

	public CostsReport(Context context) {
		super(context);

		Preferences prefs = new Preferences(context);
		unit = prefs.getUnitCurrency();
		showLegend = prefs.isShowLegend();

		Car[] cars = Car.getAll();
		for (Car car : cars) {
			Section section = addDataSection(car.getName(), car.getColor());

			costsPerMonth
					.put(car.getId(), new ReportGraphData(context, car, 0));
			costsPerYear.put(car.getId(), new ReportGraphData(context, car, 1));

			int startMileage = Integer.MAX_VALUE;
			int endMileage = Integer.MIN_VALUE;
			DateTime startDate = new DateTime();
			double costs = 0;

			Refueling[] refuelings = Refueling.getAllForCar(car, true);
			OtherCost[] otherCosts = OtherCost.getAllForCar(car, true);

			if ((refuelings.length + otherCosts.length) < 2) {
				section.addItem(new Item(context
						.getString(R.string.report_not_enough_data), ""));
				continue;
			}

			for (Refueling refueling : refuelings) {
				costs += refueling.getPrice();

				DateTime date = new DateTime(refueling.getDate());

				costsPerMonth.get(car.getId()).add(date, refueling.getPrice());
				costsPerYear.get(car.getId()).add(date, refueling.getPrice());

				startMileage = Math.min(startMileage, refueling.getMileage());
				endMileage = Math.max(endMileage, refueling.getMileage());
				if (startDate.isAfter(date)) {
					startDate = date;
				}
			}

			for (OtherCost otherCost : otherCosts) {
				costs += otherCost.getPrice()
						* otherCost.getRecurrence().getRecurrencesSince(
								otherCost.getDate());

				Recurrence recurrence = otherCost.getRecurrence();
				DateTime date = new DateTime(otherCost.getDate());
				while (date.isBeforeNow()) {
					costsPerMonth.get(car.getId()).add(date,
							otherCost.getPrice());
					costsPerYear.get(car.getId()).add(date,
							otherCost.getPrice());
					switch (recurrence.getInterval()) {
					case ONCE:
						date = date.plusYears(100);
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

				if (otherCost.getMileage() > -1) {
					startMileage = Math.min(startMileage,
							otherCost.getMileage());
					endMileage = Math.max(endMileage, otherCost.getMileage());
				}
				if (startDate.isAfter(otherCost.getDate().getTime())) {
					startDate = new DateTime(otherCost.getDate());
				}
			}

			costsPerMonth.get(car.getId()).fillXGaps();
			costsPerYear.get(car.getId()).fillXGaps();

			// Calculate averages
			Seconds elapsedSeconds = Seconds.secondsBetween(startDate,
					new DateTime());
			double costsPerSecond = costs / elapsedSeconds.getSeconds();
			// 60 seconds per minute * 60 minutes per hour * 24 hours per day =
			// 86400 seconds per day
			section.addItem(new CalculableItem(R.plurals.report_day,
					costsPerSecond * 86400));
			// 86400 seconds per day * 30,4375 days per month = 2629800 seconds
			// per month
			// (365,25 days per year means 365,25 / 12 = 30,4375 days per month)
			section.addItem(new CalculableItem(R.plurals.report_month,
					costsPerSecond * 2629800));
			// 86400 seconds per day * 365,25 days per year = 31557600 seconds
			// per year
			section.addItem(new CalculableItem(R.plurals.report_year,
					costsPerSecond * 31557600));
			int tachoDiff = Math.max(1, endMileage - startMileage);
			section.addItem(new CalculableItem(prefs.getUnitDistance(), costs
					/ tachoDiff));

			section.addItem(new Item(context.getString(R.string.report_since,
					DateFormat.getDateFormat(context)
							.format(startDate.toDate())), String.format(
					"%.2f %s", costs, unit)));
		}
	}

	@Override
	public CalculationOption[] getCalculationOptions() {
		return new CalculationOption[] { new CalculationOption(
				R.string.report_calc_multiply_name,
				R.string.report_calc_multiply_hint1) };
	}

	@Override
	public Chart getChart(final int option) {
		final Dataset dataset = new Dataset();
		RendererList renderers = new RendererList();
		BarRenderer renderer = new BarRenderer(context);
		LineRenderer trendRenderer = new LineRenderer(context);
		renderers.addRenderer(renderer);
		renderers.addRenderer(trendRenderer);

		int series = 0;
		for (Car car : Car.getAll()) {
			ReportGraphData data = option == 0 ? costsPerMonth.get(car.getId())
					: costsPerYear.get(car.getId());
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
		chart.getDomainAxis().setLabels(getXValues(dataset));
		chart.getDomainAxis().setLabelFormatter(new AxisLabelFormatter() {
			@Override
			public String formatLabel(double value) {
				DateTime date = new DateTime((long) value);
				return date.toString(xLabelFormat[option]);
			}
		});
		chart.getDomainAxis().setZoomable(false);
		chart.getDomainAxis().setMovable(false);
		long padding = option == 0 ? HALF_A_MONTH : HALF_A_YEAR;
		chart.getDomainAxis().setDefaultBottomBound(dataset.minX() - padding);
		chart.getDomainAxis().setDefaultTopBound(dataset.maxX() + padding);
		chart.getRangeAxis().setDefaultBottomBound(0);

		return chart;
	}

	@Override
	public int[] getGraphOptions() {
		return new int[] { R.string.report_graph_month_history,
				R.string.report_graph_year_history };
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
