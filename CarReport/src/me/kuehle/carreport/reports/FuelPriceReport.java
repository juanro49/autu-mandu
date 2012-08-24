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
import me.kuehle.carreport.db.Helper;
import me.kuehle.carreport.db.RefuelingTable;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.SeriesSelection;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Toast;

public class FuelPriceReport extends AbstractReport {
	private ReportData reportData;
	private String unit;

	public FuelPriceReport(Context context) {
		super(context);

		Preferences prefs = new Preferences(context);
		unit = String.format("%s/%s", prefs.getUnitCurrency(),
				prefs.getUnitVolume());
		
		reportData = new ReportData(context, "", Color.BLUE);

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

		addData(context.getString(R.string.report_highest),
				String.format("%.3f %s", cursor.getFloat(0), unit));
		addData(context.getString(R.string.report_lowest),
				String.format("%.3f %s", cursor.getFloat(1), unit));
		addData(context.getString(R.string.report_average),
				String.format("%.3f %s", cursor.getFloat(2), unit));

		cursor.close();
	}

	@Override
	public GraphicalView getGraphView() {
		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
		double[] axesMinMax = { Double.MAX_VALUE, Double.MIN_VALUE,
				Double.MAX_VALUE, Double.MIN_VALUE };

		Vector<AbstractReportData> reportData = new Vector<AbstractReportData>();
		reportData.add(this.reportData);
		if(isShowTrend()) {
			reportData.add(this.reportData.createRegressionData());
		}
		for (AbstractReportData data : reportData) {
			TimeSeries series = data.getSeries();
			dataset.addSeries(series);
			renderer.addSeriesRenderer(data.getRenderer());

			axesMinMax[0] = Math.min(axesMinMax[0], series.getMinX());
			axesMinMax[1] = Math.max(axesMinMax[1], series.getMaxX());
			axesMinMax[2] = Math.min(axesMinMax[2], series.getMinY());
			axesMinMax[3] = Math.max(axesMinMax[3], series.getMaxY());
		}

		applyDefaultStyle(renderer, axesMinMax, true, null, "%.3f");
		renderer.setShowLegend(false);

		final GraphicalView graphView = ChartFactory.getTimeChartView(context,
				dataset, renderer, getDateFormatPattern());

		graphView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				SeriesSelection seriesSelection = graphView
						.getCurrentSeriesAndPoint();
				if (seriesSelection != null) {
					String date = DateFormat.getDateFormat(context).format(
							new Date((long) seriesSelection.getXValue()));
					Toast.makeText(
							context,
							String.format(
									"%s: %.3f %s\n%s: %s",
									context.getString(R.string.report_toast_price),
									seriesSelection.getValue(),
									unit,
									context.getString(R.string.report_toast_date),
									date), Toast.LENGTH_LONG).show();
				}
			}
		});

		return graphView;
	}

	@Override
	public Section getOverallSection() {
		return null;
	}

	private class ReportData extends AbstractReportData {
		public ReportData(Context context, String name, int color) {
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
		public XYSeriesRenderer getRenderer() {
			XYSeriesRenderer renderer = new XYSeriesRenderer();
			applyDefaultStyle(renderer, color, true);
			return renderer;
		}
	}
}
