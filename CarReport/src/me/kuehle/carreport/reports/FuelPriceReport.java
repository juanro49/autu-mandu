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

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.db.Helper;
import me.kuehle.carreport.db.Refueling;
import me.kuehle.carreport.db.RefuelingTable;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.format.DateFormat;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphView.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

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
	public GraphView getGraphView() {
		LineGraphView graphView = new LineGraphView(context,
				context.getString(R.string.report_title_fuel_price)) {
			@Override
			protected String formatLabel(double value, boolean isValueX) {
				if (isValueX) {
					return super.formatLabel(value, isValueX);
				} else {
					return String.format("%.3f", value);
				}
			}
		};

		ArrayList<GraphViewData> graphData = new ArrayList<GraphView.GraphViewData>();
		Helper helper = Helper.getInstance();
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.rawQuery(String.format(
				"SELECT %s, (%s / %s) AS fuelprice FROM %s ORDER BY %s ASC",
				RefuelingTable.COL_DATE, RefuelingTable.COL_PRICE,
				RefuelingTable.COL_VOLUME, RefuelingTable.NAME,
				RefuelingTable.COL_DATE), null);
		if (cursor.getCount() >= 2) {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				graphData.add(new GraphViewData(cursor.getLong(0), cursor
						.getFloat(1)));
				cursor.moveToNext();
			}

			graphView.addSeries(new GraphViewSeries(graphData
					.toArray(new GraphViewData[graphData.size()])));
		}
		cursor.close();
		graphView.setDrawBackground(true);

		Refueling firstFuel = Refueling.getFirst();
		Refueling lastFuel = Refueling.getLast();
		if (firstFuel != null && lastFuel != null) {
			java.text.DateFormat dateFmt = DateFormat.getDateFormat(context);
			graphView.setHorizontalLabels(new String[] {
					dateFmt.format(firstFuel.getDate()),
					dateFmt.format(lastFuel.getDate()) });
		}
		return graphView;
	}

	@Override
	public Section getOverallSection() {
		return null;
	}
}
