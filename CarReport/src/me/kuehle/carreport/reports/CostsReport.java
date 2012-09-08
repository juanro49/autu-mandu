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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.db.OtherCost;
import me.kuehle.carreport.db.Refueling;
import me.kuehle.carreport.gui.SectionListAdapter.Item;
import me.kuehle.carreport.gui.SectionListAdapter.Section;
import me.kuehle.carreport.util.Recurrence;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.BarChart;
import org.achartengine.chart.BarChart.Type;
import org.achartengine.chart.TimeChart;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.joda.time.DateTime;
import org.joda.time.Seconds;

import android.content.Context;
import android.text.format.DateFormat;
import android.util.SparseArray;

public class CostsReport extends AbstractReport {
	private SparseArray<HashMap<DateTime, Double>> costsPerYear = new SparseArray<HashMap<DateTime, Double>>();
	private SparseArray<HashMap<DateTime, Double>> costsPerMonth = new SparseArray<HashMap<DateTime, Double>>();

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
				addCosts(car, date, refueling.getPrice());

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
					addCosts(car, date, otherCost.getPrice());
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
	public int[] getGraphOptions() {
		return new int[] { R.string.report_graph_month_history,
				R.string.report_graph_year_history };
	}

	@Override
	public GraphicalView getGraphView(int option) {
		final XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
		double[] axesMinMax = { Double.MAX_VALUE, Double.MIN_VALUE, 0,
				Double.MIN_VALUE };

		// Collect data
		Vector<String> types = new Vector<String>();
		Vector<AbstractReportGraphData> reportData = new Vector<AbstractReportGraphData>();
		for (Car car : Car.getAll()) {
			ReportGraphData data = new ReportGraphData(context, car, option);
			reportData.add(data);
			types.add(BarChart.TYPE);
			if (isShowTrend()) {
				reportData.add(data.createRegressionData());
				types.add(TimeChart.TYPE);
			}
		}

		// Add series
		for (AbstractReportGraphData data : reportData) {
			TimeSeries series = data.getSeries();
			dataset.addSeries(series);
			renderer.addSeriesRenderer(data.getRenderer());

			axesMinMax[0] = Math.min(axesMinMax[0], series.getMinX());
			axesMinMax[1] = Math.max(axesMinMax[1], series.getMaxX());
			axesMinMax[3] = Math.max(axesMinMax[3], series.getMaxY());
		}

		// Style report
		applyDefaultStyle(renderer, axesMinMax, false, null, "%.0f");
		renderer.setShowLegend(showLegend);
		renderer.setBarSpacing(0.5);
		Double[] xValues = getXValues(dataset);
		renderer.setXLabels(0);
		for (double value : xValues) {
			DateTime date = new DateTime((long) value);
			renderer.addXTextLabel(value, date.toString(xLabelFormat[option]));
		}
		// Legend height is not proportional to its text size. So this is a
		// quick fix.
		if (showLegend) {
			int[] margins = renderer.getMargins();
			margins[2] = (int) renderer.getLegendTextSize();
			renderer.setMargins(margins);
		}

		// Draw report
		final GraphicalView graphView = ChartFactory.getCombinedXYChartView(
				context, dataset, renderer,
				types.toArray(new String[types.size()]));
//		final GraphicalView graphView = ChartFactory.getBarChartView(context, dataset, renderer, Type.DEFAULT);

		return graphView;
	}

	private void addCosts(Car car, DateTime date, double costs) {
		if (this.costsPerYear.get(car.getId()) == null) {
			this.costsPerYear.append(car.getId(),
					new HashMap<DateTime, Double>());
		}
		HashMap<DateTime, Double> costsPerYear = this.costsPerYear.get(car
				.getId());
		DateTime year = new DateTime(date.getYear(), 1, 1, 0, 0);
		double prevCosts = costsPerYear.containsKey(year) ? costsPerYear
				.get(year) : 0;
		costsPerYear.put(year, prevCosts + costs);

		if (this.costsPerMonth.get(car.getId()) == null) {
			this.costsPerMonth.append(car.getId(),
					new HashMap<DateTime, Double>());
		}
		HashMap<DateTime, Double> costsPerMonth = this.costsPerMonth.get(car
				.getId());
		DateTime month = new DateTime(date.getYear(), date.getMonthOfYear(), 1,
				0, 0);
		prevCosts = costsPerMonth.containsKey(month) ? costsPerMonth.get(month)
				: 0;
		costsPerMonth.put(month, prevCosts + costs);

	}

	private Double[] getXValues(XYMultipleSeriesDataset dataset) {
		HashSet<Double> values = new HashSet<Double>();
		for (XYSeries series : dataset.getSeries()) {
			for (int i = 0; i < series.getItemCount(); i++) {
				values.add(series.getX(i));
			}
		}
		Double[] arrValues = values.toArray(new Double[values.size()]);
		Arrays.sort(arrValues);
		return arrValues;
	}

	private class ReportGraphData extends AbstractReportGraphData {
		private static final int BAR_COUNT = 4;

		public ReportGraphData(Context context, Car car, int option) {
			super(context, car.getName(), car.getColor());

			HashMap<DateTime, Double> data = option == 0 ? costsPerMonth
					.get(car.getId()) : costsPerYear.get(car.getId());
			DateTime[] dates = data.keySet().toArray(new DateTime[0]);
			Arrays.sort(dates);
			for (int i = Math.max(0, dates.length - BAR_COUNT); i < dates.length; i++) {
				xValues.add(dates[i].getMillis());
				yValues.add(data.get(dates[i]));
			}
		}

		@Override
		public XYSeriesRenderer getRenderer() {
			XYSeriesRenderer renderer = new XYSeriesRenderer();
			renderer.setColor(color);
			return renderer;
		}
	}

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
}
