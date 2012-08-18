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
	private String unit;

	public FuelPriceReport(Context context) {
		super(context);

		Preferences prefs = new Preferences(context);
		unit = String.format("%s/%s", prefs.getUnitCurrency(),
				prefs.getUnitVolume());

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
		double[] axesMinMax = new double[4];

		Helper helper = Helper.getInstance();
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.rawQuery(String.format(
				"SELECT %s, (%s / %s) AS fuelprice FROM %s ORDER BY %s ASC",
				RefuelingTable.COL_DATE, RefuelingTable.COL_PRICE,
				RefuelingTable.COL_VOLUME, RefuelingTable.NAME,
				RefuelingTable.COL_DATE), null);
		if (cursor.getCount() >= 2) {
			TimeSeries series = new TimeSeries("");

			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				series.add(new Date(cursor.getLong(0)), cursor.getFloat(1));
				cursor.moveToNext();
			}

			dataset.addSeries(series);

			axesMinMax[0] = series.getMinX();
			axesMinMax[1] = series.getMaxX();
			axesMinMax[2] = series.getMinY();
			axesMinMax[3] = series.getMaxY();

			XYSeriesRenderer r = new XYSeriesRenderer();
			AbstractReport.applyDefaultStyle(r, Color.BLUE, true);
			renderer.addSeriesRenderer(r);
		}
		cursor.close();

		AbstractReport.applyDefaultStyle(renderer, axesMinMax, true, null,
				"%.3f");
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
							String.format("Price: %.3f %s\nDate: %s",
									seriesSelection.getValue(), unit, date),
							Toast.LENGTH_LONG).show();
				}
			}
		});

		return graphView;
	}

	@Override
	public Section getOverallSection() {
		return null;
	}
}
