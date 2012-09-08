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

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.db.Helper;
import me.kuehle.carreport.db.RefuelingTable;
import me.kuehle.chartlib.axis.DecimalAxisLabelFormatter;
import me.kuehle.chartlib.chart.Chart;
import me.kuehle.chartlib.data.Dataset;
import me.kuehle.chartlib.data.PointD;
import me.kuehle.chartlib.renderer.AbstractRenderer;
import me.kuehle.chartlib.renderer.LineRenderer;
import me.kuehle.chartlib.renderer.OnClickListener;
import me.kuehle.chartlib.renderer.RendererList;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.text.format.DateFormat;
import android.widget.Toast;

public class FuelPriceReport extends AbstractReport {
	private class CalculableItem extends ReportData.AbstractCalculableItem {
		private static final String FORMAT_NORMAL = "%.3f %s";
		private static final String FORMAT_CALCULATION = "%.2f %s";
		private double value;
		private String[] calcLabels;

		public CalculableItem(String label, double value) {
			this(label, value, new String[] { label, label });
		}

		public CalculableItem(String label, double value, String[] calcLabels) {
			super(label, String.format(FORMAT_NORMAL, value, unit));
			this.value = value;
			this.calcLabels = calcLabels;
		}

		@Override
		public void applyCalculation(double value1, int option) {
			Preferences prefs = new Preferences(context);
			if (option == 0) {
				double result = value * value1;
				setValue(String.format(FORMAT_CALCULATION, result,
						prefs.getUnitCurrency()));
			} else if (option == 1) {
				double result = value1 / value;
				setValue(String.format(FORMAT_CALCULATION, result,
						prefs.getUnitVolume()));
			}
			setLabel(calcLabels[option]);
		}
	}

	private class ReportGraphData extends AbstractReportGraphData {
		public ReportGraphData(Context context, String name, int color) {
			super(context, name, color);

			Helper helper = Helper.getInstance();
			SQLiteDatabase db = helper.getReadableDatabase();
			Cursor cursor = db
					.rawQuery(
							String.format(
									"SELECT %s, (%s / %s) AS fuelprice FROM %s ORDER BY %s ASC",
									RefuelingTable.COL_DATE,
									RefuelingTable.COL_PRICE,
									RefuelingTable.COL_VOLUME,
									RefuelingTable.NAME,
									RefuelingTable.COL_DATE), null);
			if (cursor.getCount() >= 2) {
				cursor.moveToFirst();
				while (!cursor.isAfterLast()) {
					xValues.add(cursor.getLong(0));
					yValues.add(cursor.getDouble(1));
					cursor.moveToNext();
				}
			}
			cursor.close();
		}

		@Override
		public void applySeriesStyle(int series, AbstractRenderer renderer) {
			super.applySeriesStyle(series, renderer);
			if (renderer instanceof LineRenderer) {
				((LineRenderer) renderer).setSeriesFillBelowLine(series, true);
			}
		}
	}

	private ReportGraphData reportData;

	private String unit;

	public FuelPriceReport(Context context) {
		super(context);

		Preferences prefs = new Preferences(context);
		unit = String.format("%s/%s", prefs.getUnitCurrency(),
				prefs.getUnitVolume());

		reportData = new ReportGraphData(context, "", Color.BLUE);

		Helper helper = Helper.getInstance();
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db
				.rawQuery(
						String.format(
								"SELECT max(fuelprice), min(fuelprice), avg(fuelprice) "
										+ "FROM (SELECT (%s / %s) AS fuelprice FROM %s)",
								RefuelingTable.COL_PRICE,
								RefuelingTable.COL_VOLUME, RefuelingTable.NAME),
						null);
		cursor.moveToFirst();

		addData(new CalculableItem(context.getString(R.string.report_highest),
				cursor.getFloat(0), new String[] {
						context.getString(R.string.report_at_most),
						context.getString(R.string.report_at_least) }));
		addData(new CalculableItem(context.getString(R.string.report_lowest),
				cursor.getFloat(1), new String[] {
						context.getString(R.string.report_at_least),
						context.getString(R.string.report_at_most) }));
		addData(new CalculableItem(context.getString(R.string.report_average),
				cursor.getFloat(2)));

		cursor.close();
	}

	@Override
	public CalculationOption[] getCalculationOptions() {
		Preferences prefs = new Preferences(context);
		return new CalculationOption[] {
				new CalculationOption(context.getString(
						R.string.report_calc_vol2price_name,
						prefs.getUnitVolume()), prefs.getUnitVolume()),
				new CalculationOption(context.getString(
						R.string.report_calc_price2vol_name,
						prefs.getUnitVolume(), prefs.getUnitCurrency()),
						prefs.getUnitCurrency()) };
	}

	@Override
	public Chart getChart(int option) {
		final Dataset dataset = new Dataset();
		RendererList renderers = new RendererList();
		LineRenderer renderer = new LineRenderer(context);
		renderers.addRenderer(renderer);

		dataset.add(reportData.getSeries());
		reportData.applySeriesStyle(0, renderer);
		if (isShowTrend()) {
			AbstractReportGraphData trendReportData = reportData
					.createRegressionData();
			dataset.add(trendReportData.getSeries());
			trendReportData.applySeriesStyle(1, renderer);
		}

		renderer.setOnClickListener(new OnClickListener() {
			@Override
			public void onSeriesClick(int series, int point) {
				PointD p = dataset.get(series).get(point);
				String date = DateFormat.getDateFormat(context).format(
						new Date((long) p.x));
				Toast.makeText(
						context,
						String.format("%s: %.3f %s\n%s: %s",
								context.getString(R.string.report_toast_price),
								p.y, unit,
								context.getString(R.string.report_toast_date),
								date), Toast.LENGTH_LONG).show();
			}
		});

		final Chart chart = new Chart(context, dataset, renderers);
		applyDefaultChartStyles(chart);
		chart.setShowLegend(false);
		chart.getDomainAxis().setLabelFormatter(dateLabelFormatter);
		chart.getRangeAxis()
				.setLabelFormatter(new DecimalAxisLabelFormatter(3));

		return chart;
	}

	@Override
	public int[] getGraphOptions() {
		return new int[1];
	}
}
